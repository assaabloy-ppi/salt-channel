package saltchannel.util;

/**
 * Simple key pair class. Stores secret-public key pair in the format of byte arrays.
 * 
 * @author Frans Lundberg
 */
public class KeyPair {
    private final byte[] sec;
    private final byte[] pub;

    /**
     * Creates a new key pair.
     * 
     * @param sec  Secret key.
     * @param pub  Public key.
     */
    public KeyPair(byte[] sec, byte[] pub) {
        if (sec == null) throw new IllegalArgumentException("sec == null not allowed");
        if (pub == null) throw new IllegalArgumentException("pub == null not allowed");
        
        this.sec = sec;
        this.pub = pub;
    }
    
    /**
     * Creates and returns a KeyPair from two hex strings.
     * 
     * @param sec  Hex string of secret key.
     * @param pub  Hex string of public key.
     */
    public static KeyPair fromHex(String sec, String pub) {
        return new KeyPair(Hex.toBytes(sec), Hex.toBytes(pub));
    }
    
    public byte[] pub() {
        return pub;
    }
    
    public byte[] sec() {
        return sec;
    }
    
    public String toString() {
        return "sec:x, pub:" + Hex.create(pub);
    }
    
    public String toStringIncludingPrivateKey() {
        return "sec:" + Hex.create(sec) + ", pub:" + Hex.create(pub);
    }
}
