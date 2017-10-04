package saltchannel.v2;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import saltchannel.ByteChannel;
import saltchannel.Tunnel;
import saltchannel.dev.LoggingByteChannel;
import saltchannel.util.CryptoTestData;
import saltchannel.v2.packets.PacketHeader;

/**
 * Testing the isLast flag (lastFlag).
 * 
 * @author Frans Lundberg
 */
public class IsLastTest {

    @Test
    public void testLastFlagInSample1() {
        // Alice (client) to Bob (server). Fixed key pairs.
        // Messages are logged and then analyzed.
        
        Tunnel tunnel = new Tunnel();
        
        LoggingByteChannel clientChannel = new LoggingByteChannel(tunnel.channel1());
        
        final SaltClientSession client = new SaltClientSession(CryptoTestData.aSig, clientChannel);
        client.setEncKeyPair(CryptoTestData.aEnc);
        
        final SaltServerSession server = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        server.setEncKeyPair(CryptoTestData.bEnc);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                server.handshake();
                byte[] appMessage = server.getChannel().read();
                server.getChannel().write(true, appMessage);
            }
        });
        thread.start();
        
        client.handshake();
        
        byte[] app1 = new byte[3000];
        app1[2999] = 99;
        
        ByteChannel channel = client.getChannel();
        channel.write(false, app1);
        byte[] response = channel.read();
        
        Assert.assertArrayEquals(app1, response);
        Assert.assertArrayEquals(CryptoTestData.aSig.pub(), server.getClientSigKey());
        Assert.assertArrayEquals(CryptoTestData.bSig.pub(), client.getServerSigKey());
        
        List<LoggingByteChannel.Entry> log = clientChannel.getLog();
        for (int i = 0; i < log.size(); i++) {
            LoggingByteChannel.Entry entry = log.get(i);  
            PacketHeader header = new PacketHeader(entry.bytes, 0);
            
            if (i < log.size() - 1) {
                Assert.assertEquals("i=" + i, false, header.lastFlag());
            } else {
                Assert.assertEquals("i=" + i, true, header.lastFlag());
            }
        }
    }
}
