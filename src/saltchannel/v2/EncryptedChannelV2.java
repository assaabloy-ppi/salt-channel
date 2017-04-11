package saltchannel.v2;

import java.util.Arrays;
import saltchannel.ByteChannel;
import saltchannel.ComException;
//import saltchannel.TweetNaCl;

import saltaa.*;

import saltchannel.util.Bytes;
import saltchannel.v2.packets.EncryptedPacket;
import saltchannel.v2.packets.TTPacket;

/**
 * An implementation of an encrypted channel using a shared symmetric 
 * session key.
 * 
 * @author Frans Lundberg
 */
public class EncryptedChannelV2 implements ByteChannel {
    private long readNonceInteger;
    private byte[] readNonceBytes = new byte[SaltLib.crypto_box_NONCEBYTES];
    private long writeNonceInteger;
    private byte[] writeNonceBytes = new byte[SaltLib.crypto_box_NONCEBYTES];
    private byte[] key;
    private final ByteChannel channel;
    private byte[] pushbackMessage;
    private byte[] sessionNonce;
    
    private SaltLib salt = SaltLibFactory.getLib(SaltLibFactory.LibType.NATIVE);

    /**
     * Creates a new EncryptedChannel given the underlying channel to be 
     * encrypted, the key and the role of the peer (client or server).
     * 
     * @param key  
     *      Shared symmetric encryption key for one session. 
     *      A new key must be used for every session.
     * @param ticketId
     *      This is zero for ordinary sessions and ticketId for resumed sessions.
     *      ticketId is used to create nonce for encryption/decryption.
     */
    public EncryptedChannelV2(ByteChannel channel, byte[] key, Role role) {
        this(channel, key, role, zeroSessionNonce());
    }
    
    private static byte[] zeroSessionNonce() {
        return new byte[TTPacket.SESSION_NONCE_SIZE];
    }
    
    public EncryptedChannelV2(ByteChannel channel, byte[] key, Role role, byte[] sessionNonce) {
        if (key.length != SaltLib.crypto_box_SECRETKEYBYTES) {
            throw new IllegalArgumentException("bad key size, should be " + SaltLib.crypto_box_SECRETKEYBYTES);
        }
        
        this.channel = channel;
        this.key = key;
        this.sessionNonce = sessionNonce;
        
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
    
    public void pushback(byte[] pushbackMessage) {
        this.pushbackMessage = pushbackMessage;
    }

    @Override
    public byte[] read() throws ComException {
        byte[] encrypted = readOrTakePushback();
        encrypted = unwrap(encrypted);
        byte[] clear = decrypt(encrypted);
        increaseReadNonce();
        return clear;
    }
    
    private byte[] readOrTakePushback() {
        byte[] bytes;
        
        if (this.pushbackMessage != null) {
            bytes = this.pushbackMessage;
            this.pushbackMessage = null;
        } else {
            bytes = channel.read();
        }
        
        return bytes;
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
        byte[] c = new byte[SaltLib.crypto_secretbox_OVERHEAD_BYTES + encrypted.length];
        byte[] m = new byte[c.length];
        System.arraycopy(encrypted, 0, c, SaltLib.crypto_secretbox_OVERHEAD_BYTES, encrypted.length);
        if (c.length < 32) {
            throw new ComException("ciphertext too small");
        }
        
        try {
            salt.crypto_box_open_afternm(m, c, readNonceBytes, key);            
        }
        catch(BadEncryptedDataException e)
        {
            throw new ComException("invalid encryption");
        }         

        clear = Arrays.copyOfRange(m, SaltLib.crypto_secretbox_INTERNAL_OVERHEAD_BYTES, m.length);
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
        byte[] m = new byte[SaltLib.crypto_secretbox_INTERNAL_OVERHEAD_BYTES + clear.length];
        byte[] c = new byte[m.length];
        System.arraycopy(clear, 0, m, SaltLib.crypto_secretbox_INTERNAL_OVERHEAD_BYTES, clear.length);        
        salt.crypto_box_afternm(c, m, writeNonceBytes, key);
        return Arrays.copyOfRange(c, SaltLib.crypto_secretbox_OVERHEAD_BYTES, c.length);
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
        System.arraycopy(sessionNonce, 0, readNonceBytes, 8, TTPacket.SESSION_NONCE_SIZE);
    }
    
    private void updateWriteNonceBytes() {
        Bytes.longToBytesLE(writeNonceInteger, writeNonceBytes, 0);
        System.arraycopy(sessionNonce, 0, writeNonceBytes, 8, TTPacket.SESSION_NONCE_SIZE);
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
