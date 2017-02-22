package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

/**
 * Data of A1 message, low-level serialization / deserialization.
 * 
 * @author Frans Lundberg
 */
public class A1Packet implements Packet {
    public static final int PACKET_TYPE = 8;
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    public int getSize() {
        return 1;
    }
    
    public void toBytes(byte[] destination, int offset) {
        Serializer s = new Serializer(destination, offset);
        s.writeUint4(PACKET_TYPE);
        s.writeBit(1);    // close == true
        s.writeBit(0);
        s.writeBit(0);
        s.writeBit(0);
    }
    
    public static A1Packet fromBytes(byte[] source, int offset) {
        A1Packet p = new A1Packet();
        Deserializer d = new Deserializer(source, offset);
        
        int packetType = d.readUint4();
        if (packetType != PACKET_TYPE) {
            throw new BadPeer("unexpected packet type, " + packetType);
        }
        
        boolean close = d.readBit();
        d.readBit();
        d.readBit();
        d.readBit();
        
        if (!close) {
            throw new BadPeer("close flag must be set");
        }
        
        return p;
    }
}
