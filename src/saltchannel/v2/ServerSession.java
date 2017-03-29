package saltchannel.v2;

import java.util.Arrays;

import a1a2.A2Packet;
import saltchannel.BadPeer;
import saltchannel.ByteChannel;
import saltchannel.CryptoLib;
import saltchannel.TweetNaCl;
import saltchannel.util.Deserializer;
import saltchannel.util.KeyPair;
import saltchannel.util.NullTimeChecker;
import saltchannel.util.Rand;
import saltchannel.util.TimeChecker;
import saltchannel.util.TimeKeeper;
import saltchannel.util.NullTimeKeeper;
import saltchannel.v2.EncryptedChannelV2.Role;
import saltchannel.v2.packets.BadTicket;
import saltchannel.v2.packets.M1Packet;
import saltchannel.v2.packets.M2Packet;
import saltchannel.v2.packets.M3Packet;
import saltchannel.v2.packets.M4Packet;
import saltchannel.v2.packets.Packet;
import saltchannel.v2.packets.PacketHeader;
import saltchannel.v2.packets.TTPacket;

/**
 * Server-side implementation of a Salt Channel v2 session.
 * Usage: create object, set or create ephemeral key, use other setX methods,
 * call handshake(), get resulting encrypted ByteChannel to use by
 * application layer. Use getClientSig() to get client's pubkey.
 * Do not reuse the object for more than one Salt Channel session.
 * Limitation: does not support virtual servers, just one pubkey supported.
 * 
 * @author Frans Lundberg
 */
public class ServerSession {
    private final ByteChannel clearChannel;
    private EncryptedChannelV2 encryptedChannel;
    private TimeKeeper timeKeeper;
    private TimeChecker timeChecker;
    private A2Packet a2Packet;
    private KeyPair sigKeyPair;
    private KeyPair encKeyPair;
    private M1Packet m1;
    private byte[] m1Hash;
    private M2Packet m2;
    private byte[] m2Hash;
    private M4Packet m4;
    private AppChannelV2 appChannel;
    private ResumeHandler resumeHandler;
    private byte[] m1Bytes;
    private PacketHeader m1Header;
    private byte[] sessionKey;
    private byte[] clientSigKey;
    
    public ServerSession(KeyPair sigKeyPair, ByteChannel clearChannel) {
        this.clearChannel = clearChannel;
        this.sigKeyPair = sigKeyPair;
        this.timeKeeper = NullTimeKeeper.INSTANCE;
        this.timeChecker = NullTimeChecker.INSTANCE;
        this.resumeHandler = null;
        initDefaultA2();
    }
    
    private void initDefaultA2() {
        this.a2Packet = new A2Packet();
        a2Packet.prots = new A2Packet.Prot[1];
        a2Packet.prots[0] = new A2Packet.Prot("SC2-------", "----------");
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
    
    public void setA2(A2Packet a2Packet) {
        this.a2Packet = a2Packet;
    }
    
    public void setTimeKeeper(TimeKeeper timeKeeper) {
        this.timeKeeper = timeKeeper;
    }
    
    public void setTimeChecker(TimeChecker timeChecker) {
        this.timeChecker = timeChecker;
    }

    public void setResumeHandler(ResumeHandler resumeHandler) {
        this.resumeHandler = resumeHandler;
    }
    
    /**
     * Executes the salt channel handshake or returns the A2 packet
     * given an A1 request.
     * 
     * @throws NoSuchServer
     * @throws BadPeer
     */
    public void handshake() {
        readM1();
        
        if (m1Header.getType() == Packet.TYPE_A1) {
            a2();
            return;
        }
        
        checkThatEncKeyPairWasSet();
        
        boolean resumed = processM1();
        if (resumed) {
            return;
        }
        
        m2();
        createEncryptedChannelFromKeyAgreement();
        
        m3();
        
        m4();
        validateSignature2();
        
        tt();
    }
    
    private void readM1() {
        m1Bytes = clearChannel.read();
        m1Header = parseHeader(m1Bytes);
    }

    private void checkThatA2WasSet() {
        if (a2Packet == null) {
            throw new IllegalStateException("a2Packet was not set");
        }
    }

    private void checkThatEncKeyPairWasSet() {
        if (encKeyPair == null) {
            throw new IllegalStateException("encKeyPair must be set before calling handshake()");
        }
    }
    
    /**
     * Returns the static (signing) public key of the client.
     * Available after a successful handshake.
     */
    public byte[] getClientSigKey() {
        return this.clientSigKey;
    }
    
    /**
     * Returns the application channel after a successful handshake.
     * The returned ByteChannel is for the application to use.
     */
    public ByteChannel getChannel() {
        return this.appChannel;
    }
    
    /**
     * Writes the A2 response.
     */
    private void a2() {
        checkThatA2WasSet();
        byte[] buffer = new byte[a2Packet.getSize()];
        a2Packet.toBytes(buffer, 0);
        clearChannel.write(buffer);
    }

    /**
     * Returns true if the session was resumed using a ticket in M1.
     */
    private boolean processM1() {
        // Note the missing support for "virtual hosting". Only one server sig key is allowed.
        
        this.m1Hash = CryptoLib.sha512(m1Bytes);
        this.m1 = M1Packet.fromBytes(m1Bytes, 0);
        timeChecker.reportFirstTime(m1.time);
        
        if (m1.serverSigKeyIncluded() && !Arrays.equals(this.sigKeyPair.pub(), m1.serverSigKey)) {
            clearChannel.write(noSuchServerM2Raw());
            throw new NoSuchServer();
        }
        
        if (m1.ticketIncluded() && resumeSupported()) {
            TicketSessionData sessionData;
            try {
                sessionData = resumeHandler.validateTicket(m1.ticket);
            } catch (BadTicket e) {
                return false;
            }
            
            createEncryptedChannelFromResumedSession(sessionData);
            writeTTPacket();
            
            return true;
        }
        
        return false;
    }
    
    private void m2() {
        this.m2 = new M2Packet();
        m2.time = timeKeeper.getFirstTime();
        m2.noSuchServer = false;
        m2.serverEncKey = this.encKeyPair.pub();
        m2.resumeSupported = this.resumeHandler != null;
        
        byte[] m2Bytes = m2.toBytes();
        this.m2Hash = CryptoLib.sha512(m2Bytes);
        clearChannel.write(m2Bytes);
    }

    private void m3() {
        M3Packet p = new M3Packet();
        p.time = timeKeeper.getTime();
        p.serverSigKey = this.sigKeyPair.pub();
        p.signature1 = signature1();
        encryptedChannel.write(p.toBytes());
    }
    
    private void m4() {
        this.m4 = M4Packet.fromBytes(encryptedChannel.read(), 0);
        this.timeChecker.checkTime(m4.time);
        this.clientSigKey = m4.clientSigKey;
    }
    
    /**
     * Sends TT message if this server supports resume and 
     * the client requested a ticket.
     */
    private void tt() {
        if (!resumeSupported()) {
            return;
        }
        
        if (m1.ticketRequested) {
            writeTTPacket();
        }
    }

    private void writeTTPacket() {
        ResumeHandler.IssuedTicket t = resumeHandler.issueTicket(clientSigKey, sessionKey);
        
        TTPacket p = new TTPacket();
        p.time = timeKeeper.getTime();
        p.ticket = t.ticket;
        p.sessionNonce = t.sessionNonce;
        encryptedChannel.write(p.toBytes());
    }

    private void createEncryptedChannelFromKeyAgreement() {
        this.sessionKey = CryptoLib.computeSharedKey(encKeyPair.sec(), m1.clientEncKey);
        this.encryptedChannel = new EncryptedChannelV2(this.clearChannel, sessionKey, Role.SERVER);
        this.appChannel = new AppChannelV2(this.encryptedChannel, timeKeeper, timeChecker);
    }
    
    /**
     * Creates this.encryptedChannel, this.appChannel, 
     * sets this.sessionKey and this.clientSigKey.
     */
    private void createEncryptedChannelFromResumedSession(TicketSessionData data) {
        this.sessionKey = data.sessionKey;
        this.clientSigKey = data.clientSigKey;
        this.encryptedChannel = new EncryptedChannelV2(this.clearChannel, sessionKey, 
                Role.SERVER, data.sessionNonce);
        this.appChannel = new AppChannelV2(this.encryptedChannel, timeKeeper, timeChecker);
    }
    
    private PacketHeader parseHeader(byte[] messageBytes) {
        return new Deserializer(messageBytes, 0).readHeader();
    }
    
    private boolean resumeSupported() {
        return resumeHandler != null;
    }
    
    /**
     * Computes Signature1.
     */
    private byte[] signature1() {
        return V2Util.createSignature(sigKeyPair, 
                encKeyPair.pub(), m1.clientEncKey, m1Hash, m2Hash);
    }
    
    /**
     * Validates M4/Signature2.
     * 
     * @throws BadPeer
     */
    private void validateSignature2() {
        assert m4.signature2 != null;
        assert m1.clientEncKey != null;
        assert encKeyPair.pub() != null;
        
        byte[] signedMessage = V2Util.concat(m4.signature2, m1.clientEncKey, encKeyPair.pub(), 
                m1Hash, m2Hash);
        
        try {
            TweetNaCl.crypto_sign_open(signedMessage, m4.clientSigKey);
        } catch (TweetNaCl.InvalidSignatureException e) {
            throw new BadPeer("invalid signature");
        }
    }
    
    private byte[] noSuchServerM2Raw() {
        M2Packet m2 = new M2Packet();
        m2.time = timeKeeper.getFirstTime();
        m2.noSuchServer = true;
        m2.serverEncKey = new byte[32];
        byte[] raw = new byte[m2.getSize()];
        m2.toBytes(raw, 0);
        return raw;
    }
}
