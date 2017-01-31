package saltchannel.v2.server;

import org.junit.Test;

import saltchannel.v2.server.ResumeHandler;

public class ResumeHandlerTest {
    
    @Test
    public void testOne() {
        ResumeHandler r1 = r1();
        // TODO A. implement testOne()
    }

    private static ResumeHandler r1() {
        byte[] key = new byte[32];
        key[8] = 8;
        long first = 10;
        int size = 1000;
        return new ResumeHandler(key, first, size);
    }
}
