package saltchannel;

/**
 * Adds a listener to an existing ByteChannel.
 * Decorator pattern, byte arrays and time.
 * 
 * @author Frans Lundberg
 */
public class ByteChannelWithListener implements ByteChannel {
    private ByteChannel channel;
    private long t0;
    private Listener listener;
    
    public ByteChannelWithListener(ByteChannel channel, Listener listener) {
        this.channel = channel;
        this.listener = listener;
        this.t0 = System.nanoTime();
    }

    @Override
    public byte[] read() throws ComException {
        byte[] bytes = channel.read();
        long time = System.nanoTime() - t0;
        listener.onPostRead(bytes, time);
        return bytes;
    }

    @Override
    public void write(byte[]... messages) throws ComException {
        channel.write(messages);
        long time = System.nanoTime() - t0;
        listener.onPostWrite(messages, time);
    }
    
    /**
     * Returns the t0 reference time, nanoseconds from System.nanoTime().
     */
    public long getT0() {
        return t0;
    }
    
    public static interface Listener {
        public void onPostRead(byte[] byteArray, long time);
        public void onPostWrite(byte[][] byteArrays, long time);
    }
}
