package saltchannel.v2;

import org.junit.Assert;
import org.junit.Test;
import saltchannel.ByteChannel;
import saltchannel.Tunnel;
import saltchannel.util.CryptoTestData;

/**
 * Testing full client-server sessions with resume tickets.
 */
public class ChannelResumeTest {
    
    @Test
    public void testTicketRequested() {
        // Client requests a ticket from the server. A dummy ResumeHandler is used.
        
        Tunnel tunnel = new Tunnel();
        ResumeHandler resumeHandler = new ResumeHandler(CryptoTestData.random32a, 10, 100*1000);
        
        final SaltClientSession client = new SaltClientSession(CryptoTestData.aSig, tunnel.channel1());
        client.setEncKeyPair(CryptoTestData.aEnc);
        client.setTicketRequested(true);
        
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        server.setResumeHandler(resumeHandler);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
                byte[] app1 = server.getChannel().read();
                byte[] app2 = app1.clone();
                server.getChannel().write(app2);
            }
        });
        thread.start();
        
        client.handshake();
        
        byte[] app1 = new byte[]{120};
        client.getChannel().write(app1);
        byte[] app2 = client.getChannel().read();
        byte[] ticket = client.getNewTicketData().ticket;
        
        Assert.assertArrayEquals(app1, app2);
        Assert.assertTrue(ticket != null);
        Assert.assertTrue(ticket.length > 16);
    }
    
    @Test
    public void testValidTicket() {
        // In a first session, the client requests a ticket.
        // Said ticket is used for a zero-round-trip overhead in a second session.
        
        ResumeHandler resumeHandler = new ResumeHandler(CryptoTestData.random32a, 10, 100*1000);
        
        ClientTicketData ticketData = runSession1(resumeHandler);
        Assert.assertTrue(ticketData != null);
        
        Tunnel tunnel = new Tunnel();
        SaltClientSession client = new SaltClientSession(CryptoTestData.aSig, tunnel.channel1());
        client.setEncKeyPair(CryptoTestData.aEnc);
        client.setTicketRequested(true);
        client.setTicketData(ticketData);
        
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        server.setResumeHandler(resumeHandler);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
                byte[] app1 = server.getChannel().read();
                byte[] app2 = app1.clone();
                server.getChannel().write(app2);
            }
        });
        thread.start();
        
        client.handshake();
        
        ByteChannel appChannel = client.getChannel();
        appChannel.write(new byte[]{5});
        byte[] app2 = appChannel.read();
        
        Assert.assertArrayEquals(new byte[]{5}, app2);
    }

    private ClientTicketData runSession1(ResumeHandler resumeHandler) {
        Tunnel tunnel = new Tunnel();
        
        final SaltClientSession client = new SaltClientSession(CryptoTestData.aSig, tunnel.channel1());
        client.setEncKeyPair(CryptoTestData.aEnc);
        client.setTicketRequested(true);
        
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        server.setResumeHandler(resumeHandler);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
                byte[] app1 = server.getChannel().read();
                byte[] app2 = app1.clone();
                server.getChannel().write(app2);
            }
        });
        thread.start();
        
        client.handshake();
        
        client.getChannel().write(new byte[]{1});
        client.getChannel().read();
        return client.getNewTicketData();
    }
}
