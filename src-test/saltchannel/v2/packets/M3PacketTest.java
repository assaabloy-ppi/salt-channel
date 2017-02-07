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
        
        byte[] bytes1 = p1.toBytes();
        
        M3Packet p2 = M3Packet.fromBytes(bytes1);
        
        Assert.assertEquals(p1.hasServerSigKey(), p2.hasServerSigKey());
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
        
        byte[] bytes1 = p1.toBytes();
        byte[] bytes2 = M3Packet.fromBytes(bytes1).toBytes();
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
}
