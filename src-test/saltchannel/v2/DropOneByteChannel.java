package saltchannel.v2;

import saltchannel.ByteChannel;
import saltchannel.ComException;

public class DropOneByteChannel implements ByteChannel {
    private ByteChannel channel;
    private boolean dropNext;
    
    public DropOneByteChannel(ByteChannel channel) {
        this.channel = channel;
        this.dropNext = false;
    }
    
    /**
     * Tells the channel to discard next read message.
     */
    public void dropNext() {
        this.dropNext = true;
    }

    @Override
    public byte[] read() throws ComException {
        if (dropNext) {
            channel.read();
            dropNext = false;
        }
        return channel.read();
    }

    @Override
    public void write(byte[]... messages) throws ComException {
        channel.write(false, messages);
    }

    @Override
    public void write(boolean isLast, byte[]... messages) {
        channel.write(isLast, messages);
    }
}
