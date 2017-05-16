package saltchannel.util;

import saltchannel.TimeException;

/**
 * Simple TimeChecker that checks whether time difference is 
 * within a tolerance. If not, a TimeException is thrown from
 * checkTime().
 * 
 * @author Frans Lundberg
 */
public class TypicalTimeChecker implements TimeChecker {
    private MillisClock clock;
    private long first;
    private int tolerance;
    
    /**
     * 
     * @param clock
     *          The clock to use.
     * @param tolerance
     *          Max allowed clock difference in milliseconds.
     */
    public TypicalTimeChecker(MillisClock clock, int tolerance) {
        this.clock = clock;
        this.tolerance = tolerance;
    }

    @Override
    public void reportFirstTime(int time) {
        this.first = clock.getTime();
    }

    @Override
    public void checkTime(int time) {
        long myTime = clock.getTime() - this.first;
        long diff = Math.abs(time - myTime);
        if (diff > tolerance) {
            throw new TimeException("diff " +  diff + " greater than tolerance");
        }
    }
}
