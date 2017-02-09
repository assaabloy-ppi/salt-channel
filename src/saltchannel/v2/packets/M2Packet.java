package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

/**
 * Data of M2 message, low-level serialization / deserialization.
 * 
 * @author Frans Lundberg
 */
public class M2Packet implements Packet {
    public boolean resumeSupported;
    public boolean noSuchServer;
    public boolean badTicket;
    public byte[] serverEncKey;
    
    public static final int PACKET_TYPE = 2;
    
    public boolean hasServerEncKey() {
        return serverEncKey != null;
    }
    
    public int getSize() {
        return 1 + 
               (hasServerEncKey() ? 32 : 0);
    }
    
    public void toBytes(byte[] destination, int offset) {
        if (serverEncKey != null && serverEncKey.length != 32) {
            throw new IllegalStateException("bad serverEncKey length");
        }
        
        Serializer s = new Serializer(destination, offset);
        
        s.writeUint4(PACKET_TYPE);
        s.writeBit(hasServerEncKey());
        s.writeBit(resumeSupported);
        s.writeBit(noSuchServer);
        s.writeBit(badTicket);
        
        s.writeBytes(serverEncKey);
    }
    
    public static M2Packet fromBytes(byte[] source, int offset) {
        M2Packet p = new M2Packet();
        Deserializer d = new Deserializer(source, offset);
        
        int packetType = d.readUint4();
        if (packetType != PACKET_TYPE) {
            throw new BadPeer("unexpected packet type, " + packetType);
        }
        
        boolean serverEncKeyIncluded = d.readBit();
        p.resumeSupported = d.readBit();
        p.noSuchServer = d.readBit();
        p.badTicket = d.readBit();
        
        if (serverEncKeyIncluded) {
            p.serverEncKey = d.readBytes(32);
        }
        
        return p;
    }
}
