package saltchannel.v2;

import org.junit.Assert;
import org.junit.Test;

import saltchannel.CryptoLib;
import saltchannel.Tunnel;
import saltchannel.a1a2.A1Client;
import saltchannel.a1a2.A1Packet;
import saltchannel.a1a2.A2Packet;
import saltchannel.testutil.ToWaitFor;
import saltchannel.util.CryptoTestData;

public class A1A2SessionTest {

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
        Assert.assertEquals("SCv2------", a2.prots[0].p1());
        Assert.assertEquals("----------", a2.prots[0].p2());
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
        Assert.assertEquals("SCv2------", a2.prots[0].p1());
        Assert.assertEquals("MyProtV3--", a2.prots[0].p2());
    }
    
    @Test
    public void testInvalidPubkeyInA1() {
        // Reproducing Issue #12
        
        Tunnel tunnel = new Tunnel();
        
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
            }
        });
        thread.start();
        
        A1Client client = new A1Client(tunnel.channel1());
        A1Packet a1 = client.getA1();
        a1.addressType = A1Packet.ADDRESS_TYPE_PUBKEY;
        a1.address = new byte[CryptoLib.SIGN_PUBLIC_KEY_BYTES];
        
        A2Packet a2 = client.go();
        
        Assert.assertEquals(0, a2.prots.length);
        Assert.assertEquals(true, a2.noSuchServer);
    }
    
    @Test
    public void testA1A2SessionAndIsDone() {
        Tunnel tunnel = new Tunnel();
        
        final A1Client client = new A1Client(tunnel.channel1());
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        final boolean[] myIsDone = new boolean[]{false};
        final ToWaitFor toWaitFor = new ToWaitFor();
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
                myIsDone[0] = server.isDone();
                toWaitFor.reportHappened();
            }
        });
        thread.start();
        
        A2Packet a2 = client.go();
        
        toWaitFor.waitForIt(1000);
        
        Assert.assertTrue(toWaitFor.hasHappened());
        Assert.assertEquals(true, myIsDone[0]);
        Assert.assertEquals("SCv2------", a2.prots[0].p1());
    }
    
    
    @Test
    public void testTheGetChannelThrowsForA1A2Session() {
        Tunnel tunnel = new Tunnel();
        
        final A1Client client = new A1Client(tunnel.channel1());
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        final Exception[] exception = new Exception[]{null};
        final ToWaitFor toWaitFor = new ToWaitFor();
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
                try {
                    server.getChannel();
                } catch (IllegalStateException e) {
                    exception[0] = e;
                }
                
                toWaitFor.reportHappened();
            }
        });
        thread.start();
        
        A2Packet a2 = client.go();
        
        toWaitFor.waitForIt(1000);
        
        Assert.assertTrue(toWaitFor.hasHappened());
        Assert.assertTrue(exception[0] != null);
        Assert.assertEquals(IllegalStateException.class.getName(), exception[0].getClass().getName());
        Assert.assertEquals("SCv2------", a2.prots[0].p1());
    }
}
