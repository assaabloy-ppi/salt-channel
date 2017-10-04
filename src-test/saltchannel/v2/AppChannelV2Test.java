package saltchannel.v2;

import org.junit.Assert;
import org.junit.Test;
import saltchannel.TimeException;
import saltchannel.Tunnel;
import saltchannel.util.TimeChecker;
import saltchannel.util.TimeKeeper;

public class AppChannelV2Test {

    @Test
    public void testSanity() {
        // One app message, single write.
        Tunnel tunnel = new Tunnel();
        AppChannelV2 c1 = new AppChannelV2(tunnel.channel1(), TimeKeeper.NULL, TimeChecker.NULL);
        AppChannelV2 c2 = new AppChannelV2(tunnel.channel2(), TimeKeeper.NULL, TimeChecker.NULL);
        
        c1.write(new byte[]{0x10});
        byte[] message = c2.read();
        
        Assert.assertArrayEquals(new byte[]{0x10}, message);
    }
    
    @Test
    public void testMultiAppPacket1() {
        // Two app message in one write, MultiAppPacket should be used.
        
        Tunnel tunnel = new Tunnel();
        AppChannelV2 c1 = new AppChannelV2(tunnel.channel1(), TimeKeeper.NULL, TimeChecker.NULL);
        AppChannelV2 c2 = new AppChannelV2(tunnel.channel2(), TimeKeeper.NULL, TimeChecker.NULL);
        
        c1.write(false, new byte[]{0x10}, new byte[]{0x20, 0x21});
        byte[] message1 = c2.read();
        byte[] message2 = c2.read();
        
        Assert.assertArrayEquals(new byte[]{0x10}, message1);
        Assert.assertArrayEquals(new byte[]{0x20, 0x21}, message2);
    }
    
    @Test(expected = TimeException.class)
    public void testDelay1() {
        // One app message, single write. Time delay.
        
        TimeKeeper timeKeeper = new TimeKeeper() {
            public int getFirstTime() {
                return 1;
            }

            public int getTime() {
                return 30*1000;   // 30 s is too long
            }
        };
        
        TimeChecker timeChecker = new TimeChecker() {
            public void reportFirstTime(int time) {}

            public void checkTime(int time) {
                if (time > 10*1000) {
                    throw new TimeException("delay detected");
                }
            }
        };
        
        Tunnel tunnel = new Tunnel();
        AppChannelV2 c1 = new AppChannelV2(tunnel.channel1(), timeKeeper, TimeChecker.NULL);
        AppChannelV2 c2 = new AppChannelV2(tunnel.channel2(), TimeKeeper.NULL, timeChecker);
        
        c1.write(new byte[]{0x10});
        byte[] message = c2.read();
        
        Assert.assertArrayEquals(new byte[]{0x10}, message);
    }
    
    @Test(expected = TimeException.class)
    public void testDelay2() {
        // MultiAppPacket, delayed packet.
        
        TimeKeeper timeKeeper = new TimeKeeper() {
            public int getFirstTime() {
                return 1;
            }

            public int getTime() {
                return 30*1000;   // 30 s is too long
            }
        };
        
        TimeChecker timeChecker = new TimeChecker() {
            public void reportFirstTime(int time) {}

            public void checkTime(int time) {
                if (time > 10*1000) {
                    throw new TimeException("delay detected");
                }
            }
        };
        
        Tunnel tunnel = new Tunnel();
        AppChannelV2 c1 = new AppChannelV2(tunnel.channel1(), timeKeeper, TimeChecker.NULL);
        AppChannelV2 c2 = new AppChannelV2(tunnel.channel2(), TimeKeeper.NULL, timeChecker);
        
        c1.write(new byte[]{0x10}, new byte[]{0x20, 0x21});
        byte[] message1 = c2.read();
        byte[] message2 = c2.read();
        Assert.assertArrayEquals(new byte[]{0x10}, message1);
        Assert.assertArrayEquals(new byte[]{0x20, 0x21}, message2);
    }
}
