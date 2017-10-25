package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

public class M2PacketTest {

    @Test
    public void testSanity() {
        M2Message p = new M2Message();
        p.noSuchServer = false;
        p.serverEncKey = new byte[32];

        byte[] bytes1 = new byte[p.getSize()];
        p.toBytes(bytes1, 0);
        
        M2Message p2 = M2Message.fromBytes(bytes1, 0);
        
        byte[] bytes2 = new byte[bytes1.length];
        p2.toBytes(bytes2, 0);
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
    
    @Test
    public void testResumeSupported() {
        M2Message p = new M2Message();
        p.noSuchServer = false;
        p.resumeSupported = true;
        p.serverEncKey = new byte[32];

        byte[] bytes1 = new byte[p.getSize()];
        p.toBytes(bytes1, 0);
        
        M2Message p2 = M2Message.fromBytes(bytes1, 0);
        
        byte[] bytes2 = new byte[bytes1.length];
        p2.toBytes(bytes2, 0);
        
        Assert.assertArrayEquals(bytes1, bytes2);
        Assert.assertTrue(p2.resumeSupported);
    }
}
