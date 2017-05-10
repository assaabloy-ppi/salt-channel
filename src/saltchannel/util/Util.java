package saltchannel.util;

import java.io.Closeable;
import java.io.IOException;

public class Util {
    /**
     * Closes and ignores any IOException in from the close() method.
     */
    public static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            // empty
        }
    }
}
