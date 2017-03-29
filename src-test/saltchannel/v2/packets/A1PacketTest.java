package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

import a1a2.A1Packet;

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
}
