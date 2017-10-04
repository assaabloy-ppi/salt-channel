package saltaa.nperf;

public abstract class NTest {
    
    /**
     * Result; time of best run (seconds).
     */
    public double time;
    
    /**
     * Number of times the test was run.
     */
    public int runCount;
    
    /**
     * Name of test, suitable to override.
     */
    public String name() {
        return this.getClass().getName();
    }
    
    /**
     * Initializes the test. Called once before run() by test runner.
     * Suitable to override; the implementation in this class does nothing.
     */
    public void init() {}
    
    /**
     * Runs the test once.
     * run() can be called repeatedly on the same object.
     */
    public abstract void run();
}
