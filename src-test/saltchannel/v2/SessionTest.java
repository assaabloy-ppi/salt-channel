package saltchannel.v2;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import saltchannel.BadPeer;
import saltchannel.ByteChannel;
import saltchannel.ComException;
import saltchannel.CryptoLib;
import saltchannel.TimeException;
import saltchannel.Tunnel;
import saltchannel.dev.LoggingByteChannel;
import saltchannel.testutil.ToWaitFor;
import saltchannel.util.CryptoTestData;
import saltchannel.util.KeyPair;
import saltchannel.util.Rand;
import saltchannel.util.TimeChecker;
import saltchannel.v2.packets.PacketHeader;

/**
 * Testing full client-server sessions; in-memory.
 * 
 * @author Frans Lundberg
 */
public class SessionTest {

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
                server.getChannel().write(true, appMessage);
            }
        });
        thread.start();
        
        client.handshake();
        
        byte[] app1 = new byte[3000];
        app1[2999] = 99;
        
        ByteChannel channel = client.getChannel();
        channel.write(false, app1);
        byte[] response = channel.read();
        
        Assert.assertArrayEquals(app1, response);
        Assert.assertArrayEquals(CryptoTestData.aSig.pub(), server.getClientSigKey());
        Assert.assertArrayEquals(CryptoTestData.bSig.pub(), client.getServerSigKey());
    }
    

    @Test
    public void testThatLastFlagIsSet() {
        // Checks that lastFlag is set and available to LoggingByteChannel (the writer).
        
        Tunnel tunnel = new Tunnel();
        LoggingByteChannel loggingChannel2 = new LoggingByteChannel(tunnel.channel2());
        
        final SaltClientSession client = new SaltClientSession(CryptoTestData.aSig, tunnel.channel1());
        client.setEncKeyPair(CryptoTestData.aEnc);
        
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, loggingChannel2);
        server.setEncKeyPair(CryptoTestData.bEnc);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
                byte[] appMessage = server.getChannel().read();
                server.getChannel().write(true, appMessage);
            }
        });
        thread.start();
        
        client.handshake();
        
        byte[] app1 = new byte[3000];
        app1[2999] = 99;
        
        ByteChannel channel = client.getChannel();
        channel.write(false, app1);
        channel.read();
        
        List<LoggingByteChannel.Entry> log = loggingChannel2.getLog();
        
        Assert.assertTrue(log.get(log.size() - 1).isLast);
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
                server.getChannel().write(true, appMessage);
            }
        });
        thread.start();
        
        client.setBufferM4(true);
        client.handshake();
        
        byte[] app1 = new byte[3000];
        app1[2999] = 99;
        
        ByteChannel channel = client.getChannel();
        channel.write(false, app1);
        byte[] response = channel.read();
        
        Assert.assertArrayEquals(app1, response);
        Assert.assertArrayEquals(CryptoTestData.aSig.pub(), server.getClientSigKey());
        Assert.assertArrayEquals(CryptoTestData.bSig.pub(), client.getServerSigKey());
    }
    
    @Test
    public void testSample1WithM2Buffered() {
        Tunnel tunnel = new Tunnel();
        
        final SaltClientSession client = new SaltClientSession(CryptoTestData.aSig, tunnel.channel1());
        client.setEncKeyPair(CryptoTestData.aEnc);
        
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.setBufferM2(true);
                server.handshake();
                byte[] appMessage = server.getChannel().read();
                server.getChannel().write(true, appMessage);
            }
        });
        thread.start();
        
        client.handshake();
        
        byte[] app1 = new byte[3000];
        app1[2999] = 99;
        
        ByteChannel channel = client.getChannel();
        channel.write(false, app1);
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
                server.getChannel().write(true, appMessage);
            }
        });
        thread.start();
        
        client.handshake();
        
        byte[] app1 = new byte[3000];
        app1[2999] = 99;
        
        ByteChannel channel = client.getChannel();
        channel.write(false, app1);
        
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
                server.getChannel().write(true, app1s);
            }
        });
        thread.start();
        
        client.handshake();
        
        byte[] app1 = new byte[]{1, 2, 3};
        ByteChannel channel = client.getChannel();
        channel.write(false, app1);
        byte[] response = channel.read();
        
        Assert.assertArrayEquals(app1, response);
    }
    
    @Test
    public void testDropOne() {
        // Client drops one message, leads to "invalid encryption".
        
        Tunnel tunnel = new Tunnel();
        DropOneByteChannel ch1 = new DropOneByteChannel(tunnel.channel1());
        
        final SaltClientSession client = new SaltClientSession(CryptoTestData.aSig, ch1);
        client.setEncKeyPair(CryptoTestData.aEnc);
        
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
                byte[] appMessage = server.getChannel().read();
                server.getChannel().write(false, appMessage);
                server.getChannel().write(true, appMessage);
            }
        });
        thread.start();
        
        client.handshake();
        
        byte[] app1 = new byte[3000];
        app1[2999] = 99;
        
        ByteChannel channel = client.getChannel();
        channel.write(false, app1);
        ch1.dropNext();
        
        Exception ex = null;
        try {
            channel.read();
        } catch (BadPeer e) {
            ex = e;
        }
        
        Assert.assertTrue(ex != null);
        Assert.assertTrue(ex instanceof BadPeer);
        Assert.assertTrue(ex.getMessage().contains("invalid encryption"));
    }
    
    @Test
    public void testNoSuchServer() {
        // Session with noSuchServer set in M2.
        
        Tunnel tunnel = new Tunnel();
        LoggingByteChannel ch1 = new LoggingByteChannel(tunnel.channel1());
        
        final SaltClientSession client = new SaltClientSession(CryptoTestData.aSig, ch1);
        client.setEncKeyPair(CryptoTestData.aEnc);
        client.setWantedServer(new byte[CryptoLib.SIGN_PUBLIC_KEY_BYTES]);
        
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    server.handshake();
                } catch (NoSuchServer e) {
                    // empty
                }
            }
        });
        thread.start();
        
        try {
            client.handshake();
        } catch (NoSuchServer e) {
            // empty
        }
        
        List<LoggingByteChannel.Entry> entries = ch1.getLog();
        LoggingByteChannel.Entry last = entries.get(1);
        PacketHeader header = new PacketHeader(last.bytes, 0);
        
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(true, header.lastFlag());
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
        
        tunnel.channel1().write(false, badM1);
        
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
                ch.write(false, badM2);
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
    
    @Test
    public void testBufferedM4() {
        final Tunnel tunnel = new Tunnel();
        final SaltClientSession client = new SaltClientSession(CryptoTestData.aSig, tunnel.channel1());
        client.setEncKeyPair(CryptoTestData.aEnc);
        client.setBufferM4(true);
        
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        server.setBufferM2(true);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
                ByteChannel ch = server.getChannel();
                byte[] data1 = ch.read();
                ch.write(false, data1);
                
                byte[] data2 = ch.read();
                byte[] data3 = ch.read();
                ch.write(true, data2, data3);
            }
        });
        thread.start();
        
        client.handshake();
        ByteChannel channel = client.getChannel();
        channel.write(false, new byte[]{0x01, 0x05, 0x05, 0x05, 0x05, 0x05});
        byte[] response1 = channel.read();
        channel.write(false, 
                new byte[]{0x01, 0x04, 0x04, 0x04, 0x04},
                new byte[]{0x03, 0x03, 0x03, 0x03});
        byte[] response2 = channel.read();
        byte[] response3 = channel.read();
        
        Assert.assertArrayEquals(new byte[]{0x01, 0x05, 0x05, 0x05, 0x05, 0x05}, response1);
        Assert.assertArrayEquals(new byte[]{0x01, 0x04, 0x04, 0x04, 0x04}, response2);
        Assert.assertArrayEquals(new byte[]{0x03, 0x03, 0x03, 0x03}, response3);
    }
}
