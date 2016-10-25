package saltchannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import saltchannel.util.BinsonLight;
import saltchannel.util.BinsonLight.Parser;

/**
 * Server implementation of Salt Channel channel.
 * 
 * @author Frans Lundberg
 */
public class ServerChannel implements ByteChannel {
    private ByteChannel clearChannel;
    private EncryptedChannel encryptedChannel;
    private TweetLib tweet;
    private byte[] peerId;
    private KeyPair encKeyPair;
    
    public ServerChannel(TweetLib tweet, ByteChannel clearChannel) {
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
        
        byte[] eField = parseM1E(m1Parser);
        String pField = parseM1P(m1Parser);
        parseM1S(m1Parser);
        
        if (!pField.equals("S1")) {
            throw new ComException("protocol (p=" + pField + ") not supported");
        }
        
        if (eField.length != TweetNaCl.BOX_PUBLIC_KEY_BYTES) {
            throw new BadPeer("client encryption key of bad length, " + eField.length);
        }
        
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
        byte[] cField = handleM4(m4, eField);
        
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
    
    private byte[] parseM1E(Parser parser) {
        try {
            parser.field("e");
        } catch (BinsonLight.FormatException e) {
            throw new ComException("missing field M1:e");
        }
        
        if (parser.getBytes() == null) {
            throw new ComException("bad type of field M1:e");
        }
        
        return parser.getBytes().toByteArray();
    }
    
    private String parseM1P(Parser parser) {
        try {
            parser.field("p");
        } catch (BinsonLight.FormatException e) {
            throw new ComException("missing field M1:p");
        }
        
        if (parser.getString() == null) {
            throw new ComException("bad type of field M1:p");
        }
        
        return parser.getString().toString();
    }
    
    private byte[] parseM1S(Parser parser) {
        try {
            parser.field("s");
        } catch (BinsonLight.FormatException e) {
            // Fine, this field is optional.
            return null;
        }
        
        if (parser.getBytes() == null) {
            throw new ComException("bad type of field M1:s");
        }
        
        return parser.getBytes().toByteArray();
    }
    
    private byte[] handleM4(byte[] m4Bytes, byte[] peerE) {
        byte[] cField;
        byte[] gField;
        BinsonLight.Parser p = new BinsonLight.Parser(m4Bytes);
        
        try {
            p.field("c");
        } catch (BinsonLight.FormatException e) {
            throw new ComException("bad message from client, missing c-field");
        }
        
        if (p.getBytes() == null) {
            throw new ComException("bad message from client, bad type of c-field");
        }
        
        cField = p.getBytes().toByteArray();
        
        try {
            p.field("g");
        } catch (BinsonLight.FormatException e) {
            throw new ComException("bad message from client, missing g-field");
        }
        
        if (p.getBytes() == null) {
            throw new ComException("bad message from client, bad type of g-field");
        }
        
        gField = p.getBytes().toByteArray();
        
        try {
            tweet.checkSaltChannelSignature(cField, encKeyPair.pub(), peerE, gField);
        } catch (ComException e) {
            throw new ComException("invalid handshake signature from client");
        }
        
        return cField;
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
