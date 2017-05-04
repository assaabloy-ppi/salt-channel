package saltchannel.v1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import saltchannel.ByteChannel;
import saltchannel.ComException;
import saltaa.*;
import saltchannel.util.BinsonLight;
import saltchannel.util.Bytes;

/**
 * An implementation of an encrypted channel using a shared symmetric 
 * session key.
 * 
 * @author Frans Lundberg
 */
public class EncryptedChannelV1 implements ByteChannel {
    private long readNonceInteger;
    private byte[] readNonceBytes = new byte[SaltLib.crypto_box_NONCEBYTES];
    private long writeNonceInteger;
    private byte[] writeNonceBytes = new byte[SaltLib.crypto_box_NONCEBYTES];
    private byte[] key;
    private ByteChannel channel;
    private SaltLib salt = SaltLibFactory.getLib();
    
    /**
     * Creates a new EncryptedChannel given the underlying channel to be 
     * encrypted, the key and the role of the peer (client or server).
     * 
     * @param key  
     *      Shared symmetric encryption key for one session. 
     *      A new key must be used for every session.
     */
    public EncryptedChannelV1(ByteChannel channel, byte[] key, Role role) {
        if (key.length != SaltLib.crypto_box_SECRETKEYBYTES) {
            throw new IllegalArgumentException("bad key size, should be " + SaltLib.crypto_box_SECRETKEYBYTES);
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
    private byte[] decrypt(byte[] encrypted) {
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
     * Needed by ServerChannelV1.
     */
    byte[] encryptAndIncreaseWriteNonce(byte[] bytes) {
        byte[] encrypted = wrap(encrypt(bytes));
        increaseWriteNonce();
        return encrypted;
    }
    
    private byte[] encrypt(byte[] clear) {
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
