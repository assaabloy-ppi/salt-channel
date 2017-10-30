package saltchannel.dev;

import org.junit.Assert;
import org.junit.Test;
import saltchannel.util.TimeKeeper;

public class ExampleSession3Test {

    @Test
    public void testTimeKeeperSanity() {
        TimeKeeper tk = new ExampleSession3.OneTwoThreeTimeKeeper();
        Assert.assertEquals(1, tk.getFirstTime());
        Assert.assertEquals(2, tk.getTime());
        Assert.assertEquals(3, tk.getTime());
        Assert.assertEquals(4, tk.getTime());
    }
}
