package saltchannel.v2;

import org.junit.Assert;
import org.junit.Test;
import saltchannel.CryptoTestData;

public class ResumeHandlerTest {
    @Test
    public void testValidateTicket() {
        ResumeHandler h = handler1();
        byte[] clientSigKey = new byte[32];
        byte[] sessionKey = new byte[32];
        
        byte[] ticket = h.issueTicket(clientSigKey, sessionKey);
        TicketSessionData data = h.validateTicket(ticket);
        
        Assert.assertEquals(data.ticketIndex, 1000);
    }
    
    private static ResumeHandler handler1() {
        byte[] key = CryptoTestData.random32a;
        return new ResumeHandler(key, 1000, 8*100);
    }
}
