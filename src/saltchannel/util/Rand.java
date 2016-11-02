package saltchannel.util;

/**
 * Interface for a random number source.
 * 
 * @author Frans Lundberg
 */
public interface Rand {
    
    /**
     * Sets the bytes in the array to random bytes.
     */
    public void randomBytes(byte[] b);
}
