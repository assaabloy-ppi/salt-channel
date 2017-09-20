package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

public class TTPacket {
    public static final int PACKET_TYPE = Packet.TYPE_TT;
    public static final int SESSION_NONCE_SIZE = 8;
    public int time;
    public byte[] sessionNonce;  // session nonce for session to be created with the ticket
    public byte[] ticket;        // ticket as issued by server
    
    public TTPacket() {
    }
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    public int getSize() {
        return PacketHeader.SIZE 
                + 4 
                + (ticketIncluded() ? (TicketPacket.SESSION_NONCE_SIZE + 1 + ticket.length) : 0);
    }
    
    public boolean ticketIncluded() {
        return ticket != null;
    }
    
    public void toBytes(byte[] destination, int offset) {
        Serializer s = new Serializer(destination, offset);
        PacketHeader header = new PacketHeader(PACKET_TYPE);
        header.setBit(0, ticketIncluded());
        
        s.writeHeader(header);
        s.writeInt32(time);
        
        if (ticketIncluded()) {
            if (ticket.length > 127) {
                throw new IllegalStateException("bad ticket size, " + ticket.length);
            }
            
            s.writeBytes(sessionNonce);
            s.writeByte((byte) ticket.length);
            s.writeBytes(ticket);
        }
    }
    
    public byte[] toBytes() {
        byte[] result = new byte[getSize()];
        toBytes(result, 0);
        return result;
    }
    
    public static TTPacket fromBytes(byte[] source, int offset) {
        TTPacket p = new TTPacket();
        Deserializer d = new Deserializer(source, 0);
        
        PacketHeader header = d.readHeader();
        if (header.getType() != PACKET_TYPE) {
            throw new BadPeer("unexpected packet type, " + header.getType());
        }
        
        boolean ticketIncluded = header.getBit(0);
        
        p.time = d.readInt32();
        if (p.time < 0) {
            throw new BadPeer("bad time, " + p.time);
        }
        
        if (ticketIncluded) {
            p.sessionNonce = d.readBytes(TicketPacket.SESSION_NONCE_SIZE);
            
            byte ticketSize = d.readByte();
            if (ticketSize > 127) {
                throw new BadPeer("bad TicketSize, " + ticketSize);
            }
            
            if (ticketSize < 4) {
                throw new BadPeer("bad TicketSize, " + ticketSize);
            }
            
            p.ticket = d.readBytes(ticketSize);
        }
        
        return p;
    }
}
