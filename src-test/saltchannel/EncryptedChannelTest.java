package saltchannel;

import java.io.UnsupportedEncodingException;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

import saltchannel.util.Hex;

/**
 * Tests for EncryptedChannel.
 * 
 * @author Frans Lundberg
 */
public class EncryptedChannelTest {

    @Test
    public void testSanity() throws ComException, UnsupportedEncodingException {
        byte[] key = key1();
        Tunnel tunnel = new Tunnel();
        EncryptedChannel e1 = new EncryptedChannel(tunnel.ch1(), key, EncryptedChannel.Role.CLIENT);
        EncryptedChannel e2 = new EncryptedChannel(tunnel.ch2(), key, EncryptedChannel.Role.SERVER);
        
        e1.write("Hello!".getBytes("UTF-8"));
        String string = new String(e2.read(), "UTF-8");
        
        Assert.assertEquals("Hello!", string);
    }
    
    @Test
    public void testMultipleWrites() {
        byte[] key = key1();
        Tunnel tunnel = new Tunnel();
        EncryptedChannel e1 = new EncryptedChannel(tunnel.ch1(), key, EncryptedChannel.Role.CLIENT);
        EncryptedChannel e2 = new EncryptedChannel(tunnel.ch2(), key, EncryptedChannel.Role.SERVER);
        
        byte[] m1 = new byte[]{1};
        byte[] m2 = new byte[]{2};
        
        e1.write(m1);
        e1.write(m2);
        
        byte[] m1b = e2.read();
        byte[] m2b = e2.read();
        
        Assert.assertArrayEquals(m1, m1b);
        Assert.assertArrayEquals(m2, m2b);
    }
    
    
    @Test
    public void testMultipleMessageInOneWrite() {
        byte[] key = key1();
        Tunnel tunnel = new Tunnel();
        EncryptedChannel e1 = new EncryptedChannel(tunnel.ch1(), key, EncryptedChannel.Role.CLIENT);
        EncryptedChannel e2 = new EncryptedChannel(tunnel.ch2(), key, EncryptedChannel.Role.SERVER);
        
        byte[] m1 = new byte[]{1};
        byte[] m2 = new byte[]{2};
        
        e1.write(m1, m2);
        
        byte[] m1b = e2.read();
        byte[] m2b = e2.read();
        
        Assert.assertArrayEquals(m1, m1b);
        Assert.assertArrayEquals(m2, m2b);
    }
    
    @Test
    public void testBothWays() {
        // A dialog, two round-trips, client-server.
        
        byte[] key = key2();
        Tunnel tunnel = new Tunnel();
        EncryptedChannel e1 = new EncryptedChannel(tunnel.ch1(), key, EncryptedChannel.Role.CLIENT);
        EncryptedChannel e2 = new EncryptedChannel(tunnel.ch2(), key, EncryptedChannel.Role.SERVER);
        
        byte[] m1 = new byte[]{1};
        byte[] m2 = new byte[]{2};
        byte[] m3 = new byte[]{1};
        byte[] m4 = new byte[]{2};
        
        e1.write(m1);
        byte[] m1b = e2.read();
        Assert.assertArrayEquals(m1, m1b);
        
        e2.write(m2);
        byte[] m2b = e1.read();
        Assert.assertArrayEquals(m2, m2b);
        
        e1.write(m3);
        byte[] m3b = e2.read();
        Assert.assertArrayEquals(m3, m3b);
        
        e2.write(m4);
        byte[] m4b = e1.read();
        Assert.assertArrayEquals(m4, m4b);
    }
    
    
    @Test
    public void testFirstMessageFromClient() {
        byte[] key = key2();
        Tunnel tunnel = new Tunnel();
        EncryptedChannel e1 = new EncryptedChannel(tunnel.ch1(), key, EncryptedChannel.Role.CLIENT);
        EncryptedChannel e2 = new EncryptedChannel(tunnel.ch2(), key, EncryptedChannel.Role.SERVER);
        
        byte[] m1 = new byte[]{1};
        byte[] m2 = new byte[]{2};
        
        e2.write(m1);
        byte[] m1b = e1.read();
        Assert.assertArrayEquals(m1, m1b);
        
        e1.write(m2);
        byte[] m2b = e2.read();
        Assert.assertArrayEquals(m2, m2b);
    }
    
    @Test
    public void testWithM3SpecData() {
        // Test data from spec-salt-channel, 2016-10-25.
        
        byte[] key = Hex.toBytes("1b27556473e985d462cd51197a9a46c76009549eac6474f206c4ee0844f68389");
        Tunnel tunnel = new Tunnel();
        EncryptedChannel e2 = new EncryptedChannel(tunnel.ch2(), key, EncryptedChannel.Role.SERVER);
        
        byte[] m3raw = Hex.toBytes("401401671840fad9747882a3d6e9bf4d6bf709f20da72694f839962038fa1b9fc02342733bc01d27847bd131b09355aa055a2c7f554ef1cd5bf7e12c62f77f1d18ace5ca0300140173182007e28d4ee32bfdc4b07d41c92193c0c25ee6b3094c6296f373413b373d36168b41");
        byte[] expected = Hex.toBytes("c11a5740752f9ef562f7552123819a0085d9da0ea02ed4a1be9fd079eaab69d5e5b528668fdfae7bf4e9656c5c70ef9d151d5442c67932720146f779bf2089e7313840b9f153a83541ed446626de2d185b7aeffeefa70520ede8b68f96cb30b1566684efdcd28c962d1bfee1ee2a8367db31eab1d313dcb9d65853cb");
        
        e2.write(m3raw);
        byte[] actual = tunnel.ch1().read();
        
        Assert.assertArrayEquals(expected, actual);
    }
    
    @Test
    public void testWithM4SpecData() {
        // Test data from spec-salt-channel, 2016-10-25.
        
        byte[] key = Hex.toBytes("1b27556473e985d462cd51197a9a46c76009549eac6474f206c4ee0844f68389");
        Tunnel tunnel = new Tunnel();
        EncryptedChannel e1 = new EncryptedChannel(tunnel.ch1(), key, EncryptedChannel.Role.CLIENT);
        
        byte[] m4raw = Hex.toBytes("4014016318205529ce8ccf68c0b8ac19d437ab0f5b32723782608e93c6264f184ba152c2357b1401671840d2383c7eb5e49eac2056feed24b54525507d91594190493b7d4389f27c0ee11152db278248bfa4a3d7b4b15e1b8fb56192f1364f32af658eadf7bd799c814f0741");
        byte[] expected = Hex.toBytes("dae551bde10f0b543bbc591125c6e646f73bfc662578a54bdcc8eef60a47d0bf53057418b6054eb260cca4d827c068edff9efb48f0eb7ed71646480906c138b023aac5262616246da2481b0944ab80f41c3db20568bc40b100d72c90f75b7ec411f1d23ad620d89da9a35e3a01685041280219cd05c40e4e60ffb265");
        
        e1.write(m4raw);
        byte[] actual = tunnel.ch2().read();
        
        Assert.assertArrayEquals(expected, actual);
    }
    
    private static byte[] key1() {
        byte[] key = new byte[32];
        Random random = new Random(0);
        random.nextBytes(key);
        return key;
    }
    
    private static byte[] key2() {
        byte[] key = new byte[32];
        key[1] = 1;
        key[30] = 30;
        return key;
    }
}
