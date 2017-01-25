package saltchannel.v2;

import org.junit.Assert;
import org.junit.Test;

public class BitFieldTest {

    @Test
    public void testSanity() {
        BitField f = new BitField(100);
        f.set(10, true);
        f.set(99, true);
        
        Assert.assertEquals(true, f.get(10));
        Assert.assertEquals(true, f.get(99));
        Assert.assertEquals(false, f.get(0));
        Assert.assertEquals(false, f.get(98));
    }
    
    @Test
    public void testSize() {
        BitField f = new BitField(7);
        Assert.assertEquals(7, f.getSize());
    }
    
    @Test
    public void testSetThenClear() {
        BitField f = new BitField(10);
        f.set(3, true);
        f.set(3, false);
        Assert.assertEquals(false, f.get(3));
    }
    
    @Test
    public void testThatBitsAreClearWhenFieldCreated() {
        BitField f = new BitField(10);
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(false, f.get(i));
        }
    }
    
    @Test
    public void testGetAndClear() {
        BitField f = new BitField(10);
        f.set(3, true);
        Assert.assertEquals(true, f.get(3));
        
        boolean value = f.getAndClear(3);
        Assert.assertEquals(true, value);
        Assert.assertEquals(false, f.get(3));
        Assert.assertEquals(false, f.getAndClear(3));
    }
}
