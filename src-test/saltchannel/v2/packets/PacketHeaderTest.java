package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

public class PacketHeaderTest {

    @Test
    public void testLastFlag() {
        PacketHeader h = PacketHeader.create();
        Assert.assertEquals(false, h.lastFlag());
        h.setLastFlag(true);
        Assert.assertEquals(true, h.lastFlag());
    }
    
    @Test
    public void testBit3() {
        PacketHeader h = PacketHeader.create();
        Assert.assertEquals(false, h.getBit(3));
        h.setBit(3, true);
        Assert.assertEquals(true, h.getBit(3));
    }
}
