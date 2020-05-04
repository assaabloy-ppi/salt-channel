package saltchannel.v2;

import org.junit.Assert;
import org.junit.Test;
import saltaa.SaltLib;
import saltchannel.TimeException;
import saltchannel.Tunnel;
import saltchannel.util.TimeChecker;
import saltchannel.util.TimeKeeper;

public class ApplicationChannelTest {

    @Test
    public void testSanity() {
        // One app message, single write.
        Tunnel tunnel = new Tunnel();
        ApplicationChannel c1 = new ApplicationChannel(tunnel.channel1(), TimeKeeper.NULL, TimeChecker.NULL);
        ApplicationChannel c2 = new ApplicationChannel(tunnel.channel2(), TimeKeeper.NULL, TimeChecker.NULL);
        
        c1.write(false, new byte[]{0x10});
        byte[] message = c2.read();
        
        Assert.assertArrayEquals(new byte[]{0x10}, message);
    }
    
    @Test
    public void testMultiAppPacket1() {
        // Two app message in one write, MultiAppPacket should be used.
        
        Tunnel tunnel = new Tunnel();
        ApplicationChannel c1 = new ApplicationChannel(tunnel.channel1(), TimeKeeper.NULL, TimeChecker.NULL);
        ApplicationChannel c2 = new ApplicationChannel(tunnel.channel2(), TimeKeeper.NULL, TimeChecker.NULL);
        
        c1.write(false, new byte[]{0x10}, new byte[]{0x20, 0x21});
        byte[] message1 = c2.read();
        byte[] message2 = c2.read();
        
        Assert.assertArrayEquals(new byte[]{0x10}, message1);
        Assert.assertArrayEquals(new byte[]{0x20, 0x21}, message2);
    }
    
    @Test
    public void testMultiAppPacket2() {
	// For Issue #20
        
        Tunnel tunnel = new Tunnel();
        ApplicationChannel c1 = new ApplicationChannel(tunnel.channel1(), TimeKeeper.NULL, TimeChecker.NULL);
        ApplicationChannel c2 = new ApplicationChannel(tunnel.channel2(), TimeKeeper.NULL, TimeChecker.NULL);
        
        c1.write(false, new byte[] {0x01});
        c1.write(false, new byte[] {0x02}, new byte[] {0x03}, new byte[] {0x04});
        c1.write(false, new byte[] {0x05});
       
        Assert.assertArrayEquals(new byte[]{0x01}, c2.read());
        Assert.assertArrayEquals(new byte[]{0x02}, c2.read());
        Assert.assertArrayEquals(new byte[]{0x03}, c2.read());
        Assert.assertArrayEquals(new byte[]{0x04}, c2.read());
        Assert.assertArrayEquals(new byte[]{0x05}, c2.read());
    }
    
    @Test
    public void testMultiAppPacket3() {
	// For Issue #20
        
        Tunnel tunnel = new Tunnel();
        ApplicationChannel c1 = new ApplicationChannel(tunnel.channel1(), TimeKeeper.NULL, TimeChecker.NULL);
        ApplicationChannel c2 = new ApplicationChannel(tunnel.channel2(), TimeKeeper.NULL, TimeChecker.NULL);
        
        c1.write(false, new byte[] {1}, new byte[] {2}, new byte[] {3}, new byte[] {4});
        c1.write(false, new byte[] {5}, new byte[] {6});
        c1.write(true, new byte[] {7});
       
        for (byte i = 1; i <= 7; i++) {
            Assert.assertArrayEquals("Message " + i, new byte[]{i}, c2.read());
        }
    }
    
    @Test
    public void testAvailable() {
        Tunnel tunnel = new Tunnel();
        ApplicationChannel c1 = new ApplicationChannel(tunnel.channel1(), TimeKeeper.NULL, TimeChecker.NULL);
        ApplicationChannel c2 = new ApplicationChannel(tunnel.channel2(), TimeKeeper.NULL, TimeChecker.NULL);
        
        c1.write(false, new byte[]{0x10}, new byte[]{0x20, 0x21});
        c2.read();
        
        Assert.assertEquals(1, c2.availableFromMultiAppPacket());
    }
    
    @Test
    public void testLastFlag1() {
        // Note, the lastFlag is implemented in EncryptedChannelV2, so it must
        // be included when testing this.
        
        Tunnel tunnel = new Tunnel();
        byte[] sharedKey = new byte[SaltLib.crypto_box_SHAREDKEYBYTES];
        sharedKey[9] = 0x09;
        EncryptedChannelV2 c1e = new EncryptedChannelV2(tunnel.channel1(), sharedKey, EncryptedChannelV2.Role.CLIENT);
        EncryptedChannelV2 c2e = new EncryptedChannelV2(tunnel.channel2(), sharedKey, EncryptedChannelV2.Role.SERVER);
        
        ApplicationChannel c1a = new ApplicationChannel(c1e, TimeKeeper.NULL, TimeChecker.NULL);
        ApplicationChannel c2a = new ApplicationChannel(c2e, TimeKeeper.NULL, TimeChecker.NULL);
        
        c1a.write(true, new byte[]{0x10});
        
        Assert.assertEquals("lastFlag1", false, c2a.lastFlag());
        c2a.read();
        Assert.assertEquals("lastFlag2", true, c2a.lastFlag());
    }
    
    @Test
    public void testLastFlag2() {
        // Note, the lastFlag is implemented in EncryptedChannelV2, so it must
        // be included when testing this.
        
        Tunnel tunnel = new Tunnel();
        byte[] sharedKey = new byte[SaltLib.crypto_box_SHAREDKEYBYTES];
        sharedKey[9] = 0x09;
        EncryptedChannelV2 c1e = new EncryptedChannelV2(tunnel.channel1(), sharedKey, EncryptedChannelV2.Role.CLIENT);
        EncryptedChannelV2 c2e = new EncryptedChannelV2(tunnel.channel2(), sharedKey, EncryptedChannelV2.Role.SERVER);
        
        ApplicationChannel c1a = new ApplicationChannel(c1e, TimeKeeper.NULL, TimeChecker.NULL);
        ApplicationChannel c2a = new ApplicationChannel(c2e, TimeKeeper.NULL, TimeChecker.NULL);
        
        c1a.write(false, new byte[]{0x10});
        c1a.write(true, new byte[]{0x20}, new byte[]{0x30});
        
        c2a.read();
        Assert.assertEquals("lastFlag1", false, c2a.lastFlag());
        c2a.read();
        Assert.assertEquals("lastFlag2", true, c2a.lastFlag());
        c2a.read();
        Assert.assertEquals("lastFlag3", true, c2a.lastFlag());
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
        ApplicationChannel c1 = new ApplicationChannel(tunnel.channel1(), timeKeeper, TimeChecker.NULL);
        ApplicationChannel c2 = new ApplicationChannel(tunnel.channel2(), TimeKeeper.NULL, timeChecker);
        
        c1.write(false, new byte[]{0x10});
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
        ApplicationChannel c1 = new ApplicationChannel(tunnel.channel1(), timeKeeper, TimeChecker.NULL);
        ApplicationChannel c2 = new ApplicationChannel(tunnel.channel2(), TimeKeeper.NULL, timeChecker);
        
        c1.write(false, new byte[]{0x10}, new byte[]{0x20, 0x21});
        byte[] message1 = c2.read();
        byte[] message2 = c2.read();
        Assert.assertArrayEquals(new byte[]{0x10}, message1);
        Assert.assertArrayEquals(new byte[]{0x20, 0x21}, message2);
    }
}
