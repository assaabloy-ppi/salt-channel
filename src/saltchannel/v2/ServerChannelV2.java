package saltchannel.v2;

import java.util.Arrays;
import saltchannel.BadPeer;
import saltchannel.ByteChannel;
import saltchannel.CryptoLib;
import saltchannel.TweetNaCl;
import saltchannel.util.KeyPair;
import saltchannel.util.Rand;
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
public class ServerChannelV2 {
    private final ByteChannel clearChannel;
    private EncryptedChannelV2 encryptedChannel;
    private KeyPair sigKeyPair;
    private KeyPair encKeyPair;
    private byte[] clientSigKey;
    private M1Packet m1;
    private M2Packet m2;
    private M4Packet m4;
    
    public ServerChannelV2(KeyPair sigKeyPair, ByteChannel clearChannel) {
        this.clearChannel = clearChannel;
        this.sigKeyPair = sigKeyPair;
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
        if (encKeyPair == null) {
            throw new IllegalStateException("encKeyPair must be set before calling handshake()");
        }
        
        readM1();
        
        if (m1.hasServerSigKey() && !Arrays.equals(this.sigKeyPair.pub(), m1.serverSigKey)) {
            clearChannel.write(noSuchServerM2Raw());
            throw new NoSuchServer();
        }
        
        clearChannel.write(m2Raw());
        
        createEncryptedChannel();
        
        encryptedChannel.write(m3Raw());
        
        readM4();
        
        validateSignature2();
    }
    
    /**
     * Returns the static (signing) public key of the client.
     * Available after a successful handshake.
     */
    public byte[] getClientSigKey() {
        return this.clientSigKey;
    }

    private void readM1() {
        this.m1 = M1Packet.fromBytes(clearChannel.read(), 0);
    }
    
    private void readM4() {
        M4Packet m4 = M4Packet.fromBytes(encryptedChannel.read(), 0);
        this.clientSigKey = m4.clientSigKey;
    }
    
    private void createEncryptedChannel() {
        byte[] sharedKey = CryptoLib.computeSharedKey(encKeyPair.sec(), m1.clientEncKey);
        this.encryptedChannel = new EncryptedChannelV2(this.clearChannel, sharedKey, Role.SERVER);
    }
    
    private byte[] m2Raw() {
        this.m2 = new M2Packet();
        m2.noSuchServer = false;
        m2.serverEncKey = this.encKeyPair.pub();
        byte[] raw = new byte[m2.getSize()];
        m2.toBytes(raw, 0);
        return raw;
    }
    
    private byte[] m3Raw() {
        M3Packet p = new M3Packet();
        p.serverSigKey = this.sigKeyPair.pub();
        p.signature1 = signature1();
        return p.toBytes();
    }
    
    /**
     * Computes M3/Signature1.
     */
    private byte[] signature1() {
        int size = 32 + 32 + 64 + 64;
        byte[] toSign = new byte[size];
        byte[] hash1 = new byte[64];
        byte[] hash2 = new byte[64];
        
        byte[] m1Bytes = m1.toBytes();
        TweetNaCl.crypto_hash(hash1, m1Bytes, m1Bytes.length);
        
        byte[] m2Bytes = m2.toBytes();
        TweetNaCl.crypto_hash(hash2, m2Bytes, m2Bytes.length);
        
        int offset = 0;
        
        System.arraycopy(this.encKeyPair.pub(), 0, toSign, offset, 32);
        offset += 32;
        System.arraycopy(this.m1.clientEncKey, 0, toSign, offset, 32);
        offset += 32;
        System.arraycopy(m1Bytes, 0, toSign, offset, 64);
        offset += 64;
        System.arraycopy(m2Bytes, 0, toSign, offset, 64);
        offset += 64;
        
        assert offset == toSign.length;
        
        byte[] signedMessage = TweetNaCl.crypto_sign(toSign, this.sigKeyPair.sec());
        byte[] signature = new byte[64];
        System.arraycopy(signedMessage, 0, signature, 0, 64);
        return signature;
    }
    
    /**
     * Validates M4/Signature2.
     * 
     * @throws BadPeer
     */
    private void validateSignature2() {
        int size = 64 + 32 + 32 + 64 + 64;  // signature + key + key + hash + hash
        byte[] signedMessage = new byte[size];
        byte[] hash1 = new byte[64];
        byte[] hash2 = new byte[64];
        
        byte[] m1Bytes = m1.toBytes();
        TweetNaCl.crypto_hash(hash1, m1Bytes, m1Bytes.length);
        
        byte[] m2Bytes = m2.toBytes();
        TweetNaCl.crypto_hash(hash2, m2Bytes, m2Bytes.length);
        
        int offset = 0;
        
        System.arraycopy(m4.signature2, 0, signedMessage, 0, 64);
        offset += 64;
        System.arraycopy(this.m1.clientEncKey, 0, signedMessage, offset, 32);
        offset += 32;
        System.arraycopy(this.encKeyPair.pub(), 0, signedMessage, offset, 32);
        offset += 32;
        System.arraycopy(m1Bytes, 0, signedMessage, offset, 64);
        offset += 64;
        System.arraycopy(m2Bytes, 0, signedMessage, offset, 64);
        offset += 64;
        
        assert offset == signedMessage.length;
        
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
    
    public static class NoSuchServer extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
