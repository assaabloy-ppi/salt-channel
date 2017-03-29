package saltchannel.v2.packets;

import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

public class M3Packet implements Packet {
    public static final int PACKET_TYPE = 3;
    public int time;
    public byte[] serverSigKey;
    public byte[] signature1;
    public byte[] ticket;
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    public int getSize() {
        return PacketHeader.SIZE + 4 
                + 32 
                + 64
                + (ticketIncluded() ? (1 + ticket.length) : 0);
    }
    
    public boolean ticketIncluded() {
        return ticket != null;
    }
    
    public void toBytes(byte[] destination, int offset) {
        if (signature1 == null || signature1.length != 64) {
            throw new IllegalStateException("bad signature1");
        }
        
        if (this.serverSigKey == null) {
            throw new IllegalStateException("serverSigKey is null");
        }
        
        if (serverSigKey.length != 32) {
            throw new IllegalStateException("bad serverSigKey size");
        }
        
        Serializer s = new Serializer(destination, offset);
        PacketHeader header = new PacketHeader(PACKET_TYPE);
        header.setBit(0, ticketIncluded());
        
        s.writeHeader(header);
        s.writeInt32(time);
        s.writeBytes(serverSigKey);
        s.writeBytes(signature1);
        
        if (ticketIncluded()) {
            if (ticket.length > 127) {
                throw new IllegalStateException("bad ticket size, " + ticket.length);
            }
            
            s.writeByte((byte) ticket.length);
            s.writeBytes(ticket);
        }
    }
    
    public byte[] toBytes() {
        byte[] result = new byte[getSize()];
        toBytes(result, 0);
        return result;
    }
    
    public static M3Packet fromBytes(byte[] source, int offset) {
        M3Packet p = new M3Packet();
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
        
        p.serverSigKey = d.readBytes(32);
        p.signature1 = d.readBytes(64);
        
        if (ticketIncluded) {
            byte ticketSize = d.readByte();
            if (ticketSize > 127) {
                throw new BadPeer("bad TicketSize, " + ticketSize);
            }
            
            p.ticket = d.readBytes(ticketSize);
        }
        
        return p;
    }
}
