package saltchannel.dev;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import saltchannel.ByteChannel;
import saltchannel.SocketChannel;
import saltchannel.util.CryptoTestData;
import saltchannel.util.Hex;
import saltchannel.util.KeyPair;
import saltchannel.v2.ClientSession;

/**
 * Runs an echo client; connects to echo server at localhost and DEFAULT_PORT.
 * 
 * @author Frans Lundberg
 */
public class RunClient {
    private KeyPair keyPair = CryptoTestData.bSig; 
        // Client is "Bob".
    
    private void go() throws UnknownHostException, IOException {
        Socket socket = new Socket("localhost", TestTcpServer.DEFAULT_PORT);
        ByteChannel clear = new SocketChannel(socket);
        ClientSession session = new ClientSession(keyPair, clear);
        session.setEncKeyPair(CryptoTestData.bEnc);
        session.handshake();
        ByteChannel appChannel = session.getChannel();
        
        byte[] request = new byte[]{1, 4, 4, 4, 4};
        appChannel.write(request);
        byte[] response = appChannel.read();
        
        System.out.println("Request: " + Hex.create(request));
        System.out.println("Response: " + Hex.create(response));
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new RunClient().go();
    }
}
