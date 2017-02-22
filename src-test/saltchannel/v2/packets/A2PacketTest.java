package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

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
}
