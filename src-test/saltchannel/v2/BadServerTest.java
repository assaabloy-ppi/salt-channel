package saltchannel.v2;

import org.junit.Assert;
import org.junit.Test;
import saltchannel.BadPeer;
import saltchannel.ByteChannel;
import saltchannel.dev.ManipulableByteChannel;
import saltchannel.dev.ManipulableByteChannel.Manipulation;
import saltchannel.testutil.Env1;

/**
 * Tests sessions with bad data from the server.
 * The client should get a BadPeer exception when such a condition
 * occurs.
 * 
 * @author Frans Lundberg
 */
public class BadServerTest {
    
    @Test
    public void testTheSetup() {
        // Just testing the test setup.
        
        Env1 env = new Env1();
        ManipulableByteChannel m = new ManipulableByteChannel(env.ch2);
        env.ch2 = m;
        
        env.start();
        
        ByteChannel ac = env.appChannel;
        
        ac.write(false, new byte[]{1, 2, 3});
        byte[] response = ac.read();
        
        Assert.assertArrayEquals(new byte[]{1, 2, 3}, response);
    }
    
    @Test(expected=BadPeer.class)
    public void testBadAppResponse() {
        Env1 env = new Env1();
        
        ManipulableByteChannel m = new ManipulableByteChannel(env.ch2);
        m.addManipulation(5, new Manipulation() {
            public byte[] manipulate(int packetIndex, byte[] originalBytes) {
                byte[] res = originalBytes.clone();
                res[0] = (byte) (res[0] + 1);
                return res;
            }
        });
        env.ch2 = m;
        
        env.start();
        
        env.appChannel.write(false, new byte[]{1, 2, 3});
        env.appChannel.read();
    }
    
    
    public void testBadServerEncKeyInM2() {
        Env1 env = new Env1();
        
        ManipulableByteChannel m = new ManipulableByteChannel(env.ch2);
        m.addManipulation(1, new Manipulation() {
            public byte[] manipulate(int packetIndex, byte[] originalBytes) {
                byte[] res = originalBytes.clone();
                res[res.length-1] = (byte) (res[res.length-1] + 1);
                    // destroys last byte of M2/ServerEncKey
                return res;
            }
        });
        env.ch2 = m;
        
        env.start();
        
        env.appChannel.write(false, new byte[]{1, 2, 3});
        env.appChannel.read();
    }
    
    @Test(expected=BadPeer.class)
    public void testBadPacketTypeInM2() {
        Env1 env = new Env1();
        
        ManipulableByteChannel m = new ManipulableByteChannel(env.ch2);
        m.addManipulation(1, new Manipulation() {
            public byte[] manipulate(int packetIndex, byte[] originalBytes) {
                byte[] res = originalBytes.clone();
                res[0] = 99;    // bad packet type
                return res;
            }
        });
        env.ch2 = m;
        
        env.start();
        
        env.appChannel.write(false, new byte[]{1, 2, 3});
        env.appChannel.read();
    }
}
