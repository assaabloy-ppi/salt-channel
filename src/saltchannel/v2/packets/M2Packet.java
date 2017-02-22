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
    public static final int PACKET_TYPE = 2;
    public boolean noSuchServer;
    public byte[] serverEncKey;
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    public int getSize() {
        return 1 + 32;
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
        
        s.writeUint4(PACKET_TYPE);
        s.writeBit(noSuchServer);
        s.writeBit(0);
        s.writeBit(0);
        s.writeBit(0);
        
        s.writeBytes(serverEncKey);
    }
    
    public static M2Packet fromBytes(byte[] source, int offset) {
        M2Packet p = new M2Packet();
        Deserializer d = new Deserializer(source, offset);
        
        int packetType = d.readUint4();
        if (packetType != PACKET_TYPE) {
            throw new BadPeer("unexpected packet type, " + packetType);
        }
        
        p.noSuchServer = d.readBit();
        d.readBit();
        d.readBit();
        d.readBit();
        
        p.serverEncKey = d.readBytes(32);
        
        return p;
    }
}
