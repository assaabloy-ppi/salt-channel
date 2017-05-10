package saltchannel.dev;

import java.io.IOException;

/**
 * Starts and runs the echo server.
 * 
 * @author Frans Lundberg
 */
public class RunServer {

    public static void main(String[] args) throws IOException, InterruptedException {
        TestTcpServer s = TestTcpServer.createEchoServer();
        
        s.start();
        
        while (true) {
            Thread.sleep(100*1000);
        }
    }
}
