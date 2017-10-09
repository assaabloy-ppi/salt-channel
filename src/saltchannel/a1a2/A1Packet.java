package saltchannel.a1a2;

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
    public static final byte ADDRESS_TYPE_ANY = 0;
    public static final byte ADDRESS_TYPE_PUBKEY = 1;
    public static final int MAX_ADDRESS_LENGTH = 65535;
    public int addressType;
    public byte[] address;
    
    public A1Packet() {
        address = new byte[0];
        addressType = ADDRESS_TYPE_ANY;
    }
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    public int getSize() {
        return PacketHeader.SIZE + 1 + 2 + address.length;
    }
    
    public void toBytes(byte[] destination, int offset) {
        Serializer s = new Serializer(destination, offset);
        PacketHeader header = new PacketHeader(PACKET_TYPE);
        s.writeHeader(header);
        
        if (address.length > MAX_ADDRESS_LENGTH) {
            throw new IllegalStateException("address too long, " + address.length);
        }
        
        s.writeByte(addressType);
        s.writeUint16(address.length);
        s.writeBytes(address);
    }
    
    public static A1Packet fromBytes(byte[] source, int offset) {
        A1Packet p = new A1Packet();
        Deserializer d = new Deserializer(source, offset);
        PacketHeader header = d.readHeader();
        
        int packetType = header.getType();
        if (packetType != PACKET_TYPE) {
            throw new BadPeer("unexpected packet type, " + packetType);
        }
        
        p.addressType = d.readByte();
        
        int addressSize = d.readUint16();
        if (addressSize < 0) {
            throw new BadPeer("unexpected addressSize, " + addressSize);
        }
        
        p.address = new byte[addressSize];
        d.readBytes(addressSize);
        
        return p;
    }
}
