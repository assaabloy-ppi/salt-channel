package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

public class M3PacketTest {

    @Test
    public void testSanity1() {
        M3Packet p1 = new M3Packet();
        p1.serverSigKey = new byte[32];
        p1.serverSigKey[0] = 8;
        p1.signature1 = new byte[64];
        p1.signature1[0] = 55;
        
        byte[] bytes1 = new byte[p1.getSize()];
        p1.toBytes(bytes1, 0);
        
        M3Packet p2 = M3Packet.fromBytes(bytes1, 0);
        
        Assert.assertArrayEquals(p1.serverSigKey, p2.serverSigKey);
        Assert.assertArrayEquals(p1.signature1, p2.signature1);
    }
    
    @Test
    public void testSanity2() {
        M3Packet p1 = new M3Packet();
        p1.serverSigKey = new byte[32];
        p1.serverSigKey[0] = 8;
        p1.signature1 = new byte[64];
        p1.signature1[0] = 55;
        
        byte[] bytes1 = new byte[p1.getSize()];
        p1.toBytes(bytes1, 0);
        byte[] bytes2 = new byte[bytes1.length];
        M3Packet.fromBytes(bytes1, 0).toBytes(bytes2, 0);
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
    
    @Test
    public void testWithTicket() {
        M3Packet p1 = new M3Packet();
        p1.serverSigKey = new byte[32];
        p1.serverSigKey[0] = 8;
        p1.signature1 = new byte[64];
        p1.signature1[0] = 55;
        p1.ticket = new byte[20];   // dummy value
        
        byte[] bytes1 = new byte[p1.getSize()];
        p1.toBytes(bytes1, 0);
        byte[] bytes2 = new byte[bytes1.length];
        M3Packet.fromBytes(bytes1, 0).toBytes(bytes2, 0);
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
}
