package saltchannel;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Public static utility methods.
 */
public class Util {
    public static void readFully(InputStream in, byte[] dest, int offset, final int length) 
            throws IOException {
        int len = length;
        
        while (true) {
            int count = in.read(dest, offset, len);
            if (count == -1) {
                throw new EOFException("EOF reached");
            }
            
            offset += count;
            len -= count;
            if (len == 0) {
                break;
            }
        }
    }
}
