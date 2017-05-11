package saltchannel.dev;

/**
 * Prints build info to standard output.
 * 
 * @author Frans Lundberg
 */
public class PrintBuildInfo {
    public static void main(String[] args) {
        System.out.println("Info from " + BuildInfo.class.getName() + ":");
        System.out.println("  VERSION: " + BuildInfo.VERSION);
        System.out.println("  TIME: " + BuildInfo.TIME);
    }
}
