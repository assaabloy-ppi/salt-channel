package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

public class TicketPacketTest {

    @Test
    public void testSanity() {
        TicketPacket p = new TicketPacket();
        p.ticketType = 1;
        p.encryptedTicketNonce = new byte[TicketPacket.ENCRYPTED_NONCE_SIZE];
        p.encrypted = new byte[40];
        
        byte[] bytes1 = p.toBytes();
        byte[] bytes2 = TicketPacket.fromBytes(bytes1).toBytes();
        Assert.assertArrayEquals(bytes1, bytes2);
    }
}
