package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

public class M1PacketTest {

    @Test
    public void testSanity() {
        M1Packet p = new M1Packet();
        p.clientEncKey = new byte[32];
        p.serverSigKey = null;
        
        byte[] bytes1 = new byte[p.getSize()];
        p.toBytes(bytes1, 0);
        byte[] bytes2 = new byte[bytes1.length];
        M1Packet.fromBytes(bytes1, 0).toBytes(bytes2, 0);
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
}
