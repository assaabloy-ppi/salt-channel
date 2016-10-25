package saltchannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import saltchannel.util.BinsonLight;
import saltchannel.util.KeyPair;
import saltchannel.util.BinsonLight.Parser;

/**
 * Server-side implementation of Salt Channel.
 * 
 * @author Frans Lundberg
 */
public class ServerChannel implements ByteChannel {
    private ByteChannel clearChannel;
    private EncryptedChannel encryptedChannel;
    private ChannelCryptoLib tweet;
    private byte[] peerId;
    private KeyPair encKeyPair;
    
    public ServerChannel(ChannelCryptoLib tweet, ByteChannel clearChannel) {
        this.tweet = tweet;
        this.clearChannel = clearChannel;
        this.encKeyPair = null;
    }
    
    /**
     * Sets the ephemeral encryption key pair to use for the handshake.
     * The method is suitable for deterministic testing and when there is
     * a need to precompute the ephemeral key pair.
     * Otherwise, the ephemeral key pair is created during the handshake and
     * this method does not need to be used.
     */
    public void initEphemeralKeyPair(KeyPair encKeyPair) {
        this.encKeyPair = encKeyPair;
    }
    
    /**
     * Performs the Salt Channel handshake.
     * Note, there is currently no support for handling s-field
     * (virtual servers).
     * 
     * @param sigKeys  The server's long-term signing key pair.
     * @throws ComException
     */
    public void handshake(KeyPair sigKeys) {
        byte[] m1 = clearChannel.read();
        BinsonLight.Parser m1Parser = new BinsonLight.Parser(m1);
        
        byte[] eField = parseM1e(m1Parser);
        String pField = parseM1p(m1Parser);
        parseM1s(m1Parser);
        
        checkM1P(pField);
        checkM1E(eField);
        
        if (encKeyPair == null) {
            encKeyPair = tweet.createEncKeys();  
        }
        
        byte[] sharedKey = tweet.computeSharedKey(encKeyPair.sec(), eField);
        byte[] mySignature = tweet.createSaltChannelSignature(sigKeys, encKeyPair.pub(), eField);

        encryptedChannel = new EncryptedChannel(clearChannel, sharedKey, EncryptedChannel.Role.SERVER);
        
        byte[] m2 = createM2(encKeyPair.pub());
        
        byte[] m3 = createM3(mySignature, sigKeys.pub());
        byte[] m3encrypted = encryptedChannel.encryptAndIncreaseWriteNonce(m3);

        clearChannel.write(m2, m3encrypted);
        
        byte[] m4 = encryptedChannel.read();
        BinsonLight.Parser m4Parser = new BinsonLight.Parser(m4);
        
        byte[] cField = parseM4c(m4Parser);
        byte[] gField = parseM4g(m4Parser);
        
        try {
            tweet.checkSaltChannelSignature(cField, encKeyPair.pub(), eField, gField);
        } catch (ComException e) {
            throw new ComException("invalid handskake signature from client");
        }
        
        this.peerId = cField;
    }

    /**
     * Available after handshake has completed this 
     * method returns the public signing key of the client.
     */
    public byte[] getPeerPublicKey() {
        return peerId.clone();
    }

    @Override
    public byte[] read() throws ComException {
        return encryptedChannel.read();
    }

    @Override
    public void write(byte[]... messages) throws ComException {
        encryptedChannel.write(messages);
    }
    
    private byte[] parseM1e(Parser p) {
        try {
            p.field("e");
        } catch (BinsonLight.FormatException e) {
            throw new BadPeer("missing field M1:e");
        }
        
        if (p.getType() != BinsonLight.ValueType.BYTES) {
            throw new BadPeer("bad type of field M1:e");
        }
        
        return p.getBytes().toByteArray();
    }
    
    private void checkM1E(byte[] eField) {
        if (eField.length != TweetNaCl.BOX_PUBLIC_KEY_BYTES) {
            throw new BadPeer("client encryption key of bad length, " + eField.length);
        }
    }
    
    private String parseM1p(Parser p) {
        try {
            p.field("p");
        } catch (BinsonLight.FormatException e) {
            throw new BadPeer("missing field M1:p");
        }
        
        if (p.getType() != BinsonLight.ValueType.STRING) {
            throw new BadPeer("bad type of field M1:p");
        }
        
        return p.getString().toString();
    }

    private void checkM1P(String pField) {
        if (!pField.equals("S1")) {
            throw new ComException("protocol (p=" + pField + ") not supported");
        }
    }
    
    private byte[] parseM1s(Parser p) {
        try {
            p.field("s");
        } catch (BinsonLight.FormatException e) {
            // Fine, this field is optional.
            return null;
        }
        
        if (p.getType() != BinsonLight.ValueType.BYTES) {
            throw new BadPeer("bad type of field M1:s");
        }
        
        return p.getBytes().toByteArray();
    }
    
    private byte[] parseM4c(BinsonLight.Parser p) {
        try {
            p.field("c");
        } catch (BinsonLight.FormatException e) {
            throw new BadPeer("bad M4 message from client, missing c-field");
        }
        
        if (p.getType() != BinsonLight.ValueType.BYTES) {
            throw new BadPeer("bad M4 message from client, bad type of c-field");
        }
        
        return p.getBytes().toByteArray();
    }
    
    private byte[] parseM4g(BinsonLight.Parser p) {
        try {
            p.field("g");
        } catch (BinsonLight.FormatException e) {
            throw new BadPeer("bad M4 message from client, missing g-field");
        }
        
        if (p.getType() != BinsonLight.ValueType.BYTES) {
            throw new BadPeer("bad M4 message from client, bad type of g-field");
        }
        
        return p.getBytes().toByteArray();
    }
    
    private byte[] createM2(byte[] eField) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinsonLight.Writer w = new BinsonLight.Writer(out);
        
        try {
            w.begin()
                .name("e").bytes(eField)
            .end().flush();
        } catch (IOException e) {
            throw new Error("never happens, " + e.toString());
        }
        
        return out.toByteArray();
    }
    
    private byte[] createM3(byte[] gField, byte[] sField) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinsonLight.Writer w = new BinsonLight.Writer(out);
        
        try {
            w.begin()
                .name("g").bytes(gField)
                .name("s").bytes(sField)
            .end().flush();
        } catch (IOException e) {
            throw new Error("never happens, " + e.toString());
        }
        
        return out.toByteArray();
    }
}
