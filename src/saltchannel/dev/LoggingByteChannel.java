package saltchannel.dev;

import java.util.ArrayList;
import java.util.List;
import saltchannel.ByteChannel;
import saltchannel.ComException;
import saltchannel.util.Hex;

/**
 * Decorator for ByteChannel, stores read and written packages.
 */
public class LoggingByteChannel implements ByteChannel {
    private ByteChannel inner;
    private ArrayList<LoggingByteChannel.Entry> log;

    public LoggingByteChannel(ByteChannel channel) {
        this.log = new ArrayList<>();
        this.inner = channel;
    }
    
    public List<LoggingByteChannel.Entry> getLog() {
        return log;
    }
    
    public static enum ReadOrWrite {
        READ, WRITE, WRITE_WITH_PREVIOUS
    }
    
    public static class Entry {
        /** Time in nanos (from System.nanoTime()). */
        public long time;
        
        public LoggingByteChannel.ReadOrWrite type;
        
        public boolean isLast;
        
        public byte[] bytes;
        
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append(type.name() + ", " + time + "\n");
            b.append("    " + bytes.length + ", " + Hex.create(bytes));
            return b.toString();
        }
    }

    @Override
    public byte[] read() throws ComException {
        byte[] result = inner.read();
        
        LoggingByteChannel.Entry entry = new Entry();
        entry.time = System.nanoTime();
        entry.type = ReadOrWrite.READ;
        entry.bytes = result;
        log.add(entry);
        
        return result;
    }
    
    public void write(byte[]... messages) throws ComException {
        write(false, messages);
    }

    @Override
    public void write(boolean isLast, byte[]... messages) throws ComException {
        inner.write(isLast, messages);
        
        long time = System.nanoTime();
        
        for (int i = 0; i < messages.length; i++) {
            LoggingByteChannel.Entry entry = new Entry();
            entry.time = time;
            entry.type = i == 0 ? ReadOrWrite.WRITE : ReadOrWrite.WRITE_WITH_PREVIOUS;
            entry.bytes = messages[i];
            log.add(entry);
        }
    }
}