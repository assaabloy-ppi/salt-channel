package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

public class M1PacketTest {

    @Test
    public void testSanity() {
        M1Packet p = new M1Packet();
        p.clientEncKey = new byte[32];
        p.serverSigKey = null;
        p.ticket = null;
        p.ticketRequested = false;
        
        byte[] bytes1 = p.toBytes();
        byte[] bytes2 = M1Packet.fromBytes(bytes1).toBytes();
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
}
