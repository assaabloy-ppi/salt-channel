package saltchannel.dev;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import saltchannel.ComException;
import saltchannel.SocketChannel;
import saltchannel.util.CryptoTestData;
import saltchannel.util.KeyPair;
import saltchannel.util.Util;
import saltchannel.v2.SaltServerSession;

/**
 * A test TCP server running Salt Channel and a user-specified protocol
 * on top of that.
 * Note, this is for development purposes only. A fixed ephemeral key pair
 * to allow for deterministic sessions. This is not a secure practice
 * for production, of course.
 * 
 * @author Frans Lundberg
 */
public class TcpTestServer {    
    public static final int DEFAULT_PORT = 2033;
    private final int port;
    private final Thread thread;
    private volatile boolean shutdown;
    private ServerSocket ss;
    private ServerSessionFactory sessionFactory;
    private KeyPair keyPair;
    
    public TcpTestServer(int port, ServerSessionFactory sessionFactory) {
        // Inits things.
        // Prepares a thread.
        
        this.port = port;
        this.sessionFactory = sessionFactory;
        
        this.keyPair = CryptoTestData.bSig;   // server is "Bob"
        
        this.thread = new Thread(new Runnable() {
            public void run() {
                TcpTestServer.this.run();
            }
        });
        thread.setName("Server-thread");
    }
    
    /**
     * Helper function that creates an echo server based on the implementation
     * in EchoServerSession.
     */
    public static TcpTestServer createEchoServer() {
        ServerSessionFactory sessionFactory = new ServerSessionFactory() {
            public ByteChannelServerSession createSession() {
                return new EchoServerSession();
            }
        };
        
        return new TcpTestServer(DEFAULT_PORT, sessionFactory);
    }
    
    /**
     * Starts the TCP server.
     * 
     * @throws java.net.BindException if port is used already.
     */
    public void start() throws IOException {
        ss = new ServerSocket(port);
        thread.start();
        System.out.println("SERVER: started on port " + port + ".");
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
    
    private void run() {       
        try {
            while (shutdown != true) {
                final Socket socket;
                
                try {
                    socket = ss.accept();
                    socket.setTcpNoDelay(true);
                } catch (IOException e) {
                    // This is expected when other code calls ss.close().
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
            System.out.println("SERVER: ComException while communicating with client, " + socket.getInetAddress() + ", " + e.getMessage());
        } finally {
            Util.close(socket);
        }
    }
    
    private void reallyHandleSocket(Socket socket) throws ComException {
        System.out.println("SERVER: client connected: " + socket.getRemoteSocketAddress());
        
        SocketChannel clearChannel;
        try {
            clearChannel = new SocketChannel(socket);
        } catch (IOException e) {
            throw new ComException(e.getMessage());
        }
        
        SaltServerSession session = new SaltServerSession(keyPair, clearChannel);
        session.setEncKeyPair(CryptoTestData.aEnc);
        session.setBufferM2(true);
        session.handshake();
        
        ByteChannelServerSession s = this.sessionFactory.createSession();
        s.runSession(session.getChannel());
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        TcpTestServer s = TcpTestServer.createEchoServer();
        
        s.start();
        
        while (true) {
            Thread.sleep(100*1000);
        }
    }
}
