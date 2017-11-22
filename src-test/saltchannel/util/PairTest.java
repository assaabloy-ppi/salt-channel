package saltchannel.util;

import org.junit.Assert;
import org.junit.Test;

public class PairTest {
    @Test
    public void test0() {
        Pair<String, Long> p = new Pair<String, Long>("hello", 33L);
        Assert.assertEquals("hello", p.getValue0());
        Assert.assertEquals(33L, (long) p.getValue1());
    }
}
