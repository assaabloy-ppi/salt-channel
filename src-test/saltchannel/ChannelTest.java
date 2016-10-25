package saltchannel;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests full channel usage.
 * 
 * @author Frans Lundberg
 */
public class ChannelTest {

    @Test
    public void testRandom() {
        // Test with random key pairs.
        // App message to server that echos first message received back to client.
        
        final TweetLib lib = TweetLib.createFastAndInsecure();
        final KeyPair clientKeyPair = lib.createSigKeys();
        final KeyPair serverKeyPair = lib.createSigKeys();
        final Tunnel tunnel = new Tunnel();
        final ServerChannel serverChannel = new ServerChannel(lib, tunnel.ch2());
        final ClientChannel clientChannel = new ClientChannel(lib, tunnel.ch1());
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                serverChannel.handshake(serverKeyPair);
                byte[] app1s = serverChannel.read();
                serverChannel.write(app1s);
            }
        });
        thread.start();
        
        clientChannel.handshake(clientKeyPair);
        
        byte[] app1 = new byte[]{1, 2, 3};
        clientChannel.write(app1);
        byte[] response = clientChannel.read();
        
        Assert.assertArrayEquals(app1, response);
    }
}
