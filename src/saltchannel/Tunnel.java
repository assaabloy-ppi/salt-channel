package saltchannel;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * A channel tunnel, a pair of channels that are tunneled.
 * The input to Channel 1, is the output of Channel 2 and vice versa.
 * An instance of this class is very useful for testing.
 * 
 * @author Frans Lundberg
 */
public class Tunnel {
    private ByteChannel ch1;
    private ByteChannel ch2;
    private LinkedBlockingQueue<byte[]> q1;
    private LinkedBlockingQueue<byte[]> q2;
    
    public Tunnel() {
        q1 = new LinkedBlockingQueue<byte[]>();
        q2 = new LinkedBlockingQueue<byte[]>();
        
        ch1 = new ByteChannel() {
            public byte[] read() throws ComException {
                try {
                    byte[] result = q1.take();
                    return result;
                } catch (InterruptedException e) {
                    throw new ComException("interrupted");
                }
            }

            @Override
            public void write(byte[]... obj) throws ComException {
                for (int i = 0; i < obj.length; i++) {
                    q2.add(obj[i]);
                }
            }
        };
        
        ch2 = new ByteChannel() {
            public byte[] read() throws ComException {
                try {
                    return q2.take();
                } catch (InterruptedException e) {
                    throw new ComException("interrupted");
                }
            }

            @Override
            public void write(byte[]... messages) throws ComException {
                for (int i = 0; i < messages.length; i++) {
                    q1.add(messages[i]);
                }
            }
        };

    }
    
    public ByteChannel ch1() {
        return ch1;
    }
    
    public ByteChannel ch2() {
        return ch2;
    }
}
