package saltchannel.v2.packets;

import org.junit.Assert;
import org.junit.Test;

public class PacketHeaderTest {

    @Test
    public void testEosFlag() {
        PacketHeader h = new PacketHeader();
        Assert.assertEquals(false, h.eosFlag());
        h.setEosFlag(true);
        Assert.assertEquals(true, h.eosFlag());
    }
    
    @Test
    public void testBit3() {
        PacketHeader h = new PacketHeader();
        Assert.assertEquals(false, h.getBit(3));
        h.setBit(3, true);
        Assert.assertEquals(true, h.getBit(3));
    }
}
