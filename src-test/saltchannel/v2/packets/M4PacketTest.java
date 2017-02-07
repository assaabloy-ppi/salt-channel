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
        
        byte[] bytes1 = p.toBytes();
        byte[] bytes2 = M4Packet.fromBytes(bytes1).toBytes();
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
}
