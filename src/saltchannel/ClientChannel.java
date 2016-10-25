package saltchannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import saltchannel.util.BinsonLight;
import saltchannel.util.Hex;
import saltchannel.util.BinsonLight.Parser;

/**
 * The client-side of a secure channel ("Salt Channel").
 * 
 * @author Frans Lundberg
 */
public class ClientChannel implements ByteChannel {
    private final TweetLib tweet;
    private ByteChannel clearChannel;
    private EncryptedChannel encryptedChannel;
    private volatile byte[] m4Buffered = null;
    private byte[] actualServerKey;
    private KeyPair encKeyPair;
    
    public ClientChannel(TweetLib tweet, ByteChannel clearChannel) {
        this.tweet = tweet;
        this.clearChannel = clearChannel;
    }
    
    /**
     * Sets the ephemeral encryption key pair to use for the handshake.
     * This method is normally *not* used. It is suitable for deterministic 
     * testing. The ephemeral key pair is normally created during the handshake.
     */
    public void initEphemeralKeyPair(KeyPair encKeyPair) {
        this.encKeyPair = encKeyPair;
    }
    
    /**
     * Performs handshake with any server independent of the server's 
     * public key.
     * 
     * @param sigKeys
     *      The long-term signing key pair, sigKeys.sec() is highly confidential.
     */
    public void handshake(KeyPair sigKeys) {
        handshake(sigKeys, null, false);
    }
    
    /**
     * @param sigKeys
     *      The long-term signing key pair, sigKeys.sec() is highly confidential.
     * @param serverSigPub
     *      Expected public signing key of server, "server ID".
     */
    public void handshake(KeyPair sigKeys, byte[] serverSigPub) {
        handshake(sigKeys, serverSigPub, false);
    }
    
    /**
     * @param flush  If true, the third-way message (m4) from client to server 
     *      will be flushed and sent immediately instead of with the first application message.
     *      No flushing may save a round-trip to the server.
     * @throws BadPeer
     * @throws ComException
     * @throws NoSuchHostException  
     *      If there is no active host at the endpoint with the given public key.
     */
    public void handshake(KeyPair sigKeys, byte[] wantedServer, boolean flush) {
        checkSigKeyPairParam(sigKeys);        
        checkWantedServerParam(wantedServer);
        createEncKeyPairIfNeeded();
        
        byte[] m1 = createM1(encKeyPair.pub(), wantedServer);
        clearChannel.write(m1);
        
        byte[] m2 = clearChannel.read();
        BinsonLight.Parser m2Parser = new BinsonLight.Parser(m2);
        byte[] eField = parseM2e(m2Parser);
        String tField = parseM2t(m2Parser);
        
        handleNoSuchServer(wantedServer, tField);
        byte[] sharedKey = tweet.computeSharedKey(encKeyPair.sec(), eField);
        encryptedChannel = new EncryptedChannel(clearChannel, sharedKey, EncryptedChannel.Role.CLIENT);
        
        byte[] m3 = encryptedChannel.read();
        BinsonLight.Parser m3Parser = new BinsonLight.Parser(m3);
        byte[] serverSignature = parseM3g(m3Parser);
        byte[] actualServerKey = parseM3s(m3Parser);
        
        checkWantedServer(wantedServer, actualServerKey);        
        tweet.checkSaltChannelSignature(actualServerKey, encKeyPair.pub(), eField, serverSignature);
        byte[] mySignature = tweet.createSaltChannelSignature(sigKeys, encKeyPair.pub(), eField);
        
        byte[] m4 = createM4(sigKeys.pub(), mySignature);
        writeOrBufferM4(flush, m4);
    }

    private void writeOrBufferM4(boolean flush, byte[] m4) {
        if (flush) {
            encryptedChannel.write(m4);
        } else {
            this.m4Buffered = m4;
        }
    }
    
    /**
     * Returns the server's public key; available after handshake.
     */
    public byte[] getServerKey() {
        byte[] key = this.actualServerKey;
        if (key != null) {
            key = key.clone();
        }
        
        return key;
    }
    
    @Override
    public byte[] read() throws ComException {
        return encryptedChannel.read();
    }

    @Override
    public void write(byte[]... messages) throws ComException {
        byte[][] messagesWithBuffered;
        
        if (m4Buffered == null) {
            messagesWithBuffered = messages;
        } else {
            messagesWithBuffered = new byte[messages.length + 1][];
            messagesWithBuffered[0] = m4Buffered;
            for (int i = 0; i < messages.length; i++) {
                messagesWithBuffered[i + 1] = messages[i];
            }
            m4Buffered = null;
        }
        
        encryptedChannel.write(messagesWithBuffered);
    }
    
    private void checkSigKeyPairParam(KeyPair sigKeys) {
        if (sigKeys.sec().length != 64) {
            throw new IllegalArgumentException("64-byte secret key for signing expected");
        }
        
        if (sigKeys.pub().length != 32) {
            throw new IllegalArgumentException("32-byte public key for signing expected");
        }
    }

    private void checkWantedServerParam(byte[] wantedServer) {
        if (wantedServer != null && wantedServer.length != TweetNaCl.SIGN_PUBLIC_KEY_BYTES) {
            throw new IllegalArgumentException("bad length of wantedServer parameter");
        }
    }

    private void createEncKeyPairIfNeeded() {
        if (this.encKeyPair == null) {
            this.encKeyPair = tweet.createEncKeys();
        }
    }
    
    private byte[] createM1(byte[] pub, byte[] wantedServer) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinsonLight.Writer w = new BinsonLight.Writer(out);
        
        try {
            w.begin();
            w.name("e").bytes(pub);
            w.name("p").string("S1");
            
            if (wantedServer != null) {
                w.name("s").bytes(wantedServer);
            }
            
            w.end().flush();
        } catch (IOException e) {
            throw new Error("never happens, " + e.toString());
        }
        
        return out.toByteArray();
    }
    
    private byte[] parseM2e(Parser p) {
        try {
            p.field("e");
        } catch (BinsonLight.FormatException e) {
            throw new BadPeer("missing M2:e field");
        }
        
        if (p.getType() != BinsonLight.ValueType.BYTES) {
            throw new BadPeer("bad type of M2:e field");
        }
        
        return p.getBytes().toByteArray();
    }

    private String parseM2t(Parser p) {
        try {
            p.field("t");
        } catch (BinsonLight.FormatException e) {
            // optional field, this is normal
            return null;
        }
        
        if (p.getType() != BinsonLight.ValueType.STRING) {
            throw new BadPeer("bad type of M2:e field");
        }
        
        return p.getString().toString();
    }

    private byte[] parseM3s(Parser p) {
        try {
            p.field("s");
        } catch (BinsonLight.FormatException e) {
            throw new BadPeer("missing M3:s field");
        }
        
        if (p.getType() != BinsonLight.ValueType.BYTES) {
            throw new BadPeer("bad type of M3:s field");
        }
        
        return p.getBytes().toByteArray();
    }

    /**
     * Parses M3g field and checks its length.
     * 
     * @throws BadPeer
     */
    private byte[] parseM3g(Parser p) {
        try {
            p.field("g");
        } catch (BinsonLight.FormatException e) {
            throw new BadPeer("missing M3:g field");
        }
        
        if (p.getType() != BinsonLight.ValueType.BYTES) {
            throw new BadPeer("bad type of M3:g field");
        }
        
        byte[] gField = p.getBytes().toByteArray();
        
        if (gField.length != TweetNaCl.SIGNATURE_SIZE_BYTES) {
            throw new BadPeer("bad signature size, " + gField.length);
        }
        
        return p.getBytes().toByteArray();
    }

    private void handleNoSuchServer(byte[] wantedServer, String tField) {
        if (tField != null) {
            if (wantedServer == null) {
                throw new ComException("got noSuchServer, but no server pubkey specified by client");
            }
            
            if (tField.equals("noSuchServer")) {
                throw new NoSuchServerException("no server with pubkey " + Hex.create(wantedServer));
            } else {
                throw new ComException("unexpected t-field string from client, " + tField);
            }
        }
    }
    
    private void checkWantedServer(byte[] wantedServer, byte[] actualServerKey) {
        if (wantedServer != null) {
            if (!Arrays.equals(actualServerKey, wantedServer)) {
                throw new BadPeer("server key mismatch, not as expected, got: " 
                        + Hex.create(actualServerKey));
            }
        }
    }
    
    private byte[] createM4(byte[] pub, byte[] mySignature) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinsonLight.Writer w = new BinsonLight.Writer(out);
        
        try {
            w.begin();
            w.name("c").bytes(pub);
            w.name("g").bytes(mySignature);
            w.end().flush();
        } catch (IOException e) {
            throw new Error("never happens, " + e.toString());
        }
        
        return out.toByteArray();
    }
}
