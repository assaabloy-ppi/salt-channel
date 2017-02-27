package saltchannel.util;

/**
 * A TimeKeeper implementation that does not keep time.
 * 
 * @author Frans Lundberg
 */
public class ZeroTimeKeeper implements TimeKeeper {
    @Override
    public int getFirstTime() {
        return 0;
    }

    @Override
    public int getTime() {
        return 0;
    }
}
