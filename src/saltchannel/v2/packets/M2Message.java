package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

/**
 * Data of M2 message, low-level serialization / deserialization.
 * 
 * @author Frans Lundberg
 */
public class M2Message implements Packet {
    public static final int PACKET_TYPE = 2;
    public boolean noSuchServer;
    public int time;
    public byte[] serverEncKey;
    public boolean resumeSupported;
    public boolean lastFlag;
    
    public M2Message() {
        resumeSupported = false;
    }
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    public int getSize() {
        return PacketHeader.SIZE + 4 + 32;
    }
    
    public boolean hasServerEncKey() {
        return serverEncKey != null;
    }
    
    public void toBytes(byte[] destination, int offset) {
        if (serverEncKey == null) {
            throw new IllegalStateException("serverEncKey not set");
        }
        
        if (serverEncKey.length != 32) {
            throw new IllegalStateException("bad serverEncKey length");
        }
        
        Serializer s = new Serializer(destination, offset);
        PacketHeader header = new PacketHeader(PACKET_TYPE);
        header.setBit(0, noSuchServer);
        header.setBit(1, resumeSupported);
        header.setBit(7, lastFlag);
        
        s.writeHeader(header);
        s.writeInt32(time);
        s.writeBytes(serverEncKey);
    }
    
    public byte[] toBytes() {
        byte[] result = new byte[getSize()];
        toBytes(result, 0);
        return result;
    }
    
    public static M2Message fromBytes(byte[] source, int offset) {
        M2Message p = new M2Message();
        Deserializer d = new Deserializer(source, offset);
        
        PacketHeader header = d.readHeader();
        int packetType = header.getType();
        if (packetType != PACKET_TYPE) {
            throw new BadPeer("unexpected packet type, " + packetType + ", expected " + PACKET_TYPE);
        }
        
        p.noSuchServer = header.getBit(0);
        p.resumeSupported = header.getBit(1);
        p.lastFlag = header.getBit(7);
        
        p.time = d.readInt32();
        if (p.time < 0) {
            throw new BadPeer("bad time, " + p.time);
        }
        
        p.serverEncKey = d.readBytes(32);
        
        return p;
    }
}
