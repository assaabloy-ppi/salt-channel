package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

/**
 * Data of the M1 message, low-level serialization/deserialization.
 * 
 * @author Frans Lundberg
 */
public class M1Packet implements Packet {
    public static final int PACKET_TYPE = 1;
    
    public int time;
    public byte[] clientEncKey;
    public byte[] serverSigKey;
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    /**
     * Returns the total byte size.
     */
    public int getSize() {
        return 4 + PacketHeader.SIZE + 4
                + 32
                + (hasServerSigKey() ? 32 : 0);
    }
    
    public boolean hasServerSigKey() {
        return serverSigKey != null;
    }
    
    public void toBytes(byte[] destination, int offset) {
        Serializer s = new Serializer(destination, offset);
        PacketHeader header = new PacketHeader(PACKET_TYPE);
        
        s.writeString("SCv2");    // ProtocolIndicator
        s.writeHeader(header);
        s.writeInt32(time);
        s.writeBytes(clientEncKey);
        
        assert s.getOffset() == getSize() : "unexpected offset, " + s.getOffset();
        
        if (hasServerSigKey()) {
            s.writeBytes(serverSigKey);
        }
        
        if (s.getOffset() != getSize()) {
            throw new IllegalStateException("unexpected, " + s.getOffset());
        }
    }
    
    public byte[] toBytes() {
        byte[] result = new byte[getSize()];
        toBytes(result, 0);
        return result;
    }
    
    public static M1Packet fromBytes(byte[] source, int offset) {
        M1Packet data = new M1Packet();
        
        Deserializer d = new Deserializer(source, offset);        
        
        String protocol = d.readString(4);
        if (!"SCv2".equals(protocol)) {
            throw new BadPeer("unexpected ProtocolIndicator, " + protocol);
        }
        
        PacketHeader header = d.readHeader();
        if (header.getType() != PACKET_TYPE) {
            throw new BadPeer("bad message type, " + header.getType());
        }
        
        data.time = d.readInt32();
        if (data.time < 0) {
            throw new BadPeer("bad time, negative, " + data.time);
        }
        
        boolean serverSigKeyIncluded = header.getBit(0);
        
        data.clientEncKey = d.readBytes(32);
        
        if (serverSigKeyIncluded) {
            data.serverSigKey = d.readBytes(32);
        }
        
        return data;
    }
}
