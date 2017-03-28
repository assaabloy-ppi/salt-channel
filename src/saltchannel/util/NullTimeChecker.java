package saltchannel.util;

import saltchannel.BadPeer;

/**
 * TimeChecker that accepts all time values.
 * 
 * @author Frans Lundberg
 */
public class NullTimeChecker implements TimeChecker {
    public static final NullTimeChecker INSTANCE = new NullTimeChecker();

    @Override
    public void reportFirstTime(int time) {
        if (time != 0 && time != 1) {
            throw new BadPeer("bad first time, " + time);
        }
    }

    @Override
    public void checkTime(int time) {
        // return
    }
}
