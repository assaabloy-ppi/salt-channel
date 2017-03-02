package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

/**
 * Inner ticket data; the data that is encrypted in field Ticket/Encrypted.
 */
public class TicketEncryptedData {    
    public static final int PACKET_TYPE = 6;
    public byte[] sessionKey;    // shared symmetric session key
    public byte[] clientSigKey;  // client's public signature key
    public long ticketIndex;
    public int keyIndex;
    
    public int getSize() {
        return PacketHeader.SIZE + 2 + 8 + 32 + 32;
    }
    
    public byte[] toBytes() {
        if (sessionKey == null || sessionKey.length != 32) {
            throw new IllegalStateException("bad sessionKey value");
        }
        
        if (clientSigKey == null || clientSigKey.length != 32) {
            throw new IllegalStateException("bad clientSigKey value");
        }
        
        byte[] result = new byte[getSize()];
        
        Serializer s = new Serializer(result, 0);
        PacketHeader header = new PacketHeader();
        
        s.writeHeader(header);
        s.writeUint16(keyIndex);
        s.writeInt64(ticketIndex);
        s.writeBytes(clientSigKey);
        s.writeBytes(sessionKey);
        
        if (s.getOffset() != getSize()) {
            throw new IllegalStateException("offset mismatch, " + s.getOffset());
        }
        
        return result;
    }
    
    public static TicketEncryptedData fromBytes(byte[] bytes, int offset) {
        TicketEncryptedData data = new TicketEncryptedData();
        Deserializer d = new Deserializer(bytes, 0);
        PacketHeader header = d.readHeader();
        if (header.getType() != PACKET_TYPE) {
            throw new BadPeer("unexpected packet type, " + header.getType());
        }
        
        data.ticketIndex = d.readInt64();
        data.clientSigKey = d.readBytes(32);
        data.sessionKey = d.readBytes(32);
        
        return data;
    }
}
