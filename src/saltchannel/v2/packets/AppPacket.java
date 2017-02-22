package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

public class AppPacket implements Packet {
    public static final int PACKET_TYPE = 5;
    public byte[] appData;
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    public int getSize() {
        return 1 + appData.length;
    }
    
    public void toBytes(byte[] destination, int offset) {
        if (appData == null) {
            throw new IllegalStateException("appData is null");
        }
        
        Serializer s = new Serializer(destination, offset);
        
        s.writeUint4(PACKET_TYPE);
        s.writeBit(0);
        s.writeBit(0);
        s.writeBit(0);
        s.writeBit(0);
        
        s.writeBytes(appData);
    }
    
    public static AppPacket fromBytes(byte[] source, int offset, int packetSize) {
        AppPacket p = new AppPacket();
        Deserializer d = new Deserializer(source, offset);
        
        int packetType = d.readUint4();
        if (packetType != PACKET_TYPE) {
            throw new BadPeer("unexpected packet type");
        }
        
        d.readBit();
        d.readBit();
        d.readBit();
        d.readBit();
        
        int dataSize = packetSize - 1;
        if (dataSize < 0) {
            throw new BadPeer("bad dataSize");
        }
        
        p.appData = d.readBytes(dataSize);
        
        return p;
    }
}
