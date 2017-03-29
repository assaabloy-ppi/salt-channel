package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

public class TicketEncryptedPacketTest {
    @Test
    public void testSanity() {
        TicketEncryptedPacket p = new TicketEncryptedPacket();
        p.ticketType = TicketPacket.TICKET_TYPE_1;
        p.ticketId = 1234567890L;
        p.clientSigKey = new byte[32];
        p.sessionNonce = new byte[TTPacket.SESSION_NONCE_SIZE];
        p.sessionKey = new byte[32];
        
        byte[] bytes1 = p.toBytes();
        byte[] bytes2 = TicketEncryptedPacket.fromBytes(bytes1, 0).toBytes();
        Assert.assertArrayEquals(bytes1, bytes2);
    }
}
