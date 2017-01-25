package saltchannel;

/**
 * A two-way, reliable communication channel.
 * Byte arrays can be read and written; a simple blocking model.
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
