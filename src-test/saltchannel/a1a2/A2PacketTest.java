package saltchannel.a1a2;

import org.junit.Assert;
import org.junit.Test;

import saltchannel.a1a2.A2Packet;

public class A2PacketTest {

    @Test
    public void testSanity() {
        A2Packet p = new A2Packet();
        p.prots = new A2Packet.Prot[2];
        p.prots[0] = new A2Packet.Prot("SC2-------", "IoTX1-----");
        p.prots[1] = new A2Packet.Prot("SC3-------", "IoTX1-----");
        
        byte[] bytes1 = new byte[p.getSize()];
        p.toBytes(bytes1, 0);
        byte[] bytes2 = new byte[bytes1.length];
        A2Packet.fromBytes(bytes1, 0).toBytes(bytes2, 0);
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testTooShortP2() {
        A2Packet p = new A2Packet();
        p.prots = new A2Packet.Prot[1];
        p.prots[0] = new A2Packet.Prot("SC2-------", "X1-----");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidCharInP2() {
        A2Packet p = new A2Packet();
        p.prots = new A2Packet.Prot[1];
        p.prots[0] = new A2Packet.Prot("SC2-------", "X--%------");
    }
    
    @Test
    public void testNoSuchServerBit() {
        A2Packet p = new A2Packet();
        p.prots = new A2Packet.Prot[0];
        p.noSuchServer = true;
        
        byte[] dest = new byte[p.getSize()];
        p.toBytes(dest, 0);
        
        A2Packet p2 = A2Packet.fromBytes(dest, 0);
        
        Assert.assertEquals(true, p2.noSuchServer);
    }
}
