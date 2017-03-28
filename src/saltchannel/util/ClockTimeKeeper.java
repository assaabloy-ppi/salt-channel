package saltchannel.util;

public class ClockTimeKeeper implements TimeKeeper {
    private MillisClock clock;
    
    public ClockTimeKeeper(MillisClock clock) {
        this.clock = clock;
    }

    @Override
    public int getFirstTime() {
        return 1;
    }

    @Override
    public int getTime() {
        return clock.getTime();
    }
}
