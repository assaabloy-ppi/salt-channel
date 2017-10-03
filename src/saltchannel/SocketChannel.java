package saltchannel;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * A ByteChannel implementation based on a Socket.
 * 
 * @author Frans Lundberg
 */
public class SocketChannel implements ByteChannel {
    private final Socket socket;
    private final StreamChannel channel;

    /**
     * Creates a StreamChannel from a socket. The input stream of the socket
     * is wrapped with a BufferedInputStream. The output stream is not wrapped.
     * The socket is never closed by this class.
     * 
     * @param socket
     * @throws IOException
     */
    public SocketChannel(Socket socket) throws IOException {
        this.socket = socket;
        BufferedInputStream b = new BufferedInputStream(socket.getInputStream());
        channel = new StreamChannel(b, socket.getOutputStream());
    }
    
    public Socket getSocket() {
        return socket;
    }
    
    public byte[] read() throws ComException {
        return channel.read();
    }

    public void write(byte[]... messages) throws ComException {
        channel.write(false, messages);
    }
    
    public void write(boolean isLast, byte[]... messages) throws ComException {
        channel.write(isLast, messages);
    }
}
