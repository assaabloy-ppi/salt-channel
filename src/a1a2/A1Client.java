package a1a2;

import saltchannel.ByteChannel;

/**
 * The client-side of one A1-A2 session.
 * 
 * @author Frans Lundberg
 */
public class A1Client {
    private ByteChannel channel;
    
    public A1Client(ByteChannel channel) {
        this.channel = channel;
    }
    
    public A2Packet go() {
        A1Packet a1 = new A1Packet();
        byte[] buffer = new byte[a1.getSize()];
        a1.toBytes(buffer, 0);
        channel.write(buffer);
        
        byte[] response = channel.read();
        A2Packet a2 = A2Packet.fromBytes(response, 0);
        return a2;
    }
}
