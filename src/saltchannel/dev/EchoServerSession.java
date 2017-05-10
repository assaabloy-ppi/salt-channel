package saltchannel.dev;

import saltchannel.BadPeer;
import saltchannel.ByteChannel;

/**
 * Very simple echo server implemented on top of a ByteChannel.
 * Useful for testing.
 * Uses a subset of echo protocol as defined in 
 * repo:pot-main/echo-protocol/echo-server-protocol.md.
 * The 4-byte message size is omitted, however.
 * Send a message to the server that starts with byte value 1 
 * followed by a number of bytes. The whole messages will be echoed back.
 * 
 * @author Frans Lundberg
 */
public class EchoServerSession implements ByteChannelServerSession {
    
    /**
     * Runs the server forever or until an exception is thrown.
     */
    public void runSession(ByteChannel channel) {
        while (true) {
            byte[] data = channel.read();
            
            if (data.length < 1) {
                throw new BadPeer("message too short");
            }
            
            int commandType = data[0];
            if (commandType != 1) {
                throw new BadPeer("unsupported command type, " + commandType);
            }
            
            channel.write(data);
        }
    }
}
