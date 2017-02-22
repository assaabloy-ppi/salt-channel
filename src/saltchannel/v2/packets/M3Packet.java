package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

public class M3Packet implements Packet {
    public static final int PACKET_TYPE = 3;
    public byte[] serverSigKey;
    public byte[] signature1;
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    public int getSize() {
        return 1 + 32 + 64;
    }
    
    public void toBytes(byte[] destination, int offset) {
        if (signature1 == null || signature1.length != 64) {
            throw new IllegalStateException("bad signature1");
        }
        
        if (this.serverSigKey == null) {
            throw new IllegalStateException("serverSigKey is null");
        }
        
        if (serverSigKey.length != 32) {
            throw new IllegalStateException("bad serverSigKey size");
        }
        
        Serializer s = new Serializer(destination, offset);
        
        s.writeUint4(3);    // packet type == 3
        s.writeBit(0);
        s.writeBit(0);
        s.writeBit(0);
        s.writeBit(0);
        
        s.writeBytes(serverSigKey);
        
        s.writeBytes(signature1);
    }
    
    public byte[] toBytes() {
        byte[] result = new byte[getSize()];
        toBytes(result, 0);
        return result;
    }
    
    public static M3Packet fromBytes(byte[] source, int offset) {
        M3Packet p = new M3Packet();
        Deserializer d = new Deserializer(source, 0);
        
        int packetType = d.readUint4();
        if (packetType != 3) {
            throw new BadPeer("unexpected packet type, " + packetType);
        }
        
        d.readBit();
        d.readBit();
        d.readBit();
        d.readBit();
        
        p.serverSigKey = d.readBytes(32);
        
        p.signature1 = d.readBytes(64);
        
        return p;
    }
}
