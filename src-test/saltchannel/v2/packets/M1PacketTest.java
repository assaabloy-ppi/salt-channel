package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

public class M1PacketTest {

    @Test
    public void testSanity() {
        M1Message p = new M1Message();
        p.clientEncKey = new byte[32];
        p.serverSigKey = null;
        
        byte[] bytes1 = new byte[p.getSize()];
        p.toBytes(bytes1, 0);
        byte[] bytes2 = new byte[bytes1.length];
        M1Message.fromBytes(bytes1, 0).toBytes(bytes2, 0);
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
    
    @Test
    public void testWithTicket() {
        M1Message p = new M1Message();
        p.clientEncKey = new byte[32];
        p.serverSigKey = null;
        p.ticket = new byte[20];    // dummy data
        
        byte[] bytes1 = new byte[p.getSize()];
        p.toBytes(bytes1, 0);
        byte[] bytes2 = new byte[bytes1.length];
        M1Message.fromBytes(bytes1, 0).toBytes(bytes2, 0);
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
    
    
    @Test
    public void testWithTicketRequested() {
        M1Message p = new M1Message();
        p.clientEncKey = new byte[32];
        p.serverSigKey = null;
        p.ticketRequested = true;
        
        byte[] bytes1 = new byte[p.getSize()];
        p.toBytes(bytes1, 0);
        byte[] bytes2 = new byte[bytes1.length];
        M1Message.fromBytes(bytes1, 0).toBytes(bytes2, 0);
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
}
