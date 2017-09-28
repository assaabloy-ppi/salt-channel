package saltchannel;

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
        b.append("  " + saltchannel.dev.TcpTestServer.class.getName() + "\n");
        b.append("  " + saltchannel.dev.RunTcpClient.class.getName() + "\n");
        b.append("  " + saltchannel.dev.WsTestServer.class.getName() + "\n");
        System.out.println(b.toString());
    }

    public static void main(String[] args) {
        new Help().go(args);
    }
}
