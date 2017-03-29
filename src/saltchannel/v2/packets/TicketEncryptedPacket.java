package saltchannel.v2.packets;

import saltchannel.util.Deserializer;
import saltchannel.util.Serializer;

/**
 * Inner ticket data; the data that is encrypted in the field Ticket/Encrypted.
 */
public class TicketEncryptedPacket {
    public byte ticketType;
    public long ticketId;
    public byte[] sessionNonce;  // the "session nonce" to use in the ticket session
    public byte[] sessionKey;    // shared symmetric session key
    public byte[] clientSigKey;  // client's public signature key
    
    public TicketEncryptedPacket() {
        ticketType = TicketPacket.TICKET_TYPE_1;
    }
    
    public int getSize() {
        return 1 + 1 + 8 + 8 + 32 + 32;
    }
    
    public byte[] toBytes() {
        if (sessionKey == null || sessionKey.length != 32) {
            throw new IllegalStateException("bad sessionKey value");
        }
        
        if (sessionNonce == null || sessionNonce.length != TTPacket.SESSION_NONCE_SIZE) {
            throw new IllegalStateException("bad sessionNonce value");
        }
        
        if (clientSigKey == null) {
            throw new IllegalStateException("clientSigKey must not be null");
        }
        
        if (clientSigKey.length != 32) {
            throw new IllegalStateException("bad clientSigKey length, " + clientSigKey.length);
        }
        
        byte[] result = new byte[getSize()];
        
        Serializer s = new Serializer(result, 0);
        s.writeByte(ticketType);
        s.writeByte(0);
        s.writeInt64(ticketId);
        s.writeBytes(sessionNonce);
        s.writeBytes(sessionKey);
        s.writeBytes(clientSigKey);
        
        if (s.getOffset() != getSize()) {
            throw new IllegalStateException("offset mismatch, " + s.getOffset());
        }
        
        return result;
    }
    
    /**
     * @throws BadTicket
     */
    public static TicketEncryptedPacket fromBytes(byte[] bytes, int offset) {
        TicketEncryptedPacket p = new TicketEncryptedPacket();
        Deserializer d = new Deserializer(bytes, 0);
        
        p.ticketType = d.readByte();
        
        if (p.ticketType != TicketPacket.TICKET_TYPE_1) {
            throw new BadTicket("unsupported ticket type, " + p.ticketType);
        }
        
        byte zero = d.readByte();
        if (zero != 0) {
            throw new BadTicket("expected zero, got " + zero);
        }
        
        p.ticketId = d.readInt64();
        p.sessionNonce = d.readBytes(TTPacket.SESSION_NONCE_SIZE);
        p.sessionKey = d.readBytes(32);
        p.clientSigKey = d.readBytes(32);
        
        return p;
    }
}
