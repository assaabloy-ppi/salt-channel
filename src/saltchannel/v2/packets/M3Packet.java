package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

public class M3Packet {
    public byte[] serverSigKey;
    public byte[] signature1;
    
    public int getSize() {
        return 1 + 
               (hasServerSigKey() ? 32 : 0) +
               64;
    }
    
    public boolean hasServerSigKey() {
        return serverSigKey != null;
    }
    
    public byte[] toBytes() {
        if (signature1 == null || signature1.length != 64) {
            throw new IllegalStateException("bad signature1");
        }
        
        if (hasServerSigKey() && serverSigKey.length != 32) {
            throw new IllegalStateException("bad serverSigKey size");
        }
        
        byte[] result = new byte[getSize()];
        Serializer s = new Serializer(result, 0);
        
        s.writeUint4(3);    // packet type == 3
        s.writeBit(hasServerSigKey());
        s.writeBit(0);
        s.writeBit(0);
        s.writeBit(0);
        
        if (hasServerSigKey()) {
            s.writeBytes(serverSigKey);
        }
        
        s.writeBytes(signature1);
        
        return result;
    }
    
    public static M3Packet fromBytes(byte[] bytes) {
        M3Packet p = new M3Packet();
        Deserializer d = new Deserializer(bytes, 0);
        
        int packetType = d.readUint4();
        if (packetType != 3) {
            throw new BadPeer("unexpected packet type, " + packetType);
        }
        
        boolean serverSigIncluded = d.readBit();
        d.readBit();
        d.readBit();
        d.readBit();
        
        if (serverSigIncluded) {
            p.serverSigKey = d.readBytes(32);
        }
        
        p.signature1 = d.readBytes(64);
        
        return p;
    }
}
