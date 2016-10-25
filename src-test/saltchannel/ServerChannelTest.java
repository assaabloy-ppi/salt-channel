package saltchannel;

import org.junit.Assert;
import org.junit.Test;
import saltchannel.util.Hex;
import saltchannel.util.KeyPair;

/**
 * Tests ServerChannel.
 * 
 * @author Frans Lundberg
 */
public class ServerChannelTest {
    
    @Test
    public void testServerReplyBasedOnSpecData() {
        // Checks reply from server (M2, M3) with example session data in spec-salt-channel.md.
        // The client part is handled manually with hard-coded messages.
        
        final KeyPair serverKeyPair = KeyPair.fromHex(
                "7a772fa9014b423300076a2ff646463952f141e2aa8d98263c690c0d72eed52d07e28d4ee32bfdc4b07d41c92193c0c25ee6b3094c6296f373413b373d36168b", 
                "07e28d4ee32bfdc4b07d41c92193c0c25ee6b3094c6296f373413b373d36168b");
        
        final KeyPair serverEphemeralKeyPair = KeyPair.fromHex(
                "5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb", 
                "de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f");
        
        byte[] m1 = Hex.toBytes("4014016518208520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a1401701402533141");
        byte[] m2Expected = Hex.toBytes("401401651820de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f41");
        byte[] m3Expected = Hex.toBytes("40140162187cc11a5740752f9ef562f7552123819a0085d9da0ea02ed4a1be9fd079eaab69d5e5b528668fdfae7bf4e9656c5c70ef9d151d5442c67932720146f779bf2089e7313840b9f153a83541ed446626de2d185b7aeffeefa70520ede8b68f96cb30b1566684efdcd28c962d1bfee1ee2a8367db31eab1d313dcb9d65853cb41");
        byte[] m4 = Hex.toBytes("40140162187cdae551bde10f0b543bbc591125c6e646f73bfc662578a54bdcc8eef60a47d0bf53057418b6054eb260cca4d827c068edff9efb48f0eb7ed71646480906c138b023aac5262616246da2481b0944ab80f41c3db20568bc40b100d72c90f75b7ec411f1d23ad620d89da9a35e3a01685041280219cd05c40e4e60ffb26541");
        
        ChannelCryptoLib tweet = ChannelCryptoLib.createInsecureAndFast();
        Tunnel tunnel = new Tunnel();
        ByteChannel client = tunnel.channel1();
        
        final ServerChannel serverChannel = new ServerChannel(tweet, tunnel.channel2());
        serverChannel.initEphemeralKeyPair(serverEphemeralKeyPair);
        
        client.write(m1);
        
        Thread serverThread = new Thread(new Runnable() {
            public void run() {
                serverChannel.handshake(serverKeyPair);
            }
        });
        serverThread.start();
        
        byte[] m2 = client.read();
        Assert.assertArrayEquals(m2Expected, m2);
        
        byte[] m3 = client.read();
        Assert.assertArrayEquals(m3Expected, m3);
        
        client.write(m4);
    }
}
