package saltchannel.testutil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * An instance of this class is used as something to wait for.
 * One or more thread can wait (waitForIt()) and one thread can call
 * reportHappened() to report that the event has happened.
 * The waiting threads will then continue executing.
 * 
 * A ToWaitFor object must only be used once.
 *
 * @author Frans Lundberg
 */
public class ToWaitFor {
    private final CountDownLatch latch;
    
    public ToWaitFor() {
        latch = new CountDownLatch(1);
    }

    /**
     * Reports that the event has happened.
     * This method should only be called once; if called 
     * more than once, subsequent calls have no effect.
     */
    public void reportHappened() {
        latch.countDown();
    }

    /**
     * Waits for event to occur, or returns immediately if it has occurred. The
     * method also returns immediately if the calling thread is interrupted.
     * Waiting is constrained by the time parameter.
     * 
     * @param time
     *      Maximum time to wait in milliseconds.
     * @return 
     *      true if the event has happened, false if this method returns 
     *      because of a timeout or because this thread was interrupted
     */
    public boolean waitForIt(long millis) {
        try {
            return latch.await(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * Returns true if the event actually happened.
     * 
     * @return true if happened.
     */
    public boolean hasHappened() {
        return latch.getCount() == 0;
    }
}