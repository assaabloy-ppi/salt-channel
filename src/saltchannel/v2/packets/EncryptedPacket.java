package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

public class EncryptedPacket implements Packet {
    public static final int PACKET_TYPE = 6;
    public boolean closeFlag;
    public byte[] body;
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    public int getSize() {
        return 1 + 32 + 64;
    }

    public void toBytes(byte[] destination, int offset) {
        if (body == null || body.length < 16) {
            throw new IllegalStateException("bad body");
        }
        
        Serializer s = new Serializer(destination, offset);
        
        s.writeUint4(PACKET_TYPE);     // packet type == 6
        s.writeBit(closeFlag);
        s.writeBit(0);
        s.writeBit(0);
        s.writeBit(0);
    }
    
    public static EncryptedPacket fromBytes(byte[] source, int offset) {
        EncryptedPacket p = new EncryptedPacket();
        Deserializer d = new Deserializer(source, offset);
        
        int packetType = d.readUint4();
        if (packetType != PACKET_TYPE) {
            throw new BadPeer("unexpected packet type");
        }
        
        p.closeFlag = d.readBit();
        d.readBit();
        d.readBit();
        d.readBit();
        
        return p;
    }
}
