package saltchannel;

import java.io.UnsupportedEncodingException;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

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
