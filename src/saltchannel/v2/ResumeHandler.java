package saltchannel.v2;

import java.util.Arrays;

import saltchannel.CryptoLib;
import saltchannel.util.Bytes;
import saltchannel.v2.packets.TicketEncryptedPacket;
import saltchannel.v2.packets.TicketPacket;

//
// IDEA. Have step value. For example, increase ticketIndex by 1000 each time a 
// ticket is issued. If last three digits are random this could provide some security 
// against problem with clock set wrong so time repeats according to the host clock. 
// For time-based firstTicketIndex, we cannot increase faster than time increases. 
// If we assume max 1000 issued tickets per second, we can use a step size of 1000 
// when time is measured in microseconds.
//

/** 
 * Handles the resume feature on the server-side.
 * Stores a bit map to avoid replay attacks.
 * All data is stored in memory. First simple implementation.
 * 
 * @author Frans Lundberg
 */
public class ResumeHandler {
    public static final int KEY_SIZE = 32;
    private final TicketBits ticketIndexes;
    private byte[] key;
    
    /**
     * Creates a new instance with a given wanted capacity.
     * The capacity is the maximum number of resume tickets that can
     * be handled. The memory consumption is wantedSize/8 bytes 
     * plus overhead.
     */
    public ResumeHandler(byte[] key, long firstTicketIndex, int wantedBitSize) {
        if (wantedBitSize < 0) {
            throw new IllegalArgumentException("negative wantedSize not allowed");
        }
        
        this.ticketIndexes = new TicketBits(firstTicketIndex, wantedBitSize);
        
        if (key.length != KEY_SIZE) {
            throw new IllegalArgumentException("bad key size");
        }
        
        this.key = key.clone();
    }
    
    public synchronized byte[] issueTicket(byte[] clientSigKey, byte[] sessionKey) {
        TicketEncryptedPacket p1 = new TicketEncryptedPacket();
        p1.ticketId = ticketIndexes.issue();
        p1.sessionKey = sessionKey;
        p1.clientSigKey = clientSigKey;
        
        byte[] nonce24 = createNonce24(p1.ticketId);
        byte[] encrypted = CryptoLib.encrypt(key, nonce24, p1.toBytes());
        
        TicketPacket p2 = new TicketPacket();
        p2.ticketType = TicketPacket.TICKET_TYPE_1;
        p2.nonce = to10Bytes(nonce24);
        p2.encrypted = encrypted;
        
        byte[] totalBytes = p2.toBytes();
        
        return totalBytes;
    }

    /**
     * Validates ticket, decrypts, returns crypto session data.
     * 
     * @throws InvalidTicket if the ticket is not valid.
     */
    public synchronized TicketSessionData validateTicket(byte[] ticket) {
        throw new IllegalStateException("NOT implemented");    // TODO D. implement checkTicket
    }
    
    public static class InvalidTicket extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        public InvalidTicket(String message) {
            super(message);
        }
    }
    
    /**
     * Creates 24-byte long nonce. Only the first 10 bytes are used (are non-zero).
     */
    private byte[] createNonce24(long ticketId) {
        byte[] result = new byte[24];
        result[0] = 120;
        result[1] = 121;
        Bytes.longToBytesLE(ticketId, result, 2);
        return result;
    }
    
    private byte[] to10Bytes(byte[] nonce24) {
        return Arrays.copyOfRange(nonce24, 0, 10);
    }
}
