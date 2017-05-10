package saltchannel.util;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Io {
    public static byte[] streamToBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }
    
    /**
     * Copies data from in to out as long as there is still data to read from
     * in. The streams are not closed by this method.
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer;
        int n;

        buffer = new byte[4 * 1024];
        while (true) {
            n = in.read(buffer);
            if (n == -1) {
                break;
            }
            out.write(buffer, 0, n);
        }
    }
    
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
