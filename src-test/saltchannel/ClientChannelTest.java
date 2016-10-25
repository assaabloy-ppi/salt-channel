package saltchannel;

import org.junit.Assert;
import org.junit.Test;

import saltchannel.util.Hex;

/**
 * Tests for ClientChannel.
 * 
 * @author Frans Lundberg
 */
public class ClientChannelTest {
    
    @Test
    public void testClientBasedOnSpecData() {
        // Tests ClientChannel using session example from spec-salt-channel.
        // The server-side is handled manually with raw hard-coded messages.
        
        final KeyPair clientSigKeyPair = KeyPair.fromHex(
                "55f4d1d198093c84de9ee9a6299e0f6891c2e1d0b369efb592a9e3f169fb0f795529ce8ccf68c0b8ac19d437ab0f5b32723782608e93c6264f184ba152c2357b", 
                "5529ce8ccf68c0b8ac19d437ab0f5b32723782608e93c6264f184ba152c2357b");
        final KeyPair clientEncKeyPair = KeyPair.fromHex(
                "77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a", 
                "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a");
        
        byte[] m1Expected = Hex.toBytes("4014016518208520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a1401701402533141");
        byte[] m2 = Hex.toBytes("401401651820de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f41");
        byte[] m3 = Hex.toBytes("40140162187cc11a5740752f9ef562f7552123819a0085d9da0ea02ed4a1be9fd079eaab69d5e5b528668fdfae7bf4e9656c5c70ef9d151d5442c67932720146f779bf2089e7313840b9f153a83541ed446626de2d185b7aeffeefa70520ede8b68f96cb30b1566684efdcd28c962d1bfee1ee2a8367db31eab1d313dcb9d65853cb41");
        byte[] m4Expected = Hex.toBytes("40140162187cdae551bde10f0b543bbc591125c6e646f73bfc662578a54bdcc8eef60a47d0bf53057418b6054eb260cca4d827c068edff9efb48f0eb7ed71646480906c138b023aac5262616246da2481b0944ab80f41c3db20568bc40b100d72c90f75b7ec411f1d23ad620d89da9a35e3a01685041280219cd05c40e4e60ffb26541");
        
        TweetLib tweet = TweetLib.createFastAndInsecure();
        Tunnel tunnel = new Tunnel();
        ByteChannel serverChannel = tunnel.ch2();
        
        final ClientChannel clientChannel = new ClientChannel(tweet, tunnel.ch1());
        clientChannel.initEphemeralKeyPair(clientEncKeyPair);
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                clientChannel.handshake(clientSigKeyPair, null, true);
            }
        });
        thread.start();
        
        byte[] m1 = serverChannel.read();
        Assert.assertArrayEquals("m1", m1Expected, m1);
        
        serverChannel.write(m2, m3);
        
        byte[] m4 = serverChannel.read();
        Assert.assertArrayEquals("m4", m4Expected, m4);
    }
}
