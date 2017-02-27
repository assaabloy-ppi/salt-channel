package saltchannel.v2;

import java.util.Arrays;
import saltchannel.BadPeer;
import saltchannel.ByteChannel;
import saltchannel.CryptoLib;
import saltchannel.TweetNaCl;
import saltchannel.util.KeyPair;
import saltchannel.util.Rand;
import saltchannel.util.TimeKeeper;
import saltchannel.util.ZeroTimeKeeper;
import saltchannel.v2.EncryptedChannelV2.Role;
import saltchannel.v2.packets.M1Packet;
import saltchannel.v2.packets.M2Packet;
import saltchannel.v2.packets.M3Packet;
import saltchannel.v2.packets.M4Packet;

/**
 * Server-side implementation of Salt Channel v2.
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
    private KeyPair sigKeyPair;
    private KeyPair encKeyPair;
    private M1Packet m1;
    private byte[] m1Hash;
    private M2Packet m2;
    private byte[] m2Hash;
    private M4Packet m4;
    
    public Server(KeyPair sigKeyPair, ByteChannel clearChannel) {
        this.clearChannel = clearChannel;
        this.sigKeyPair = sigKeyPair;
        this.timeKeeper = new ZeroTimeKeeper();
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
    
    /**
     * @throws NoSuchServer
     * @throws BadPeer
     */
    public void handshake() {
        checkThatEncKeyPairWasSet();        
        
        m1();
        handleNoSuchServerIfNeeded();
        
        m2();
        createEncryptedChannel();
        
        m3();
        
        m4();
        validateSignature2();
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
     * Returns the encrypted channel after a successful handshake.
     */
    public ByteChannel getChannel() {
        return this.encryptedChannel;
    }

    private void m1() {
        byte[] m1Bytes = clearChannel.read();
        this.m1Hash = CryptoLib.sha512(m1Bytes);
        this.m1 = M1Packet.fromBytes(m1Bytes, 0);
    }
    
    private void m2() {
        this.m2 = new M2Packet();
        m2.noSuchServer = false;
        m2.serverEncKey = this.encKeyPair.pub();
        byte[] m2Bytes = m2.toBytes();
        this.m2Hash = CryptoLib.sha512(m2Bytes);
        clearChannel.write(m2Bytes);
    }
    
    private void m4() {
        this.m4 = M4Packet.fromBytes(encryptedChannel.read(), 0);
    }
    
    private void createEncryptedChannel() {
        byte[] sharedKey = CryptoLib.computeSharedKey(encKeyPair.sec(), m1.clientEncKey);
        this.encryptedChannel = new EncryptedChannelV2(this.clearChannel, sharedKey, Role.SERVER);
    }

    
    private void m3() {
        M3Packet p = new M3Packet();
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
        m2.noSuchServer = true;
        m2.serverEncKey = new byte[32];
        byte[] raw = new byte[m2.getSize()];
        m2.toBytes(raw, 0);
        return raw;
    }
}
