package saltchannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import saltchannel.util.BinsonLight;
import saltchannel.util.Bytes;

/**
 * Provides an encrypted channel that uses a shared symmetric session key.
 * 
 * @author Frans Lundberg
 */
public class EncryptedChannel implements ByteChannel {
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
    public EncryptedChannel(ByteChannel channel, byte[] key, Role role) {
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
        byte[] clear;
        byte[] encrypted = channel.read();
        
        encrypted = unwrap(encrypted);
        
        try {
            clear = TweetNaCl.secretbox_open(encrypted, readNonceBytes, key);
        } catch (IllegalStateException e) {
            throw new ComException("invalid encrypted data");
        }
        
        increaseReadNonce();
        
        return clear;
    }

    @Override
    public void write(byte[]... messages) throws ComException {
        byte[][] toWrite = new byte[messages.length][];
        
        for (int i = 0; i < messages.length; i++) {
            toWrite[i] = wrap(encrypt(messages[i]));
            increaseWriteNonce();
        }
        
        channel.write(toWrite);
    }
    
    /**
     * Needed by ServerChannel.
     */
    byte[] encryptAndIncreaseWriteNonce(byte[] bytes) {
        byte[] encrypted = wrap(encrypt(bytes));
        increaseWriteNonce();
        return encrypted;
    }
    
    private byte[] encrypt(byte[] clear) {
        byte[] encrypted = TweetNaCl.secretbox(clear, writeNonceBytes, key);
        return encrypted;
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
     * Wrap encrypted bytes in a Binson object.
     */
    private byte[] wrap(byte[] bytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinsonLight.Writer w = new BinsonLight.Writer(out);
        try {
            w.begin()
                .name("b").bytes(bytes)
            .end().flush();
        } catch (IOException e) {
            throw new Error("never happens");
        }
        
        return out.toByteArray();
    }
    
    private byte[] unwrap(byte[] binsonBytes) {
        BinsonLight.Parser p = new BinsonLight.Parser(binsonBytes);
        
        try {
            p.field("b");
        } catch (BinsonLight.FormatException e) {
            throw new ComException("bad format, no b-field");
        }
        
        BinsonLight.BytesValue bytesValue = p.getBytes();
        if (bytesValue == null) {
            throw new ComException("bad format of encrypted message, bad b-field");
        }
        
        return bytesValue.toByteArray();
    }
}
