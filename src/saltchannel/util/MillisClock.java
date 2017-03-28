package saltchannel.util;

/**
 * A clock with millisecond resolution.
 * Can be relative time.
 * 
 * @author Frans Lundberg
 */
public interface MillisClock {
    /**
     * Returns a positive value, time elapsed since a fixed
     * point in time.
     */
    public int getTime();
}
