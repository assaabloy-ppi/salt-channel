package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

/**
 * Data of M2 message, low-level serialization / deserialization.
 * 
 * @author Frans Lundberg
 */
public class M2Packet {
    public boolean resumeSupported;
    public boolean noSuchServer;
    public boolean badTicket;
    public byte[] serverEncKey;
    
    public boolean hasServerEncKey() {
        return serverEncKey != null;
    }
    
    public int getSize() {
        return 1 + 
               (hasServerEncKey() ? 32 : 0);
    }
    
    public byte[] toBytes() {
        if (serverEncKey != null && serverEncKey.length != 32) {
            throw new IllegalStateException("bad serverEncKey length");
        }
        
        byte[] result = new byte[getSize()];
        Serializer s = new Serializer(result, 0);
        
        s.writeUint4(2);   // packet type 2
        s.writeBit(hasServerEncKey());
        s.writeBit(resumeSupported);
        s.writeBit(noSuchServer);
        s.writeBit(badTicket);
        
        s.writeBytes(serverEncKey);
        
        return result;
    }
    
    public static M2Packet fromBytes(byte[] bytes) {
        M2Packet p = new M2Packet();
        Deserializer d = new Deserializer(bytes, 0);
        
        int packetType = d.readUint4();
        if (packetType != 2) {
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
