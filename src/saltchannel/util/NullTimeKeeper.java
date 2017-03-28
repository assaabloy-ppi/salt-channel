package saltchannel.util;

/**
 * A TimeKeeper implementation that does not keep time.
 * getFirstTime() and getTime() always returns 0.
 * 
 * @author Frans Lundberg
 */
public class NullTimeKeeper implements TimeKeeper {
    public static final NullTimeKeeper INSTANCE = new NullTimeKeeper();
    
    @Override
    public int getFirstTime() {
        return 0;
    }

    @Override
    public int getTime() {
        return 0;
    }
}
