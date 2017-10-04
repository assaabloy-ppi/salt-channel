package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;
import saltchannel.BadPeer;

public class MultiAppPacketTest {

    @Test
    public void testSanity() {
        MultiAppPacket p = new MultiAppPacket();
        p.appMessages = new byte[][]{
            {0x04}
        };
        
        byte[] bytes1 = new byte[p.getSize()];
        p.toBytes(bytes1, 0);
        
        MultiAppPacket p2 = MultiAppPacket.fromBytes(bytes1, 0, bytes1.length);
        
        Assert.assertEquals(1, p2.appMessages.length);
        Assert.assertEquals(1, p2.appMessages[0].length);
        Assert.assertEquals(0x04, p2.appMessages[0][0]);
    }
    
    @Test
    public void testMultipleParts() {
        MultiAppPacket p = new MultiAppPacket();
        p.appMessages = new byte[][]{
            {0x04},
            {0x05, 0x05}
        };
        
        byte[] bytes1 = new byte[p.getSize()];
        p.toBytes(bytes1, 0);        
        byte[] bytes2 = new byte[bytes1.length];
        MultiAppPacket.fromBytes(bytes1, 0, bytes1.length).toBytes(bytes2, 0);
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
    
    @Test(expected=BadPeer.class)
    public void testTooSmall1() {
        byte[] packetBytes = new byte[1];
        MultiAppPacket.fromBytes(packetBytes, 0, packetBytes.length);
    }
    
    @Test(expected=BadPeer.class)
    public void testBadCount() {
        MultiAppPacket p = new MultiAppPacket();
        p.appMessages = new byte[][]{
            {0x04}
        };
        
        byte[] bytes1 = new byte[p.getSize()];
        p.toBytes(bytes1, 0);        
        
        // manipulate:
        bytes1[2 + 4] = 9;   // count set to 9 instead of 1
        
        byte[] bytes2 = new byte[bytes1.length];
        MultiAppPacket.fromBytes(bytes1, 0, bytes1.length).toBytes(bytes2, 0);
    }
    
    @Test
    public void testShouldUse1() {
        byte[][] appMessages = new byte[][]{
            {0x01}
        };
        
        Assert.assertEquals(false, MultiAppPacket.shouldUse(appMessages));
    }
    
    @Test
    public void testShouldUse2() {
        byte[][] appMessages = new byte[][]{
            {0x04},
            {0x05, 0x05}
        };
        
        Assert.assertEquals(true, MultiAppPacket.shouldUse(appMessages));
    }
    
    @Test
    public void testShouldUse3() {
        byte[][] appMessages = new byte[][]{
            {0x04},
            new byte[65536]
        };
        
        Assert.assertEquals(false, MultiAppPacket.shouldUse(appMessages));
    }
}
