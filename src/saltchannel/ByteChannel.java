package saltchannel;

/**
 * A two-way, reliable communication channel.
 * Byte arrays can be read and written; a simple blocking model.
 * 
 * Concurrency note: an implementation of this interface must handle that 
 * one thread calls read() while another one calls write(). However, 
 * multiple threads calling read() concurrently, or multiple threads calling
 * write() concurrently *cannot* be assumed to work. The consumer of this
 * interface should in such cases ensure that only thread calls read() at
 * time, and only one thread calls write() at a time.
 * 
 * @author Frans Lundberg
 */
public interface ByteChannel {
    /**
     * Reads one message; blocks until one is available.
     * 
     * @throws ComException
     *          If there was an IO error or data format error in 
     *          the underlying layer.
     */
    public byte[] read() throws ComException;
    
    /**
     * Writes messages. This method may block.
     * 
     * @throws ComException
     */
    public void write(byte[]... messages) throws ComException;
}
