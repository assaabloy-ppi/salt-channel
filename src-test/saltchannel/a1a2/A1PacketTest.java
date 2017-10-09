package saltchannel.a1a2;

import org.junit.Assert;
import org.junit.Test;

import saltchannel.a1a2.A1Packet;

public class A1PacketTest {

    @Test
    public void testSanity() {
        A1Packet p = new A1Packet();
        
        byte[] bytes1 = new byte[p.getSize()];
        p.toBytes(bytes1, 0);
        byte[] bytes2 = new byte[bytes1.length];
        A1Packet.fromBytes(bytes1, 0).toBytes(bytes2, 0);
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
    
    @Test
    public void testWithAddress() {
        A1Packet p1 = new A1Packet();
        p1.addressType = A1Packet.ADDRESS_TYPE_PUBKEY;
        p1.address = new byte[32];
        
        byte[] bytes1 = new byte[p1.getSize()];
        p1.toBytes(bytes1, 0);
        
        byte[] bytes2 = new byte[bytes1.length];
        A1Packet p2 = A1Packet.fromBytes(bytes1, 0);
        
        Assert.assertEquals(1, p2.addressType);
        Assert.assertEquals(32, p2.address.length);
        
        p2.toBytes(bytes2, 0);
        Assert.assertArrayEquals(bytes1, bytes2);
    }
}
