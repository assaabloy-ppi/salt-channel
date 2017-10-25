package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

public class EncryptedMessage implements Packet {
    public static final int PACKET_TYPE = 6;
    public byte[] body;
    public boolean lastFlag = false;
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    public int getSize() {
        return PacketHeader.SIZE + body.length;
    }
    
    public boolean lastFlag() {
        return lastFlag;
    }

    public void toBytes(byte[] destination, int offset) {
        if (body == null || body.length < 16) {
            throw new IllegalStateException("bad body");
        }
        
        Serializer s = new Serializer(destination, offset);
        PacketHeader header = new PacketHeader(PACKET_TYPE);
        header.setLastFlag(lastFlag);
        s.writeHeader(header);
        s.writeBytes(body);
    }
    
    public static EncryptedMessage fromBytes(byte[] source, int offset, int messageSize) {
        EncryptedMessage p = new EncryptedMessage();
        Deserializer d = new Deserializer(source, offset);
        
        PacketHeader header = d.readHeader();
        if (header.getType() != PACKET_TYPE) {
            throw new BadPeer("unexpected packet type, " + header.getType());
        }
        
        int size = messageSize - PacketHeader.SIZE;
        p.body = d.readBytes(size);
        p.lastFlag = header.lastFlag();
        
        return p;
    }
}
