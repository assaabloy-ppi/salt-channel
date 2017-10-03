package saltchannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import saltchannel.util.Bytes;
import saltchannel.util.Io;

/**
 * A ByteChannel implementation based on a pair of streams.
 * 
 * @author Frans Lundberg
 */
public class StreamChannel implements ByteChannel {
    private final InputStream in;
    private final OutputStream out;
    private final Listener listener;

    public StreamChannel(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        this.listener = Listener.NULL_LISTENER;
    }
    
    @Override
    public byte[] read() throws ComException {
        try {
            return innerRead();
        } catch (IOException e) {
            throw new ComException(e.getMessage());
        }
    }

    @Override
    public void write(byte[]... messages) throws ComException {
        write(false, messages);
    }
    
    @Override
    public void write(boolean isLast, byte[]... messages) throws ComException {
        try {
            innerWrite(isLast, messages);
        } catch (IOException e) {
            throw new ComException(e.getMessage());
        }
    }
    
    private byte[] innerRead() throws IOException {
        byte[] bytes = new byte[4];
        Io.readFully(in, bytes, 0, 4);
        
        int length = Bytes.bytesToIntLE(bytes, 0);
        
        if (length <= 0) {
            throw new BadPeer("non-positive packet size, " + length);
        }
        
        if (length > 10*1000000) {
            throw new BadPeer("package too huge, not supported, " + length);
        }
        
        byte[] data = new byte[length];
        Io.readFully(in, data, 0, data.length);
        
        listener.messageRead(data);
        
        return data;
    }
    

    private void innerWrite(boolean isLast, byte[]... messages) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] bytes;
        byte[] sizeBytes = new byte[4];
        
        for (int i = 0; i < messages.length; i++) {
            bytes = messages[i];
            Bytes.intToBytesLE(bytes.length, sizeBytes, 0);
            bout.write(sizeBytes);
            bout.write(bytes);
        }
        bout.flush();
        bout.close();
        
        byte[] allBytes = bout.toByteArray();
        out.write(allBytes);
        out.flush();
        
        listener.messagesWritten(messages);
    }
    
    public static interface Listener {
        public static final Listener NULL_LISTENER = new Listener() {
            public void messagesWritten(byte[][] messages) {}
            public void messageRead(byte[] message) {}
        };
        
        public void messagesWritten(byte[][] messages);
        
        public void messageRead(byte[] message);
    }
}
