package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

public class M4PacketTest {

    @Test
    public void testSanity() {
        M4Packet p = new M4Packet();
        
        p.clientSigKey = new byte[32];
        p.clientSigKey[7] = 7;
        
        p.signature2 = new byte[64];
        p.signature2[63] = 63;
        
        byte[] bytes1 = new byte[p.getSize()];
        p.toBytes(bytes1, 0);
        
        byte[] bytes2 = new byte[p.getSize()];
        M4Packet.fromBytes(bytes1, 0).toBytes(bytes2, 0);
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
}
