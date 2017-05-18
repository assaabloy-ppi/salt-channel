package saltchannel.dev;

import org.junit.Assert;
import org.junit.Test;
import saltchannel.ByteChannel;
import saltchannel.Tunnel;
import saltchannel.dev.ManipulableByteChannel.Manipulation;

public class ManipulableByteChannelTest {

    @Test
    public void testSanity() {
        Tunnel tunnel = new Tunnel();
        ByteChannel ch1 = tunnel.channel1();
        ByteChannel ch2 = tunnel.channel2();
        
        ManipulableByteChannel m = new ManipulableByteChannel(ch1);
        
        m.addManipulation(0, new Manipulation() {
            public byte[] manipulate(int packetIndex, byte[] originalBytes) {
                byte[] result = originalBytes.clone();
                result[0] = (byte) (originalBytes[0] + 1);
                return result;
            }
        });
        
        m.write(new byte[]{4, 4, 4, 4});
        byte[] bytes = ch2.read();
        
        Assert.assertArrayEquals(new byte[]{5, 4, 4, 4}, bytes);
    }
}
