package saltchannelx.ws;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import saltchannel.ByteChannel;
import saltchannel.ComException;
import saltchannel.dev.ByteChannelServerSession;
import saltchannel.dev.EchoServerSession;
import saltchannel.dev.ServerSessionFactory;
import saltchannel.util.CryptoTestData;
import saltchannel.util.KeyPair;
import saltchannel.util.Pair;
import saltchannel.v2.SaltServerSession;
import saltchannel.v2.packets.PacketHeader;

/**
 * WebSocket echo server over Salt Channel.
 * Just for testing; an example implementation, not for production use.
 * 
 * @author Frans Lundberg
 */
public class WsTestServer {
    public static final int DEFAULT_PORT = 2034;
    private final int port;
    private HashMap<WebSocket, WebSocketInfo> sockets = new HashMap<>();
    private ServerSessionFactory sessionFactory;
    private KeyPair sigKeyPair;
    
    public WsTestServer(int port, ServerSessionFactory sessionFactory) {
        this.port = port;
        this.sessionFactory = sessionFactory;
        this.sigKeyPair = CryptoTestData.bSig;   // server is "Bob"
    }
    
    public int getPort() {
        return port;
    }
    
    public void start() {
        InetSocketAddress address = new InetSocketAddress(port);
        WebSocketServer server = new WebSocketServer(address) {
            @Override
            public void onClose(WebSocket socket, int code, String reason, boolean remote) {
                synchronized (this) {
                    sockets.remove(socket);
                }
            }

            @Override
            public void onError(WebSocket socket, Exception ex) {
                System.out.println("SERVER, onError, " + ex);
            }
            
            @Override
            public void onMessage(WebSocket socket, java.nio.ByteBuffer message) {
                byte[] bytes = message.array();
                
                if (bytes.length < PacketHeader.SIZE) {
                    // Could be logged if this were production code.
                    socket.close();
                }
                
                PacketHeader header = new PacketHeader(bytes, 0);
                
                synchronized (this) {
                    WebSocketInfo info = sockets.get(socket);
                    if (info != null) {
                        info.messageQ.add(new Pair<byte[], Boolean>(bytes, header.lastFlag()));
                    }
                }
            }

            @Override
            public void onMessage(WebSocket socket, String message) {
                // Should never happen, ignore.
            }

            @Override
            public void onOpen(final WebSocket socket, ClientHandshake handshake) {                
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        handleSocket(socket);
                    }
                });
                
                thread.start();
            }
        };
        
        server.start();
    }
    
    private void handleSocket(final WebSocket socket) {
        final WebSocketInfo socketInfo = new WebSocketInfo();
        
        try {
            synchronized (this) {
                sockets.put(socket, socketInfo);
            }
            
            ByteChannel clearChannel = new ByteChannel() {
                public byte[] read() throws ComException {
                    Pair<byte[], Boolean> pair;
                    try {
                        pair = socketInfo.messageQ.take();
                    } catch (InterruptedException e) {
                        throw new ComException(e.getMessage());
                    }
                    
                    byte[] bytes = pair.getValue0();
                    boolean isLast = pair.getValue1();
                    
                    if (isLast) {
                        socket.close();
                    }
                    
                    return bytes;
                }

                public void write(byte[]... messages) throws ComException {
                    write(false, messages);
                }
                
                public void write(boolean isLast, byte[]... messages) throws ComException {
                    for (int i = 0; i < messages.length; i++) {
                        socket.send(messages[i]);
                    }
                    
                    if (isLast) { // close socket if last message has been sent
                        socket.close();
                    }
                }
            };
            
            SaltServerSession session = new SaltServerSession(sigKeyPair, clearChannel);
            session.setEncKeyPair(CryptoTestData.aEnc);
            session.setBufferM2(true);
            session.handshake();
            
            ByteChannelServerSession s = this.sessionFactory.createSession();
            s.runSession(session.getChannel());
            
        } finally {
            synchronized (this) {
                sockets.remove(socket);
            }
        }        
    }

    private static class WebSocketInfo {
        BlockingQueue<Pair<byte[], Boolean>> messageQ;
        
        WebSocketInfo() {
            this.messageQ = new LinkedBlockingQueue<Pair<byte[], Boolean>>();
        }
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSessionFactory sessionFactory = new ServerSessionFactory() {
            public ByteChannelServerSession createSession() {
                return new EchoServerSession();
            }
        };
        
        WsTestServer s = new WsTestServer(WsTestServer.DEFAULT_PORT, sessionFactory);
        
        s.start();
        
        while (true) {
            Thread.sleep(100*1000);
        }
    }
}

