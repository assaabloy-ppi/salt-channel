package saltchannel.v2;

import org.junit.Assert;
import org.junit.Test;
import a1a2.A1Client;
import a1a2.A2Packet;
import saltchannel.BadPeer;
import saltchannel.ByteChannel;
import saltchannel.ComException;
import saltchannel.CryptoLib;
import saltchannel.TimeException;
import saltchannel.Tunnel;
import saltchannel.testutil.ToWaitFor;
import saltchannel.util.CryptoTestData;
import saltchannel.util.KeyPair;
import saltchannel.util.Rand;
import saltchannel.util.TimeChecker;

/**
 * Testing full client-server channels. In-memory.
 */
public class ChannelTest {

    @Test
    public void testSample1() {
        // Alice (client) to Bob (server). Fixed key pairs.
        // Could be used as protocol example.
        
        Tunnel tunnel = new Tunnel();
        
        final SaltClientSession client = new SaltClientSession(CryptoTestData.aSig, tunnel.channel1());
        client.setEncKeyPair(CryptoTestData.aEnc);
        
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
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
    public void testSample1WithM4Buffered() {
        Tunnel tunnel = new Tunnel();
        
        final SaltClientSession client = new SaltClientSession(CryptoTestData.aSig, tunnel.channel1());
        client.setEncKeyPair(CryptoTestData.aEnc);
        
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
                byte[] appMessage = server.getChannel().read();
                server.getChannel().write(appMessage);
            }
        });
        thread.start();
        
        client.setBufferM4(true);
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
        
        final SaltClientSession client = new SaltClientSession(CryptoTestData.aSig, tunnel.channel1());
        client.setEncKeyPair(CryptoTestData.aEnc);
        client.setTimeChecker(testChecker);
        
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
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
        
        final SaltServerSession server = new SaltServerSession(serverKeyPair, tunnel.channel2());
        server.setEncKeyPair(rand);
        
        final SaltClientSession client = new SaltClientSession(clientKeyPair, tunnel.channel1());
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
    
    @Test
    public void testDefaultA2() {
        Tunnel tunnel = new Tunnel();
        
        final A1Client client = new A1Client(tunnel.channel1());
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
            }
        });
        thread.start();
        
        A2Packet a2 = client.go();
        
        Assert.assertEquals(1, a2.prots.length);
        Assert.assertEquals("SC2-------", a2.prots[0].p1);
        Assert.assertEquals("----------", a2.prots[0].p2);
    }
    
    @Test
    public void testCustomA2() {
        Tunnel tunnel = new Tunnel();
        A2Packet a2 = new A2Packet.Builder().prot("MyProtV3--").prot("NataliaV2-").build();
        
        final A1Client client = new A1Client(tunnel.channel1());
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        server.setA2(a2);
        server.setEncKeyPair(CryptoTestData.bEnc);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
            }
        });
        thread.start();
        
        A2Packet a2b = client.go();
        
        Assert.assertEquals(2, a2b.prots.length);
        Assert.assertEquals("SC2-------", a2.prots[0].p1);
        Assert.assertEquals("MyProtV3--", a2.prots[0].p2);
    }
    
    @Test
    public void testBadM1_1() throws InterruptedException {
        testBadM1Internal(0);
    }
    
    @Test
    public void testBadM1_2() throws InterruptedException {
        testBadM1Internal(1);
    }
    
    @Test
    public void testBadM1_3() throws InterruptedException {
        testBadM1Internal(1000);
    }
    
    private void testBadM1Internal(int size) throws InterruptedException {
        Tunnel tunnel = new Tunnel();
        final MyEvent myEvent = new MyEvent();
        byte[] badM1 = new byte[size];
        
        final SaltClientSession client = new SaltClientSession(CryptoTestData.aSig, tunnel.channel1());
        client.setEncKeyPair(CryptoTestData.aEnc);
        
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    server.handshake();
                } catch (BadPeer e) {
                    myEvent.ex = e;
                }
                
                myEvent.reportHappened();
            }
        });
        thread.start();
        
        tunnel.channel1().write(badM1);
        
        myEvent.waitForIt(1000);
        
        Assert.assertTrue(myEvent.ex != null);
        Assert.assertEquals(myEvent.ex.getClass(), BadPeer.class);
    }
    
    @Test
    public void testBadM2_1() throws InterruptedException {
        testBadM2Internal(0);
    }
    
    @Test
    public void testBadM2_2() throws InterruptedException {
        testBadM2Internal(1);
    }
    
    @Test
    public void testBadM2_3() throws InterruptedException {
        testBadM2Internal(1000);
    }
    
    private void testBadM2Internal(int size) throws InterruptedException {
        final Tunnel tunnel = new Tunnel();
        final byte[] badM2 = new byte[size];
        
        final SaltClientSession client = new SaltClientSession(CryptoTestData.aSig, tunnel.channel1());
        client.setEncKeyPair(CryptoTestData.aEnc);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                ByteChannel ch = tunnel.channel2();
                
                ch.read();
                ch.write(badM2);
            }
        });
        thread.start();
        
        Exception ex = null;
        
        try {
            client.handshake();
        } catch (Exception e) {
            ex = e;
        }
        
        Assert.assertTrue("ex not null", ex != null);
        Assert.assertEquals(ex.getClass(), BadPeer.class);
    }
    
    private static class MyEvent extends ToWaitFor {
        public Exception ex;
    }
}
