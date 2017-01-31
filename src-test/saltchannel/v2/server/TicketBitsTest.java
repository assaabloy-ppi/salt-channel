package saltchannel.v2.server;

import org.junit.Assert;
import org.junit.Test;

import saltchannel.v2.server.TicketBits;

public class TicketBitsTest {

    @Test
    public void testIssue1() {
        TicketBits t = new TicketBits(12, 100);
        Assert.assertEquals(12, t.issue());
        Assert.assertEquals(13, t.issue());
    }
    
    @Test
    public void testIsValid() {
        TicketBits t = new TicketBits(12, 100);
        Assert.assertEquals(false, t.isValid(3));
        Assert.assertEquals(false, t.isValid(12));
        Assert.assertEquals(false, t.isValid(13));
        Assert.assertEquals(false, t.isValid(Long.MIN_VALUE));
        Assert.assertEquals(false, t.isValid(Long.MAX_VALUE));
        
        long ticketIndex = t.issue();
        Assert.assertEquals(12, ticketIndex);
        Assert.assertEquals(false, t.isValid(11));
        Assert.assertEquals(true, t.isValid(12));
        Assert.assertEquals(false, t.isValid(13));
    }
}
