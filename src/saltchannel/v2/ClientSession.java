package saltchannel.v2;

import saltchannel.BadPeer;
import saltchannel.ByteChannel;
import saltchannel.CryptoLib;
import saltchannel.TweetNaCl;
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
public class ClientSession {
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
    private AppChannelV2 appChannel;

    public ClientSession(KeyPair sigKeyPair, ByteChannel clearChannel) {
        this.clearChannel = clearChannel;
        this.sigKeyPair = sigKeyPair;
        this.timeKeeper = NullTimeKeeper.INSTANCE;
        this.timeChecker = NullTimeChecker.INSTANCE;
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
        
        m2();
        createEncryptedChannel();
        
        m3();
        validateSignature1();
        
        m4();
    }
    
    /**
     * Returns a channel to be used by upper layer (application layer).
     */
    public ByteChannel getChannel() {
        return this.appChannel;
    }
    
    public byte[] getServerSigKey() {
        return this.m3.serverSigKey;
    }
    
    /**
     * Creates and writes M1 message.
     */
    private void m1() {
        this.m1 = new M1Packet();
        m1.time = timeKeeper.getFirstTime();
        m1.clientEncKey = this.encKeyPair.pub();
        m1.serverSigKey = this.wantedServerSigKey;
        
        byte[] m1Bytes = m1.toBytes();
        this.m1Hash = CryptoLib.sha512(m1Bytes);
        
        clearChannel.write(m1Bytes);
    }
    
    /**
     * Reads M2 message.
     * 
     * @throws NoSuchServer.
     */
    private void m2() {
        this.m2 = M2Packet.fromBytes(clearChannel.read(), 0);
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
        encryptedChannel.write(m4.toBytes());
    }
    
    /**
     * Validates M3/Signature1.
     * 
     * @throws BadPeer
     */
    private void validateSignature1() {        
        byte[] signedMessage = V2Util.concat(
                m3.signature1, m2.serverEncKey, m1.clientEncKey, m1Hash, m2Hash);
        
        try {
            TweetNaCl.crypto_sign_open(signedMessage, m3.serverSigKey);
        } catch (TweetNaCl.InvalidSignatureException e) {
            throw new BadPeer("invalid signature");
        }
    }
    
    /**
     * Computes Signature2.
     */
    private byte[] signature2() {
        return V2Util.createSignature(sigKeyPair, 
                encKeyPair.pub(), m2.serverEncKey, m1Hash, m2Hash);
    }
    
    private void createEncryptedChannel() {
        byte[] sharedKey = CryptoLib.computeSharedKey(encKeyPair.sec(), m2.serverEncKey);
        this.encryptedChannel = new EncryptedChannelV2(this.clearChannel, sharedKey, Role.CLIENT);
        this.appChannel = new AppChannelV2(this.encryptedChannel, timeKeeper, timeChecker);
    }

    private void checkThatEncKeyPairWasSet() {
        if (encKeyPair == null) {
            throw new IllegalStateException("encKeyPair must be set before calling handshake()");
        }
    }
}
