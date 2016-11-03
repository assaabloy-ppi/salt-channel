package saltchannel;

import org.junit.Assert;
import org.junit.Test;

import saltchannel.util.KeyPair;
import saltchannel.util.Rand;

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
        
        final Rand rand = CryptoLib.createInsecureAndFastRand();
        final KeyPair clientKeyPair = CryptoLib.createSigKeys(rand);
        final KeyPair serverKeyPair = CryptoLib.createSigKeys(rand);
        final Tunnel tunnel = new Tunnel();
        final ServerChannel serverChannel = new ServerChannel(tunnel.channel2());
        final ClientChannel clientChannel = new ClientChannel(tunnel.channel1());
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                serverChannel.handshake(serverKeyPair, rand);
                byte[] app1s = serverChannel.read();
                serverChannel.write(app1s);
            }
        });
        thread.start();
        
        clientChannel.handshake(clientKeyPair, rand);
        
        byte[] app1 = new byte[]{1, 2, 3};
        clientChannel.write(app1);
        byte[] response = clientChannel.read();
        
        Assert.assertArrayEquals(app1, response);
    }
    
    @Test
    public void testThatServerPubKeyIsAvailable() {
        final Rand rand = CryptoLib.createInsecureAndFastRand();
        final KeyPair clientKeyPair = CryptoLib.createSigKeys(rand);
        final KeyPair serverKeyPair = CryptoLib.createSigKeys(rand);
        final Tunnel tunnel = new Tunnel();
        final ServerChannel serverChannel = new ServerChannel(tunnel.channel2());
        final ClientChannel clientChannel = new ClientChannel(tunnel.channel1());
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                serverChannel.handshake(serverKeyPair, rand);
                byte[] app1s = serverChannel.read();
                serverChannel.write(app1s);
            }
        });
        thread.start();
        
        clientChannel.handshake(clientKeyPair, rand);
        
        byte[] serversPubKey = clientChannel.getServerKey();
        Assert.assertTrue("asserts-that-pubkey-is-not-null", serversPubKey != null);
        Assert.assertEquals(32, serversPubKey.length);
    }
}
