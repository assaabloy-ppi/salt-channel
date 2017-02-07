package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

public class M2PacketTest {

    @Test
    public void testSanity() {
        M2Packet p = new M2Packet();
        p.badTicket = false;
        p.noSuchServer = false;
        p.resumeSupported = false;
        p.serverEncKey = new byte[32];
        
        byte[] bytes1 = p.toBytes();
        byte[] bytes2 = M2Packet.fromBytes(bytes1).toBytes();
        Assert.assertArrayEquals(bytes1, bytes2);
    }
}
