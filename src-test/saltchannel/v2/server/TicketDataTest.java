package saltchannel.v2.server;

import org.junit.Assert;
import org.junit.Test;

public class TicketDataTest {

    @Test
    public void testSanity() {
        TicketData d1 = new TicketData();
        d1.clientSigKey = new byte[TicketData.CLIENT_SIG_KEY_SIZE];
        d1.clientSigKey[0] = 12;
        d1.sessionKey = new byte[TicketData.SESSION_KEY_SIZE];
        d1.sessionKey[0] = 13;
        
        byte[] bytes1 = d1.toBytes();
        TicketData d2 = TicketData.fromBytes(bytes1, 0);
        byte[] bytes2 = d2.toBytes();
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
}
