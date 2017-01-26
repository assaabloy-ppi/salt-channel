package saltchannel.v2;

import org.junit.Test;

public class ResumeHandlerTest {
    
    @Test
    public void testOne() {
        ResumeHandler r1 = r1();
        // TODO A. implement testOne()
    }

    private static ResumeHandler r1() {
        byte[] key = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        long first = 10;
        int size = 1000;
        return new ResumeHandler(key, first, size);
    }
}
