package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

public class TTPacketTest {

    @Test
    public void testSanity1() {
        TTPacket p1 = new TTPacket();
        p1.time = 12345;
        p1.sessionNonce = new byte[TTPacket.SESSION_NONCE_SIZE];
        p1.ticket = new byte[21];
        Assert.assertTrue(p1.ticketIncluded());
        
        byte[] p1Bytes = p1.toBytes();
        TTPacket p2 = TTPacket.fromBytes(p1Bytes, 0);
        
        Assert.assertTrue("ticketIncluded", p2.ticketIncluded());
        Assert.assertArrayEquals(p1.ticket, p2.ticket);
    }
}
