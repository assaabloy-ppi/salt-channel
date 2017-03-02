package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

public class EncryptedPacket implements Packet {
    public static final int PACKET_TYPE = 6;
    public byte[] body;
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    public int getSize() {
        return PacketHeader.SIZE + body.length;
    }

    public void toBytes(byte[] destination, int offset) {
        if (body == null || body.length < 16) {
            throw new IllegalStateException("bad body");
        }
        
        Serializer s = new Serializer(destination, offset);
        PacketHeader header = new PacketHeader(PACKET_TYPE);
        
        s.writeHeader(header);
        s.writeBytes(body);
    }
    
    public static EncryptedPacket fromBytes(byte[] source, int offset, int messageSize) {
        EncryptedPacket p = new EncryptedPacket();
        Deserializer d = new Deserializer(source, offset);
        
        PacketHeader header = d.readHeader();
        if (header.getType() != PACKET_TYPE) {
            throw new BadPeer("unexpected packet type, " + header.getType());
        }
        
        int size = messageSize - PacketHeader.SIZE;
        p.body = d.readBytes(size);
        
        return p;
    }
}
