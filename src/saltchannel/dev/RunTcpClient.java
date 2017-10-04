package saltchannel.dev;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import saltchannel.ByteChannel;
import saltchannel.SocketChannel;
import saltchannel.util.CryptoTestData;
import saltchannel.util.Hex;
import saltchannel.util.KeyPair;
import saltchannel.v2.SaltClientSession;

/**
 * Runs an echo client; connects to echo server at localhost and DEFAULT_PORT.
 * Application layer: from-client: 6, from-server: 6
 * With sizes we have 10-10 bytes transferred.
 * 
 * @author Frans Lundberg
 */
public class RunTcpClient {
    private KeyPair sigKeyPair = CryptoTestData.aSig;
        // Client is "Alice".
    
    private void go() throws UnknownHostException, IOException {
        Socket socket = new Socket("localhost", TcpTestServer.DEFAULT_PORT);
        ByteChannel clear = new SocketChannel(socket);
        SaltClientSession session = new SaltClientSession(sigKeyPair, clear);
        session.setEncKeyPair(CryptoTestData.bEnc);
        session.setBufferM4(true);
        session.handshake();
        ByteChannel appChannel = session.getChannel();
        
        byte[] request = new byte[]{1, 5, 5, 5, 5, 5};
        appChannel.write(false, request);
        byte[] response = appChannel.read();
        
        System.out.println("Request: " + Hex.create(request));
        System.out.println("Response: " + Hex.create(response));
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new RunTcpClient().go();
    }
}

/*

WIRESHARK ANALYSIS 2017-05-10

Total: 402 bytes and 3 turns.

    C       S
    46 ----->
    <----- 42
    <---- 124
    158 ---->
    <----- 34
        
46+42+124+158+34 = 404 bytes.
The Salt Channel handshake is: 46+42+124+124 = 336 bytes.

Note, we have *two* TCP packets 42+124. This could be avoided.
When IO is slow compared to crypto, 42+124 should be combined into one write.
When crypto is slow compared to IO, it makes sense to split them.

Update,2017-05-18, now M2+M3 write is supported and we have
the following when M2 is buffered on server-side.

    C       S
    46 ----->
    <---- 166
    158 ---->
    <----- 34
        
Same totals as before, of course.

 */
