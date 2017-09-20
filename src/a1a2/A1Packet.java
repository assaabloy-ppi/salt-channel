package a1a2;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;
import saltchannel.v2.packets.Packet;
import saltchannel.v2.packets.PacketHeader;

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
        return PacketHeader.SIZE;
    }
    
    public void toBytes(byte[] destination, int offset) {
        Serializer s = new Serializer(destination, offset);
        PacketHeader header = new PacketHeader(PACKET_TYPE);
        s.writeHeader(header);
    }
    
    public static A1Packet fromBytes(byte[] source, int offset) {
        A1Packet p = new A1Packet();
        Deserializer d = new Deserializer(source, offset);
        PacketHeader header = d.readHeader();
        
        int packetType = header.getType();
        if (packetType != PACKET_TYPE) {
            throw new BadPeer("unexpected packet type, " + packetType);
        }
        
        return p;
    }
}
