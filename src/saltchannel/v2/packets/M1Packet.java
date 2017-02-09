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
    public boolean ticketRequested = false;
    public byte[] clientEncKey;
    public byte[] serverSigKey;
    public byte[] ticket;
    
    public byte[] getTicket() {
        return ticket;
    }
    
    public boolean hasServerSigKey() {
        return serverSigKey != null;
    }
    
    public boolean hasTicket() {
        return ticket != null;
    }
    
    /**
     * Returns the total byte size.
     */
    public int getSize() {
        return 1 + 2 + 32
                + (hasServerSigKey() ? 32 : 0)
                + (hasTicket() ? (1 + ticket.length) : 0);
    }
    
    public void toBytes(byte[] destination, int offset) {
        byte[] result = new byte[getSize()];
        
        Serializer s = new Serializer(destination, offset);
        
        s.writeUint4(1);    // message type is 1
        s.writeBit(hasServerSigKey());
        s.writeBit(hasTicket());
        s.writeBit(ticketRequested);
        s.writeBit(0);
        
        s.writeByte('S').writeByte('2');
        
        s.writeBytes(clientEncKey);
        
        assert s.getOffset() == 3 + 32 : "unexpected offset, " + s.getOffset();
        
        if (hasServerSigKey()) {
            s.writeBytes(serverSigKey);
        }
        
        if (hasTicket()) {
            if (ticket.length > 127 || ticket.length < 1) {
                throw new IllegalStateException("bad ticket length, " + ticket.length);
            }
            
            s.writeByte(ticket.length);
            s.writeBytes(ticket);
        }
        
        if (s.getOffset() != result.length) {
            throw new IllegalStateException("unexpected, " + s.getOffset() + ", " + result.length);
        }
    }
    
    public static M1Packet fromBytes(byte[] source, int offset) {
        M1Packet data = new M1Packet();
        
        Deserializer d = new Deserializer(source, offset);
        
        int messageType = d.readUint4();
        if (messageType != 1) {
            throw new BadPeer("bad message type, " + messageType);
        }
        
        boolean serverSigKeyIncluded = d.readBit();
        boolean ticketIncluded = d.readBit();
        data.ticketRequested = d.readBit();
        d.readBit();
        
        int b0 = d.readUnsignedByte();
        int b1 = d.readUnsignedByte();
        
        if (!(b0 == 'S' && b1 == '2')) {
            throw new BadPeer("unexpected ProtocolIndicator");
        }
        
        data.clientEncKey = d.readBytes(32);
        
        if (serverSigKeyIncluded) {
            data.serverSigKey = d.readBytes(32);
        }
        
        if (ticketIncluded) {
            int ticketSize = d.readUnsignedByte();
            if (ticketSize < 1 || ticketSize > 255) {
                throw new BadPeer("bad TicketSize, " + ticketSize);
            }
            
            data.ticket = d.readBytes(ticketSize);
        }
        
        return data;
    }
}
