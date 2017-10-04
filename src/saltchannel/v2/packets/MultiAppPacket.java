package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

/**
 * MultiAppPacket as introduced in spec-salt-channel-v2-draft5.
 * 
 * @author Frans Lundberg
 */
public class MultiAppPacket implements Packet {
    public static final int PACKET_TYPE = 11;
    public int time;
    public byte[][] appMessages;
    public static final int MAX_SIZE = 65535;
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    public int getSize() {
        int size = PacketHeader.SIZE + 4 + 2;
        for (int i = 0; i < appMessages.length; i++) {
            size += (2 + appMessages[i].length);
        }
        return size;
    }
    
    public void toBytes(byte[] destination, int offset) {
        if (appMessages == null) {
            throw new IllegalStateException("appMessages is null");
        }
        
        if (appMessages.length > MAX_SIZE) {
            throw new IllegalStateException("appMessages.length too large, " + appMessages.length);
        }
        
        Serializer s = new Serializer(destination, offset);
        PacketHeader header = new PacketHeader(PACKET_TYPE);
        s.writeHeader(header);
        s.writeInt32(time);
        s.writeUint16(appMessages.length);
        for (int i = 0; i < appMessages.length; i++) {
            byte[] appMessage = appMessages[i];
            if (appMessage.length > MAX_SIZE) {
                throw new IllegalStateException("appMessage, " + i + ", too large");
            }
            
            s.writeUint16(appMessage.length);
            s.writeBytes(appMessage);
        }
    }
    
    public static MultiAppPacket fromBytes(byte[] source, int offset, int packetSize) {
        if (packetSize < PacketHeader.SIZE + 4 + 2) {
            throw new BadPeer("packet too small, was " + packetSize);
        }
        
        MultiAppPacket p = new MultiAppPacket();
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
        
        int count = readUint16(d);
        
        if (count < 1) {
            throw new BadPeer("count in MultiAppPacket is " + count);
        }
        
        p.appMessages = new byte[count][];
        for (int i = 0; i < count; i++) {
            int size = readUint16(d);
            
            if (size < 0 || size > MAX_SIZE) {
                throw new BadPeer("bad size in MultiAppPacket, " + size);
            }
            
            p.appMessages[i] = new byte[size];
            p.appMessages[i] = d.readBytes(size);
        }
        
        return p;
    }
    
    /**
     * Returns true iff the given array of application messages
     * should be encoded in a MultiAppPacket.
     */
    public static boolean shouldUse(byte[][] appMessages) {
        if (appMessages.length < 2) {
            return false;
        }
        
        for (int i = 0; i < appMessages.length; i++) {
            if (appMessages[i].length > MAX_SIZE) {
                return false;
            }
        }
        
        return true;
    }
    
    private static int readUint16(Deserializer d) {
        try {
            return d.readUint16();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new BadPeer("bad data in MultiAppPacket");
        }
    }
}
