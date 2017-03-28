package saltchannel.util;

public interface TimeChecker {

    /**
     * Reports first time. Should be 0 or 1.
     */
    public void reportFirstTime(int time);
    
    /**
     * Checks 'time' against this object's clock.
     * 
     * @throws TimeException if an unexpected time difference
     * was detected.
     */
    public void checkTime(int time);
}
