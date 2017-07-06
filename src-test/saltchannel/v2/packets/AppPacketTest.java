package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

import saltchannel.BadPeer;

public class AppPacketTest {

    @Test
    public void testSanity() {
        AppPacket p = new AppPacket();
        p.appData = new byte[12];
        
        byte[] bytes1 = new byte[p.getSize()];
        p.toBytes(bytes1, 0);
        byte[] bytes2 = new byte[bytes1.length];
        AppPacket.fromBytes(bytes1, 0, bytes1.length).toBytes(bytes2, 0);
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
    
    @Test(expected=BadPeer.class)
    public void testTooSmall1() {
        byte[] packetBytes = new byte[1];
        AppPacket.fromBytes(packetBytes, 0, packetBytes.length);
    }
    
    
    @Test(expected=BadPeer.class)
    public void testTooSmall2() {
        byte[] packetBytes = new byte[5];
        AppPacket.fromBytes(packetBytes, 0, packetBytes.length);
    }
}
