package saltchannel.v2;

import java.util.Arrays;
import saltchannel.ByteChannel;
import saltchannel.ComException;
import saltchannel.TweetNaCl;
import saltchannel.util.Bytes;
import saltchannel.v2.packets.EncryptedPacket;

/**
 * An implementation of an encrypted channel using a shared symmetric 
 * session key.
 * 
 * @author Frans Lundberg
 */
public class EncryptedChannelV2 implements ByteChannel {
    private long readNonceInteger;
    private byte[] readNonceBytes = new byte[TweetNaCl.BOX_NONCE_BYTES];
    private long writeNonceInteger;
    private byte[] writeNonceBytes = new byte[TweetNaCl.BOX_NONCE_BYTES];
    private byte[] key;
    private ByteChannel channel;
    
    /**
     * Creates a new EncryptedChannel given the underlying channel to be 
     * encrypted, the key and the role of the peer (client or server).
     * 
     * @param key  
     *      Shared symmetric encryption key for one session. 
     *      A new key must be used for every session.
     */
    public EncryptedChannelV2(ByteChannel channel, byte[] key, Role role) {
        if (key.length != TweetNaCl.BOX_SECRET_KEY_BYTES) {
            throw new IllegalArgumentException("bad key size, should be " + TweetNaCl.BOX_SECRET_KEY_BYTES);
        }
        
        this.channel = channel;
        this.key = key;
        
        switch (role) {
        case CLIENT:
            setWriteNonce(1);
            setReadNonce(2);
            break;
        case SERVER:
            setWriteNonce(2);
            setReadNonce(1);
            break;
        default:
            throw new Error("never happens");
        }
    }
    
    /**
     * Role of this peer of the encrypted channel.
     * Used for nonce handling.
     */
    public static enum Role {
        CLIENT, SERVER
    }

    @Override
    public byte[] read() throws ComException {
        byte[] encrypted = channel.read();
        encrypted = unwrap(encrypted);
        byte[] clear = decrypt(encrypted);
        increaseReadNonce();
        return clear;
    }

    @Override
    public void write(byte[]... messages) throws ComException {
        byte[][] toWrite = new byte[messages.length][];
        
        for (int i = 0; i < messages.length; i++) {
            byte[] encrypted = encrypt(messages[i]);
            toWrite[i] = wrap(encrypted);
            increaseWriteNonce();
        }
        
        channel.write(toWrite);
    }
    
    /**
     * @throws ComException
     */
    byte[] decrypt(byte[] encrypted) {
        if (encrypted == null) {
            throw new Error("encrypted == null");
        }
        
        byte[] clear;
        byte[] c = new byte[TweetNaCl.SECRETBOX_OVERHEAD_BYTES + encrypted.length];
        byte[] m = new byte[c.length];
        System.arraycopy(encrypted, 0, c, TweetNaCl.SECRETBOX_OVERHEAD_BYTES, encrypted.length);
        if (c.length < 32) {
            throw new ComException("ciphertext too small");
        }
        
        if (TweetNaCl.crypto_box_open_afternm(m, c, c.length, readNonceBytes, key) != 0) {
            throw new ComException("invalid encryption");
        }
        
        clear = Arrays.copyOfRange(m, TweetNaCl.SECRETBOX_INTERNAL_OVERHEAD_BYTES, m.length);
        return clear;
    }
    
    /**
     * Needed by ServerChannelV2.
     */
    byte[] encryptAndIncreaseWriteNonce(byte[] bytes) {
        byte[] encrypted = wrap(encrypt(bytes));
        increaseWriteNonce();
        return encrypted;
    }
    
    byte[] encrypt(byte[] clear) {
        byte[] m = new byte[TweetNaCl.SECRETBOX_INTERNAL_OVERHEAD_BYTES + clear.length];
        byte[] c = new byte[m.length];
        System.arraycopy(clear, 0, m, TweetNaCl.SECRETBOX_INTERNAL_OVERHEAD_BYTES, clear.length);
        TweetNaCl.crypto_box_afternm(c, m, m.length, writeNonceBytes, key);
        return Arrays.copyOfRange(c, TweetNaCl.SECRETBOX_OVERHEAD_BYTES, c.length);
    }
    
    private void setWriteNonce(long nonceInteger) {
        this.writeNonceInteger = nonceInteger;
        updateWriteNonceBytes();
    }
    
    /**
     * Not private intentionally. Used by ServerChannel.
     */
    void increaseWriteNonce() {
        setWriteNonce(writeNonceInteger + 2);
    }
    
    private void setReadNonce(long nonceInteger) {
        this.readNonceInteger = nonceInteger;
        updateReadNonceBytes();
    }
    
    private void increaseReadNonce() {
        setReadNonce(readNonceInteger + 2);
    }
    
    private void updateReadNonceBytes() {
        Bytes.longToBytesLE(readNonceInteger, readNonceBytes, 0);
    }
    
    private void updateWriteNonceBytes() {
        Bytes.longToBytesLE(writeNonceInteger, writeNonceBytes, 0);
    }
    
    /**
     * Wrap encrypted bytes in EncryptedPacket.
     */
    static byte[] wrap(byte[] bytes) {
        EncryptedPacket p = new EncryptedPacket();
        p.body = bytes;
        byte[] result = new byte[p.getSize()];
        p.toBytes(result, 0);
        return result;
    }
    
    static byte[] unwrap(byte[] packetBytes) {
        EncryptedPacket p = EncryptedPacket.fromBytes(packetBytes, 0, packetBytes.length);
        return p.body;
    }
}
