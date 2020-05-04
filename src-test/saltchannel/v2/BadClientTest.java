package saltchannel.v2;

import org.junit.Assert;
import org.junit.Test;
import saltchannel.BadPeer;
import saltchannel.ByteChannel;
import saltchannel.dev.ManipulableByteChannel;
import saltchannel.dev.ManipulableByteChannel.Manipulation;
import saltchannel.testutil.Env2;
import saltchannel.v2.packets.M1Message;

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
    
    @Test
    public void testBadFirstTimeFromClient() {
	// Client sends time = 3 in m1 message. Not allowed. Must be 0 or 1.
	// Server uses ch2.
	
	Env2 env = new Env2();
        
        ManipulableByteChannel m = new ManipulableByteChannel(env.ch2);
        m.addManipulation(0, new Manipulation() {
            public byte[] manipulate(int packetIndex, byte[] originalBytes) {
        	M1Message m1 = M1Message.fromBytes(originalBytes, 0);
                m1.time = 1234;
                return m1.toBytes();
            }
        });
        env.ch2 = m;
        
        try {
            env.start();
        } catch (BadPeer e) {
            String errorMessage = e.getMessage();
            if (!errorMessage.contains("1234")) {
        	throw new RuntimeException("Unexpected error message, was: " + errorMessage);
            }
        }
    }
}
