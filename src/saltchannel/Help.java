package saltchannel;

import saltchannel.dev.RunTcpClient;
import saltchannel.dev.TcpTestServer;

/**
 * Prints help: a partial list of executable classes.
 * 
 * @author Frans Lundberg
 */
public class Help {
    private void go(String[] args) {
        StringBuilder b = new StringBuilder();
        b.append("Choose an executable class:\n");
        b.append("  " + saltchannel.dev.PrintBuildInfo.class.getName() + "\n");
        b.append("  " + TcpTestServer.class.getName() + "\n");
        b.append("  " + RunTcpClient.class.getName() + "\n");
        System.out.println(b.toString());
    }

    public static void main(String[] args) {
        new Help().go(args);
    }
}
