package saltchannel.a1a2;

import saltchannel.ByteChannel;

/**
 * The client-side of one A1-A2 session.
 * 
 * @author Frans Lundberg
 */
public class A1Client {
    private ByteChannel channel;
    private A1Packet a1;
    
    public A1Client(ByteChannel channel) {
        this.channel = channel;
        this.a1 = new A1Packet();
    }
    
    /**
     * Allows the consumer to modify A1 before it is sent.
     */
    public A1Packet getA1() {
        return a1;
    }
    
    public A2Packet go() {
        byte[] buffer = new byte[a1.getSize()];
        a1.toBytes(buffer, 0);
        channel.write(false, buffer);
        
        byte[] response = channel.read();
        A2Packet a2 = A2Packet.fromBytes(response, 0);
        return a2;
    }
}
