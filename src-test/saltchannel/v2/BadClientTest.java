package saltchannel.v2;

import org.junit.Assert;
import org.junit.Test;
import saltchannel.BadPeer;
import saltchannel.ByteChannel;
import saltchannel.dev.ManipulableByteChannel;
import saltchannel.dev.ManipulableByteChannel.Manipulation;
import saltchannel.testutil.Env2;

/**
 * Tests sessions with bad data from the client.
 * The server should get a BadPeer exception when such a condition occurs.
 * 
 * @author Frans Lundberg
 */
public class BadClientTest {

    @Test
    public void testTheSetup() {
        // Just testing the test setup.
        
        Env2 env = new Env2();
        ManipulableByteChannel m = new ManipulableByteChannel(env.ch1);
        env.ch1 = m;
        
        env.start();
        
        ByteChannel ac = env.serverAppChannel;
        byte[] request = ac.read();
        
        Assert.assertArrayEquals(new byte[]{1, 5, 5, 5, 5, 5}, request);
    }
    
    @Test(expected=BadPeer.class)
    public void testBadProtocolIndicatorInM1() {
        Env2 env = new Env2();
        
        ManipulableByteChannel m = new ManipulableByteChannel(env.ch1);
        m.addManipulation(0, new Manipulation() {
            public byte[] manipulate(int packetIndex, byte[] originalBytes) {
                byte[] res = originalBytes.clone();
                res[0] = (byte) 'Z';
                return res;
            }
        });
        env.ch1 = m;
        
        env.start();
    }
}
