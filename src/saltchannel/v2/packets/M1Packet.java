package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

/**
 * Data of the M1 message, low-level serialization/deserialization.
 * 
 * @author Frans Lundberg
 */
public class M1Packet implements Packet {
    public static final int PACKET_TYPE = 1;
    
    public byte[] clientEncKey;
    public byte[] serverSigKey;
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    /**
     * Returns the total byte size.
     */
    public int getSize() {
        return 4 + 1 + 32
                + (hasServerSigKey() ? 32 : 0);
    }
    
    public boolean hasServerSigKey() {
        return serverSigKey != null;
    }
    
    public void toBytes(byte[] destination, int offset) {
        Serializer s = new Serializer(destination, offset);
        
        s.writeString("SCv2");    // ProtocolIndicator
        s.writeHeader(PACKET_TYPE, hasServerSigKey(), false, false, false);
        s.writeBytes(clientEncKey);
        
        assert s.getOffset() == getSize() : "unexpected offset, " + s.getOffset();
        
        if (hasServerSigKey()) {
            s.writeBytes(serverSigKey);
        }
        
        if (s.getOffset() != getSize()) {
            throw new IllegalStateException("unexpected, " + s.getOffset());
        }
    }
    
    public byte[] toBytes() {
        byte[] result = new byte[getSize()];
        toBytes(result, 0);
        return result;
    }
    
    public static M1Packet fromBytes(byte[] source, int offset) {
        M1Packet data = new M1Packet();
        
        Deserializer d = new Deserializer(source, offset);        
        
        String protocol = d.readString(4);
        if (!"SCv2".equals(protocol)) {
            throw new BadPeer("unexpected ProtocolIndicator, " + protocol);
        }

        int messageType = d.readUint4();
        if (messageType != 1) {
            throw new BadPeer("bad message type, " + messageType);
        }
        
        boolean serverSigKeyIncluded = d.readBit();
        d.readBit();
        d.readBit();
        d.readBit();
        
        data.clientEncKey = d.readBytes(32);
        
        if (serverSigKeyIncluded) {
            data.serverSigKey = d.readBytes(32);
        }
        
        return data;
    }
}
