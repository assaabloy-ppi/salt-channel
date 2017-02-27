package saltchannel.util;

public class SystemClockTimeKeeper implements TimeKeeper {
    private boolean isInited = false;
    private long t0;
    
    @Override
    public int getFirstTime() {
        t0 = System.currentTimeMillis();
        isInited = true;
        return 1;
    }

    @Override
    public int getTime() {
        if (!isInited) throw new IllegalStateException("call getFirstTime first");
        long diff = System.currentTimeMillis() - t0;
        int result = (int) diff;
        if (result  < 0) {
            result = Integer.MAX_VALUE;
        }
        return result;
    }

}
