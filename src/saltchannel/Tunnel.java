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
    private ByteChannel channel1;
    private ByteChannel channel2;
    private LinkedBlockingQueue<byte[]> q1;
    private LinkedBlockingQueue<byte[]> q2;
    
    public Tunnel() {
        q1 = new LinkedBlockingQueue<byte[]>();
        q2 = new LinkedBlockingQueue<byte[]>();
        
        channel1 = new ByteChannel() {
            public byte[] read() throws ComException {
                try {
                    byte[] result = q1.take();
                    return result;
                } catch (InterruptedException e) {
                    throw new ComException("interrupted");
                }
            }
            
            @Override
            public void write(byte[]... messages) throws ComException {
                write(false, messages);
            }

            @Override
            public void write(boolean isLast, byte[]... messages) throws ComException {
                for (int i = 0; i < messages.length; i++) {
                    q2.add(messages[i]);
                }
            }
        };
        
        channel2 = new ByteChannel() {
            public byte[] read() throws ComException {
                try {
                    return q2.take();
                } catch (InterruptedException e) {
                    throw new ComException("interrupted");
                }
            }

            @Override
            public void write(byte[]... messages) throws ComException {
                write(false, messages);
            }
            
            @Override
            public void write(boolean isLast, byte[]... messages) throws ComException {
                for (int i = 0; i < messages.length; i++) {
                    q1.add(messages[i]);
                }
            }
        };
    }
    
    public ByteChannel channel1() {
        return channel1;
    }
    
    public ByteChannel channel2() {
        return channel2;
    }
}
