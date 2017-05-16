package saltchannel.v2;

import saltchannel.BadPeer;
import saltchannel.ByteChannel;
import saltchannel.CryptoLib;
import saltaa.*;
import saltchannel.util.KeyPair;
import saltchannel.util.NullTimeChecker;
import saltchannel.util.Rand;
import saltchannel.util.TimeChecker;
import saltchannel.util.TimeKeeper;
import saltchannel.util.NullTimeKeeper;
import saltchannel.v2.EncryptedChannelV2.Role;
import saltchannel.v2.packets.M1Packet;
import saltchannel.v2.packets.M2Packet;
import saltchannel.v2.packets.M3Packet;
import saltchannel.v2.packets.M4Packet;
import saltchannel.v2.packets.Packet;
import saltchannel.v2.packets.PacketHeader;
import saltchannel.v2.packets.TTPacket;

/**
 * Client-side implementation of a Salt Channel v2 session.
 * Usage: create object, set or create ephemeral key, 
 * call handshake(), get resulting encrypted channel (getChannel()) 
 * to use by application layer. Use getServerSigKey() to get the server's pubkey.
 * Do not reuse the object for more than one Salt Channel session.
 * Limitation: does not support virtual servers, just one pubkey supported.
 * For debug/inspection: the handshake messages (m1, m2, m3, m4) are stored.
 * 
 * @author Frans Lundberg
 */
public class SaltClientSession {
    private final ByteChannel clearChannel;
    private EncryptedChannelV2 encryptedChannel;
    private TimeKeeper timeKeeper;
    private TimeChecker timeChecker;
    private KeyPair sigKeyPair;
    private KeyPair encKeyPair;
    private byte[] wantedServerSigKey;
    private M1Packet m1;
    private byte[] m1Hash;
    private M2Packet m2;
    private byte[] m2Hash;
    private M3Packet m3;
    private M4Packet m4;
    private TTPacket tt;
    private AppChannelV2 appChannel;
    private boolean ticketRequested;
    private byte[] m2Bytes;
    private PacketHeader m2Header;
    private byte[] sessionKey;
    private ClientTicketData ticketData;     // ticket data to use
    private ClientTicketData newTicketData;  // new ticket from server
    private SaltLib salt = SaltLibFactory.getLib();
    private boolean bufferM4 = false;

    public SaltClientSession(KeyPair sigKeyPair, ByteChannel clearChannel) {
        this.clearChannel = clearChannel;
        this.sigKeyPair = sigKeyPair;
        this.timeKeeper = NullTimeKeeper.INSTANCE;
        this.timeChecker = NullTimeChecker.INSTANCE;
        this.ticketRequested = false;
    }
    
    public void setWantedServer(byte[] wantedServerSigKey) {
        this.wantedServerSigKey = wantedServerSigKey;
    }
    
    /**
     * Sets the ephemeral encryption key pair.
     */
    public void setEncKeyPair(KeyPair encKeyPair) {
        this.encKeyPair = encKeyPair;
    }
    
    /**
     * Generates the ephemeral encryption key pair based on the
     * supplied source of randomness.
     */
    public void setEncKeyPair(Rand rand) {
        this.encKeyPair = CryptoLib.createEncKeys(rand);
    }
    
    public void setBufferM4(boolean bufferM4) {
        this.bufferM4 = bufferM4;
    }
    
    public void setTicketRequested(boolean requestTicket) {
        this.ticketRequested = requestTicket;
    }
    
    public void setTicketData(ClientTicketData ticketData) {
        this.ticketData = ticketData;
    }
    
    public void setTimeKeeper(TimeKeeper timeKeeper) {
        this.timeKeeper = timeKeeper;
    }
    
    public void setTimeChecker(TimeChecker timeChecker) {
        this.timeChecker = timeChecker;
    }
    
    /**
     * @throws NoSuchServer
     * @throws BadPeer
     */
    public void handshake() {
        checkThatEncKeyPairWasSet();        
        
        m1();
        
        readM2Bytes();   // M2 or TT message
        
        if (m2Header.getType() == Packet.TYPE_ENCRYPTED_MESSAGE) {
            tt1();
            return;
        }
        
        m2();
        createEncryptedChannelForNewSession();
        
        m3();
        validateSignature1();
        
        m4();
        
        tt2();
    }
    
    /**
     * Returns a channel to be used by layer above (application layer).
     * 
     * @throws IllegalStateException 
     *          If the channel is not available, has not been created yet.
     */
    public ByteChannel getChannel() {
        ByteChannel result = this.appChannel;
        if (result == null) {
            throw new IllegalStateException("this.appChannel == null");
        }
        
        return result;
    }
    
    public byte[] getServerSigKey() {
        return this.m3.serverSigKey;
    }
    
    /**
     * Returns the newly issued ticket from the server or null 
     * if no new ticket was sent from the server.
     */
    public ClientTicketData getNewTicketData() {
        return newTicketData;
    }
    
    /**
     * Creates and writes M1 message.
     */
    private void m1() {
        this.m1 = new M1Packet();
        m1.time = timeKeeper.getFirstTime();
        m1.clientEncKey = this.encKeyPair.pub();
        m1.serverSigKey = this.wantedServerSigKey;
        m1.ticketRequested = this.ticketRequested;
        
        if (ticketData != null) {
            m1.ticket = ticketData.ticket;
        }
        
        byte[] m1Bytes = m1.toBytes();
        this.m1Hash = CryptoLib.sha512(m1Bytes);
        
        clearChannel.write(m1Bytes);
        
        if (ticketData != null) {
            createEncryptedChannelForResumedSession();
        }
    }
    
    private void readM2Bytes() {
        this.m2Bytes = clearChannel.read();
        this.m2Header = V2Util.parseHeader(m2Bytes);
    }
    
    /**
     * Handles M2 message.
     * 
     * @throws NoSuchServer.
     */
    private void m2() {
        this.m2 = M2Packet.fromBytes(m2Bytes, 0);
        if (m2.noSuchServer) {
            throw new NoSuchServer();
        }
        this.timeChecker.reportFirstTime(m2.time);
        
        this.m2Hash = CryptoLib.sha512(m2.toBytes());
    }
    
    private void m3() {
        this.m3 = M3Packet.fromBytes(encryptedChannel.read(), 0);
        this.timeChecker.checkTime(m3.time);
    }
    
    private void m4() {
        this.m4 = new M4Packet();
        m4.time = timeKeeper.getTime();
        m4.clientSigKey = this.sigKeyPair.pub();
        m4.signature2 = signature2();
        
        if (this.bufferM4) {
            appChannel.setBufferedMessage(m4);
        } else {
            encryptedChannel.write(m4.toBytes());
        }
    }
    
    /**
     * Reads expected TT message.
     */
    private void tt1() {
        encryptedChannel.pushback(this.m2Bytes);
        
        byte[] bytes = encryptedChannel.read();
        TTPacket tt = TTPacket.fromBytes(bytes, 0);
        
        this.newTicketData = new ClientTicketData();
        this.newTicketData.sessionKey = this.sessionKey;
        this.newTicketData.sessionNonce = tt.sessionNonce;
        this.newTicketData.ticket = tt.ticket;
    }
    
    /**
     * Reads TT packet from server after 3-way handshake.
     */
    private void tt2() {
        if (m1.ticketRequested && m2.resumeSupported) {
            byte[] bytes = encryptedChannel.read();
            tt = TTPacket.fromBytes(bytes, 0);
            newTicketData = new ClientTicketData();
            newTicketData.ticket = tt.ticket;
            newTicketData.sessionKey = this.sessionKey;
            newTicketData.sessionNonce = tt.sessionNonce;
        }
    }
    
    /**
     * Validates M3/Signature1.
     * 
     * @throws BadPeer
     */
    private void validateSignature1() {        
        byte[] signedMessage = V2Util.concat(
                m3.signature1, m1Hash, m2Hash);    

        try {
            byte[] m = new byte[signedMessage.length];
            salt.crypto_sign_open(m, signedMessage, m3.serverSigKey);
        } catch (BadSignatureException e) {
            throw new BadPeer("invalid signature");
        }
    }
    
    /**
     * Computes Signature2.
     */
    private byte[] signature2() {
        return V2Util.createSignature(sigKeyPair, m1Hash, m2Hash);
    }
    
    private void createEncryptedChannelForNewSession() {
        this.sessionKey = CryptoLib.computeSharedKey(encKeyPair.sec(), m2.serverEncKey);
        this.encryptedChannel = new EncryptedChannelV2(this.clearChannel, sessionKey, Role.CLIENT);
        this.appChannel = new AppChannelV2(this.encryptedChannel, timeKeeper, timeChecker);
    }
    
    private void createEncryptedChannelForResumedSession() {
        this.sessionKey = this.ticketData.sessionKey;
        this.encryptedChannel = new EncryptedChannelV2(this.clearChannel, sessionKey, 
                Role.CLIENT, this.ticketData.sessionNonce);
        this.appChannel = new AppChannelV2(this.encryptedChannel, timeKeeper, timeChecker);
    }

    private void checkThatEncKeyPairWasSet() {
        if (encKeyPair == null) {
            throw new IllegalStateException("encKeyPair must be set before calling handshake()");
        }
    }
}
