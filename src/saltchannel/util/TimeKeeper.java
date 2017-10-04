package saltchannel.util;

/**
 * Keeps time; relative time since first message sent. Measure in milliseconds.
 * 
 * @author Frans Lundberg
 */
public interface TimeKeeper { 
    /**
     * Call this first, must return 1 when timing is supported
     * and 0 if not.
     */
    public int getFirstTime();
    
    /**
     * Returns time in millis passed since getFirstTime() was called
     * or 0 if timing is not supported.
     */
    public int getTime();
    
    public static final NullTimeKeeper NULL = new NullTimeKeeper();
}
