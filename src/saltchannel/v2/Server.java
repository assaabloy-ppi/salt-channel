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
import saltchannel.v2.packets.M1Packet;
import saltchannel.v2.packets.M2Packet;
import saltchannel.v2.packets.M3Packet;
import saltchannel.v2.packets.M4Packet;
import saltchannel.v2.packets.Packet;
import saltchannel.v2.packets.PacketHeader;

/**
 * Server-side implementation of a Salt Channel v2 session.
 * Usage: create object, set or create ephemeral key, 
 * call handshake(), get resulting encrypted ByteChannel to use by
 * application layer. Use getClientSig() to get client's pubkey.
 * Do not reuse the object for more than one Salt Channel session.
 * Limitation: does not support virtual servers, just one pubkey supported.
 * 
 * @author Frans Lundberg
 */
public class Server {
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
    
    public Server(KeyPair sigKeyPair, ByteChannel clearChannel) {
        this.clearChannel = clearChannel;
        this.sigKeyPair = sigKeyPair;
        this.timeKeeper = NullTimeKeeper.INSTANCE;
        this.timeChecker = NullTimeChecker.INSTANCE;
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
    
    /**
     * Executes the salt channel handshake or returns the A2 packet
     * given an A1 request.
     * 
     * @throws NoSuchServer
     * @throws BadPeer
     */
    public void handshake() {
        byte[] m1Bytes = clearChannel.read();
        PacketHeader header = parseHeader(m1Bytes);
        
        if (header.getType() == Packet.TYPE_A1) {
            checkThatA2WasSet();
            a2();
            return;
        }
        
        checkThatEncKeyPairWasSet();
        
        m1(m1Bytes);
        handleNoSuchServerIfNeeded();
        
        m2();
        createEncryptedChannel();
        
        m3();
        
        m4();
        validateSignature2();
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

    private void handleNoSuchServerIfNeeded() {
        if (m1.hasServerSigKey() && !Arrays.equals(this.sigKeyPair.pub(), m1.serverSigKey)) {
            clearChannel.write(noSuchServerM2Raw());
            throw new NoSuchServer();
        }
    }
    
    /**
     * Returns the static (signing) public key of the client.
     * Available after a successful handshake.
     */
    public byte[] getClientSigKey() {
        return this.m4.clientSigKey;
    }
    
    /**
     * Returns the application channel after a successful handshake.
     * The returned ByteChannel is for the application to use.
     */
    public ByteChannel getChannel() {
        return this.appChannel;
    }
    
    private PacketHeader parseHeader(byte[] messageBytes) {
        return new Deserializer(messageBytes, 0).readHeader();
    }
    
    /**
     * Writes the A2 response.
     */
    private void a2() {
        byte[] buffer = new byte[a2Packet.getSize()];
        a2Packet.toBytes(buffer, 0);
        clearChannel.write(buffer);
    }

    private void m1(byte[] m1Bytes) {
        this.m1Hash = CryptoLib.sha512(m1Bytes);
        this.m1 = M1Packet.fromBytes(m1Bytes, 0);
        timeChecker.reportFirstTime(m1.time);
    }
    
    private void m2() {
        this.m2 = new M2Packet();
        m2.time = timeKeeper.getFirstTime();
        m2.noSuchServer = false;
        m2.serverEncKey = this.encKeyPair.pub();
        byte[] m2Bytes = m2.toBytes();
        this.m2Hash = CryptoLib.sha512(m2Bytes);
        clearChannel.write(m2Bytes);
    }
    
    private void m4() {
        this.m4 = M4Packet.fromBytes(encryptedChannel.read(), 0);
        this.timeChecker.checkTime(m4.time);
    }
    
    private void createEncryptedChannel() {
        byte[] sharedKey = CryptoLib.computeSharedKey(encKeyPair.sec(), m1.clientEncKey);
        this.encryptedChannel = new EncryptedChannelV2(this.clearChannel, sharedKey, Role.SERVER);
        this.appChannel = new AppChannelV2(this.encryptedChannel, timeKeeper, timeChecker);
    }

    private void m3() {
        M3Packet p = new M3Packet();
        p.time = timeKeeper.getTime();
        p.serverSigKey = this.sigKeyPair.pub();
        p.signature1 = signature1();
        encryptedChannel.write(p.toBytes());
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
