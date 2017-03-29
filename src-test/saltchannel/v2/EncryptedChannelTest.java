package saltchannel.v2;

import java.io.UnsupportedEncodingException;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;
import saltchannel.ComException;
import saltchannel.Tunnel;

public class EncryptedChannelTest {
    
    @Test
    public void testEncryptDecrypt() throws UnsupportedEncodingException {
        byte[] key = key1();
        Tunnel tunnel = new Tunnel();
        EncryptedChannelV2 e1 = new EncryptedChannelV2(tunnel.channel1(), key, 
                EncryptedChannelV2.Role.CLIENT);
        EncryptedChannelV2 e2 = new EncryptedChannelV2(tunnel.channel2(), key, 
                EncryptedChannelV2.Role.SERVER);
        
        byte[] clear1 = "AAAA".getBytes("UTF-8");
        byte[] encrypted = e1.encrypt(clear1);
        Assert.assertEquals(clear1.length + 16, encrypted.length);
        
        byte[] clear2 = e2.decrypt(encrypted);
        Assert.assertArrayEquals(clear1, clear2);
    }
    
    @Test
    public void testWrapUnwrap() {
        byte[] bytes1 = new byte[20];
        bytes1[10] = 10;
        byte[] wrapped = EncryptedChannelV2.wrap(bytes1);
        byte[] bytes2 = EncryptedChannelV2.unwrap(wrapped);
        Assert.assertArrayEquals(bytes1, bytes2);
    }

    @Test
    public void testChannel() throws ComException, UnsupportedEncodingException {
        byte[] key = key1();
        Tunnel tunnel = new Tunnel();
        EncryptedChannelV2 e1 = new EncryptedChannelV2(tunnel.channel1(), key, 
                EncryptedChannelV2.Role.CLIENT);
        EncryptedChannelV2 e2 = new EncryptedChannelV2(tunnel.channel2(), key, 
                EncryptedChannelV2.Role.SERVER);
        
        byte[] clear1 = "AAAA".getBytes("UTF-8");
        e1.write(clear1);
        byte[] clear2 = e2.read();
        
        Assert.assertArrayEquals(clear1, clear2);
    }
    
    @Test
    public void testMultipleWrites() {
        byte[] key = key1();
        Tunnel tunnel = new Tunnel();
        EncryptedChannelV2 e1 = new EncryptedChannelV2(tunnel.channel1(), key, 
                EncryptedChannelV2.Role.CLIENT);
        EncryptedChannelV2 e2 = new EncryptedChannelV2(tunnel.channel2(), key, 
                EncryptedChannelV2.Role.SERVER);
        
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
        EncryptedChannelV2 e1 = new EncryptedChannelV2(tunnel.channel1(), key, 
                EncryptedChannelV2.Role.CLIENT);
        EncryptedChannelV2 e2 = new EncryptedChannelV2(tunnel.channel2(), key, 
                EncryptedChannelV2.Role.SERVER);
        
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
        EncryptedChannelV2 e1 = new EncryptedChannelV2(tunnel.channel1(), key, 
                EncryptedChannelV2.Role.CLIENT);
        EncryptedChannelV2 e2 = new EncryptedChannelV2(tunnel.channel2(), key, 
                EncryptedChannelV2.Role.SERVER);
        
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
        EncryptedChannelV2 e1 = new EncryptedChannelV2(tunnel.channel1(), key, 
                EncryptedChannelV2.Role.CLIENT);
        EncryptedChannelV2 e2 = new EncryptedChannelV2(tunnel.channel2(), key, 
                EncryptedChannelV2.Role.SERVER);
        
        byte[] m1 = new byte[]{1};
        byte[] m2 = new byte[]{2};
        
        e2.write(m1);
        byte[] m1b = e1.read();
        Assert.assertArrayEquals(m1, m1b);
        
        e1.write(m2);
        byte[] m2b = e2.read();
        Assert.assertArrayEquals(m2, m2b);
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
