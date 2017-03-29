package saltchannel.v2.packets;

import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

public class TicketPacket  implements Packet {
    public static final int PACKET_TYPE = Packet.TYPE_TICKET;
    public static final int ENCRYPTED_NONCE_SIZE = 10;
    public static final int TICKET_TYPE_1 = 1;
    public static final int SESSION_NONCE_SIZE = 8;
    public byte ticketType;
    public byte[] encryptedTicketNonce;
    public byte[] encrypted;
    
    public TicketPacket() {
    }
    
    public int getType() {
        return PACKET_TYPE;
    }
    
    /**
     * Returns the total byte size.
     */
    public int getSize() {
        return PacketHeader.SIZE
                + 2
                + ENCRYPTED_NONCE_SIZE
                + encrypted.length;
    }
    
    public void toBytes(byte[] destination, int offset) {
        Serializer s = new Serializer(destination, offset);
        PacketHeader header = new PacketHeader(PACKET_TYPE);
        s.writeHeader(header);
        
        if (encryptedTicketNonce == null || encryptedTicketNonce.length != ENCRYPTED_NONCE_SIZE) {
            throw new IllegalStateException("bad nonce, " + encryptedTicketNonce.length);
        }
        
        if (encrypted == null) {
            throw new IllegalStateException("bad encrypted field");
        }
        
        if (getSize() > 127) {
            throw new IllegalStateException("ticket would be too big, " + getSize());
        }
        
        s.writeByte(ticketType);
        s.writeByte(0);
        s.writeBytes(encryptedTicketNonce);
        s.writeBytes(encrypted);
        
        if (s.getOffset() != getSize()) {
            throw new IllegalStateException("unexpected, " + s.getOffset());
        }
    }
    
    public byte[] toBytes() {
        byte[] result = new byte[getSize()];
        toBytes(result, 0);
        return result;
    }
    
    public static TicketPacket fromBytes(byte[] source) {
        TicketPacket p = new TicketPacket();
        
        Deserializer d = new Deserializer(source, 0);
        
        PacketHeader header = d.readHeader();
        if (header.getType() != PACKET_TYPE) {
            throw new BadTicket("bad message type, " + header.getType());
        }
        
        p.ticketType = d.readByte();
        if (p.ticketType != TicketPacket.TICKET_TYPE_1) {
            throw new BadTicket("unknown ticket type, " + p.ticketType);
        }
        
        byte zero = d.readByte();
        if (zero != 0) {
            throw new BadTicket("expected zero, got " + zero);
        }
        
        p.encryptedTicketNonce = d.readBytes(ENCRYPTED_NONCE_SIZE);
        
        int left = source.length - (PacketHeader.SIZE + 2 + ENCRYPTED_NONCE_SIZE);
        p.encrypted = d.readBytes(left);
        
        return p;
    }
}
