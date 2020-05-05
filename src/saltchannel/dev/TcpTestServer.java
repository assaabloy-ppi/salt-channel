package saltchannel.dev;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import saltchannel.ByteChannel;
import saltchannel.ComException;
import saltchannel.SocketChannel;
import saltchannel.util.CryptoTestData;
import saltchannel.util.KeyPair;
import saltchannel.util.Util;
import saltchannel.v2.SaltServerSession;

/**
 * An INSECURE test TCP server running Salt Channel and a 
 * user-specified protocol on top of that.
 * Note, this is for development purposes only. A fixed ephemeral key pair
 * is used to allow for deterministic sessions. This is, of course, not a secure 
 * practice for production.
 * 
 * @author Frans Lundberg
 */
public class TcpTestServer {    
    public static final int DEFAULT_PORT = 2033;
    private final int port;
    private final Thread thread;
    private volatile boolean shutdown;
    private volatile ServerSocket ss;
    private ServerSessionFactory sessionFactory;
    private KeyPair keyPair;
    private Listener listener = Listener.NULL;
    private boolean useEncryption = true;
    
    public TcpTestServer(int port, ServerSessionFactory sessionFactory) {
        // Inits things.
        // Prepares a thread.
        
        this.port = port;
        this.sessionFactory = sessionFactory;
        this.keyPair = CryptoTestData.bSig;   // server is "Bob"
        
        this.thread = new Thread(new Runnable() {
            public void run() {
                TcpTestServer.this.runIt();
            }
        });
        thread.setName("Server-thread");
    }
    
    public void setListener(Listener listener) {
        this.listener = listener;
    }
    
    /**
     * If true (the default), the server will use Salt Channel connections.
     * Otherwise, cleartext is used.
     */
    public void setUseEncryption(boolean useEncryption) {
        this.useEncryption = useEncryption;
    }
    
    /**
     * Helper function that creates an echo server based on the implementation
     * in EchoServerSession. The DEFAULT_PORT is used.
     */
    public static TcpTestServer createEchoServer() {
        return createEchoServer(DEFAULT_PORT);
    }
    
    /**
     * Helper function that creates an echo server based on the implementation
     * in EchoServerSession.
     */
    public static TcpTestServer createEchoServer(int port) {
        ServerSessionFactory sessionFactory = new ServerSessionFactory() {
            public ByteChannelServerSession createSession() {
                return new EchoServerSession();
            }
        };
        
        return new TcpTestServer(port, sessionFactory);
    }
    
    /**
     * Starts the TCP server.
     * 
     * @throws java.net.BindException if port is used already.
     */
    public void start() throws IOException {
        ss = new ServerSocket(port);
        thread.start();
        listener.println("SERVER: started on port " + port + ".");
    }
    
    /**
     * Stops the server if started. 
     * After this method has been called, this object should
     * not be used again.
     */
    public void stop() {
        shutdown = true;
        
        ServerSocket mySs = this.ss;
        if (mySs != null) {
            Util.close(mySs);
        }
    }
    
    public int getPort() {
        return port;
    }
    
    private void runIt() {       
        try {
            while (shutdown != true) {
                final Socket socket;
                
                try {
                    socket = ss.accept();
                } catch (IOException e) {
                    // This is expected when other thread calls ss.close().
                    break;
                }
                
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        handleSocket(socket);
                    }
                });
                
                thread.setName("tcpClient-" + thread.getId() + "-" + socket.getRemoteSocketAddress());
                thread.start();
            }
        } finally {
            Util.close(ss);
        }
    }

    private void handleSocket(Socket socket) {
        try {
            reallyHandleSocket(socket);
        } catch (ComException e) {
            listener.println("SERVER: ComException while communicating with client, " 
                    + socket.getInetAddress() + ", " + e.getMessage());
        } finally {
            Util.close(socket);
        }
    }
    
    private void reallyHandleSocket(Socket socket) throws ComException {
        System.out.println("SERVER: client connected: " + socket.getRemoteSocketAddress());
        
        SocketChannel clearChannel;
        ByteChannel channel;
        
        try {
            socket.setTcpNoDelay(true);
            clearChannel = new SocketChannel(socket);
        } catch (IOException e) {
            throw new ComException(e.getMessage());
        }
        
        if (useEncryption) {
            SaltServerSession session = new SaltServerSession(keyPair, clearChannel);
            session.setEncKeyPair(CryptoTestData.aEnc);
            session.setBufferM2(true);
            session.handshake();
            if (session.isDone()) {
                return;
            }
            
            channel = session.getChannel();
        } else {
            channel = clearChannel;
        }
        
        ByteChannelServerSession s = this.sessionFactory.createSession();
        s.runSession(channel);
    }
    
    public static interface Listener {
        public static final Listener NULL = new Listener() {
            public void println(String string) {}
        };
        
        /**
         * Prints a debug message to the listener.
         */
        public void println(String string);
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        TcpTestServer s = TcpTestServer.createEchoServer();
        
        s.start();
        
        while (true) {
            Thread.sleep(100*1000);
        }
    }
}
