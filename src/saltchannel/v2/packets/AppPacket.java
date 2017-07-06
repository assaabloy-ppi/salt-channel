package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

public class AppPacket implements Packet {
    public static final int PACKET_TYPE = 5;
    public int time;
    public byte[] appData;
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    public int getSize() {
        return PacketHeader.SIZE + 4 + appData.length;
    }
    
    public void toBytes(byte[] destination, int offset) {
        if (appData == null) {
            throw new IllegalStateException("appData is null");
        }
        
        Serializer s = new Serializer(destination, offset);
        PacketHeader header = new PacketHeader(PACKET_TYPE);
        s.writeHeader(header);
        s.writeInt32(time);
        s.writeBytes(appData);
    }
    
    public static AppPacket fromBytes(byte[] source, int offset, int packetSize) {
        if (packetSize < PacketHeader.SIZE + 4) {
            throw new BadPeer("packet too small, was " + packetSize);
        }
        
        AppPacket p = new AppPacket();
        Deserializer d = new Deserializer(source, offset);
        
        PacketHeader header = d.readHeader();
        int packetType = header.getType();
        if (packetType != PACKET_TYPE) {
            throw new BadPeer("unexpected packet type, " + packetType + ", expected " + PACKET_TYPE);
        }
        
        p.time = d.readInt32();
        if (p.time < 0) {
            throw new BadPeer("bad time, " + p.time);
        }
        
        int dataSize = packetSize - (PacketHeader.SIZE + 4);
        if (dataSize < 0) {
            throw new BadPeer("bad dataSize");
        }
        
        p.appData = d.readBytes(dataSize);
        
        return p;
    }
}
