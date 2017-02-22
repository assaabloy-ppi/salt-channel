package saltchannel.v1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import saltchannel.BadPeer;
import saltchannel.ByteChannel;
import saltchannel.ComException;
import saltchannel.CryptoLib;
import saltchannel.TweetNaCl;
import saltchannel.util.KeyPair;
import saltchannel.util.Rand;
import saltchannel.v1.BinsonLight.Parser;

/**
 * Server-side implementation of Salt Channel.
 * Usage: create object, call handshake(), call read()/write() methods.
 * Do not reuse the object for more than one Salt Channel session.
 * 
 * @author Frans Lundberg
 */
public class ServerChannelV1 implements ByteChannel {
    private final ByteChannel clearChannel;
    private EncryptedChannelV1 encryptedChannel;
    private byte[] peerId;
    
    public ServerChannelV1(ByteChannel clearChannel) {
        this.clearChannel = clearChannel;
    }
    
    /**
     * Performs the Salt Channel handshake.
     * Note, there is currently no support for handling s-field
     * (virtual servers).
     * 
     * @param sigKeyPair  The server's long-term signing key pair.
     * @param encKeyPair  Ephemeral encryption key pair, for this session only.
     * @throws ComException
     */
    public void handshake(KeyPair sigKeyPair, Rand rand) {
        KeyPair ephemeralKeyPair = CryptoLib.createEncKeys(rand);
        handshake(sigKeyPair, ephemeralKeyPair);
    }
    
    /**
     * Performs the Salt Channel handshake.
     * Note, there is currently no support for handling s-field
     * (virtual servers).
     * 
     * @param sigKeyPair  The server's long-term signing key pair.
     * @param encKeyPair  Ephemeral encryption key pair, for this session only.
     * @throws ComException
     */
    public void handshake(KeyPair sigKeyPair, KeyPair encKeyPair) {
        byte[] m1 = clearChannel.read();
        BinsonLight.Parser m1Parser = new BinsonLight.Parser(m1);
        
        byte[] eField = parseM1e(m1Parser);
        String pField = parseM1p(m1Parser);
        parseM1s(m1Parser);
        
        checkM1P(pField);
        checkM1E(eField);
        
        byte[] sharedKey = CryptoLib.computeSharedKey(encKeyPair.sec(), eField);
        byte[] mySignature = CryptoLib.createSaltChannelV1Signature(sigKeyPair, encKeyPair.pub(), eField);

        encryptedChannel = new EncryptedChannelV1(clearChannel, sharedKey, EncryptedChannelV1.Role.SERVER);
        
        byte[] m2 = createM2(encKeyPair.pub());
        
        byte[] m3 = createM3(mySignature, sigKeyPair.pub());
        byte[] m3encrypted = encryptedChannel.encryptAndIncreaseWriteNonce(m3);

        clearChannel.write(m2, m3encrypted);
        
        byte[] m4 = encryptedChannel.read();
        BinsonLight.Parser m4Parser = new BinsonLight.Parser(m4);
        
        byte[] cField = parseM4c(m4Parser);
        byte[] gField = parseM4g(m4Parser);
        
        try {
            CryptoLib.checkSaltChannelV1Signature(cField, encKeyPair.pub(), eField, gField);
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
