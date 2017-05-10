package saltchannel.v2;

import org.junit.Assert;
import org.junit.Test;

import saltchannel.util.CryptoTestData;
import saltchannel.v2.packets.BadTicket;

public class ResumeHandlerTest {
    @Test
    public void testValidTicket() {
        ResumeHandler h = handler1();
        byte[] clientSigKey = new byte[32];
        byte[] sessionKey = new byte[32];
        
        byte[] ticket = h.issueTicket(clientSigKey, sessionKey).ticket;
        TicketSessionData data = h.validateTicket(ticket);
        
        Assert.assertEquals(data.ticketId, 1000);
    }
    
    @Test(expected=BadTicket.class)
    public void testInvalidTicket() {
        ResumeHandler h = handler1();
        byte[] clientSigKey = new byte[32];
        byte[] sessionKey = new byte[32];
        byte[] ticket = h.issueTicket(clientSigKey, sessionKey).ticket;
        ticket[70] = 70;
        h.validateTicket(ticket);
    }
    
    @Test
    public void testClearedTicket() {
        ResumeHandler h = handler1();
        byte[] clientSigKey = new byte[32];
        byte[] sessionKey = new byte[32];
        byte[] ticket = h.issueTicket(clientSigKey, sessionKey).ticket;
        
        h.validateTicket(ticket);
        
        BadTicket ex = null;
        
        try {
            h.validateTicket(ticket);
        } catch (BadTicket e) {
            ex = e;
        }
        
        Assert.assertTrue(ex != null);
    }
    
    private static ResumeHandler handler1() {
        byte[] key = CryptoTestData.random32a;
        return new ResumeHandler(key, 1000, 8*100);
    }
}
