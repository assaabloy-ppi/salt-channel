package saltchannel.v2;

import org.junit.Assert;
import org.junit.Test;
import saltchannel.ByteChannel;
import saltchannel.ComException;
import saltchannel.CryptoLib;
import saltchannel.CryptoTestData;
import saltchannel.TimeException;
import saltchannel.Tunnel;
import saltchannel.util.KeyPair;
import saltchannel.util.Rand;
import saltchannel.util.TimeChecker;

/**
 * Testing full client-server channels.
 */
public class ChannelTest {

    @Test
    public void testSample1() {
        // Alice (client) to Bob (server). Fixed key pairs.
        // Could be used as protocol example.
        
        Tunnel tunnel = new Tunnel();
        
        final Client client = new Client(CryptoTestData.aSig, tunnel.channel1());
        client.setEncKeyPair(CryptoTestData.aEnc);
        
        final Server server = new Server(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
                byte[] appMessage = server.getChannel().read();
                server.getChannel().write(appMessage);
            }
        });
        thread.start();
        
        client.handshake();
        
        byte[] app1 = new byte[3000];
        app1[2999] = 99;
        
        ByteChannel channel = client.getChannel();
        channel.write(app1);
        byte[] response = channel.read();
        
        Assert.assertArrayEquals(app1, response);
        Assert.assertArrayEquals(CryptoTestData.aSig.pub(), server.getClientSigKey());
        Assert.assertArrayEquals(CryptoTestData.bSig.pub(), client.getServerSigKey());
    }
    

    @Test
    public void testSample1WithTimeChecker() {
        Tunnel tunnel = new Tunnel();
        
        TimeChecker testChecker = new TimeChecker() {
            int counter = 0;
            
            public void reportFirstTime(int time) {}

            public void checkTime(int time) {
                if (counter == 1) {
                    throw new TimeException("t1");
                }
                
                counter++;
            }
        };
        
        final Client client = new Client(CryptoTestData.aSig, tunnel.channel1());
        client.setEncKeyPair(CryptoTestData.aEnc);
        client.setTimeChecker(testChecker);
        
        final Server server = new Server(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
                byte[] appMessage = server.getChannel().read();
                server.getChannel().write(appMessage);
            }
        });
        thread.start();
        
        client.handshake();
        
        byte[] app1 = new byte[3000];
        app1[2999] = 99;
        
        ByteChannel channel = client.getChannel();
        channel.write(app1);
        
        Exception ex = null;
        try {
            channel.read();
        } catch (ComException e) {
            ex = e;
        }
        
        Assert.assertTrue(ex != null);
        Assert.assertEquals(TimeException.class, ex.getClass());
        Assert.assertEquals("t1", ex.getMessage());
    }
    
    @Test
    public void testRandom() {
        // Test with random key pairs.
        // App message to server that echos first message received back to client.
        
        final Rand rand = CryptoLib.createInsecureAndFastRand();
        final KeyPair clientKeyPair = CryptoLib.createSigKeys(rand);
        final KeyPair serverKeyPair = CryptoLib.createSigKeys(rand);
        final Tunnel tunnel = new Tunnel();
        
        final Server server = new Server(serverKeyPair, tunnel.channel2());
        server.setEncKeyPair(rand);
        
        final Client client = new Client(clientKeyPair, tunnel.channel1());
        client.setEncKeyPair(rand);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
                byte[] app1s = server.getChannel().read();
                server.getChannel().write(app1s);
            }
        });
        thread.start();
        
        client.handshake();
        
        byte[] app1 = new byte[]{1, 2, 3};
        ByteChannel channel = client.getChannel();
        channel.write(app1);
        byte[] response = channel.read();
        
        Assert.assertArrayEquals(app1, response);
    }
}
