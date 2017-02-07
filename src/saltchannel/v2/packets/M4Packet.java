package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

/**
 * Low-level M3 packet data, serialization/deserialization.
 * 
 * @author Frans Lundberg
 */
public class M4Packet {
    public byte[] clientSigKey;
    public byte[] signature2;
    
    public int getSize() {
        return 1 + 32 + 64;
    }
    
    public byte[] toBytes() {
        if (clientSigKey == null || clientSigKey.length != 32) {
            throw new IllegalStateException("bad clientSigKey");
        }
        
        if (signature2 == null || signature2.length != 64) {
            throw new IllegalStateException("bad signature2");
        }
        
        byte[] result = new byte[getSize()];
        Serializer s = new Serializer(result, 0);
        
        s.writeUint4(4);     // packet type == 4
        s.writeBit(0);
        s.writeBit(0);
        s.writeBit(0);
        s.writeBit(0);
        
        s.writeBytes(clientSigKey);
        s.writeBytes(signature2);
        
        return result;
    }
    
    public static M4Packet fromBytes(byte[] bytes) {
        M4Packet p = new M4Packet();
        Deserializer d = new Deserializer(bytes, 0);
        
        int packetType = d.readUint4();
        if (packetType != 4) {
            throw new BadPeer("unexpected packet type");
        }
        
        d.readBit();
        d.readBit();
        d.readBit();
        d.readBit();
        
        p.clientSigKey = d.readBytes(32);
        p.signature2 = d.readBytes(64);
        
        return p;
    }
}
