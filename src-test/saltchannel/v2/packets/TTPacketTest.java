package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

public class TTPacketTest {

    @Test
    public void testSanity1() {
        TTPacket p1 = new TTPacket();
        p1.time = 12345;
        p1.ticket = new byte[21];
        Assert.assertTrue(p1.ticketIncluded());
        byte[] bytes1 = p1.toBytes();
        
        TTPacket p2 = TTPacket.fromBytes(bytes1, 0);
        byte[] bytes2 = p2.toBytes();
        
        Assert.assertArrayEquals(p1.ticket, p2.ticket);
        Assert.assertArrayEquals(bytes1, bytes2);
    }
}
