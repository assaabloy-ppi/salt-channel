package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

/**
 * Data of the M1 message, low-level serialization/deserialization.
 * 
 * @author Frans Lundberg
 */
public class M1Message implements Packet {
    public static final int PACKET_TYPE = 1;
    public static final int BIT_INDEX_SERVER_SIG_KEY_INCLUDED = 0;
    public static final int BIT_INDEX_TICKET_INCLUDED = 1;
    public static final int BIT_INDEX_TICKET_REQUESTED = 2;
    
    public int time;
    public byte[] clientEncKey;
    public byte[] serverSigKey;
    public byte[] ticket;
    public boolean ticketRequested;
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    /**
     * Returns the total byte size.
     */
    public int getSize() {
        return 4 + PacketHeader.SIZE + 4
                + 32
                + (serverSigKeyIncluded() ? 32 : 0)
                + (ticketIncluded() ? (1 + ticket.length) : 0);
    }
    
    public boolean serverSigKeyIncluded() {
        return serverSigKey != null;
    }
    
    public boolean ticketIncluded() {
        return ticket != null;
    }
    
    public void toBytes(byte[] destination, int offset) {
        Serializer s = new Serializer(destination, offset);
        PacketHeader header = new PacketHeader(PACKET_TYPE);
        header.setBit(BIT_INDEX_SERVER_SIG_KEY_INCLUDED, serverSigKeyIncluded());
        header.setBit(BIT_INDEX_TICKET_INCLUDED, ticketIncluded());
        header.setBit(BIT_INDEX_TICKET_REQUESTED, ticketRequested);
        
        s.writeString("SCv2");    // ProtocolIndicator
        s.writeHeader(header);
        s.writeInt32(time);
        s.writeBytes(clientEncKey);
        
        if (serverSigKeyIncluded()) {
            s.writeBytes(serverSigKey);
        }
        
        if (ticketIncluded()) {
            if (ticket.length > 127) {
                throw new IllegalStateException("bad ticket length, " + ticket.length);
            }
            
            s.writeByte((byte) ticket.length);
            s.writeBytes(ticket);
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
    
    public static M1Message fromBytes(byte[] source, int offset) {
        M1Message data = new M1Message();
        
        Deserializer d = new Deserializer(source, offset);        
        
        String protocol = d.readString(4);
        if (!"SCv2".equals(protocol)) {
            throw new BadPeer("unexpected ProtocolIndicator, " + protocol);
        }
        
        PacketHeader header = d.readHeader();
        if (header.getType() != PACKET_TYPE) {
            throw new BadPeer("bad message type, " + header.getType());
        }
        
        data.time = d.readInt32();
        if (data.time < 0) {
            throw new BadPeer("bad time, negative, " + data.time);
        }
        
        boolean serverSigKeyIncluded = header.getBit(BIT_INDEX_SERVER_SIG_KEY_INCLUDED);
        boolean ticketIncluded = header.getBit(BIT_INDEX_TICKET_INCLUDED);
        data.ticketRequested = header.getBit(BIT_INDEX_TICKET_REQUESTED);
        
        data.clientEncKey = d.readBytes(32);
        
        if (serverSigKeyIncluded) {
            data.serverSigKey = d.readBytes(32);
        }
        
        if (ticketIncluded) {
            byte ticketSize = d.readByte();
            if (ticketSize < 0 || ticketSize > 127) {
                throw new BadPeer("bad ticketSize, " + ticketSize);
            }
            
            data.ticket = d.readBytes(ticketSize);
        }
        
        return data;
    }
}
