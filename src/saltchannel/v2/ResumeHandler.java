package saltchannel.v2;

import saltchannel.util.Bytes;

//
// IDEA. Have step value. For example, increase ticketIndex by 1000 each time a 
// ticket is issued. If last three digits are random this could provide some security 
// against problem with clock set wrong so time repeats according to the host clock. 
// For time-based firstTicketIndex, we cannot increase faster than time increases. 
// If we assume max 1000 issued tickets per second, we can use a step size of 1000 
// when time is measured in microseconds.
//

/**
 * Handles the resume-feature on the server-side.
 * Stores a bit map to avoid replay attacks.
 * All data is stored in memory. First implementation is very simple.
 * 
 * @author Frans Lundberg
 */
public class ResumeHandler {
    public static final int KEY_SIZE = 16;
    private final TicketBits ticketIndexes;
    private byte[] key;
    
    /**
     * Creates a new instance with a given wanted capacity.
     * The capacity is the maximum number of resume tickets that can
     * be handled. The memory consumption is wantedSize / 8 bytes 
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
    
    public synchronized void issueTicket() {
        // TODO A. implement issueTicket().
    }

    /**
     * Checks ticket, decrypts, returns crypto session data.
     * 
     * @throws BadTicket if the ticket is not valid.
     */
    public synchronized TicketSessionData checkTicket(ResumeTicket resumeTicket) {
        byte[] encrypted = resumeTicket.encryptedTicket;
        byte[] hostData = resumeTicket.hostData;
        
        int firstTwoBytes = Bytes.unsigned(hostData[0]) + 256 * Bytes.unsigned(hostData[1]);
        if (firstTwoBytes != 0) {
            throw new BadTicket("bad first two bytes of hostData");
        }
        
        long ticketIndex = Bytes.bytesToLongLE(hostData, 2);
        
        
        
        
        boolean isValid = ticketIndexes.isValid(ticketIndex);
        
        
        return null;
    }
    
    public static class BadTicket extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        public BadTicket(String message) {
            super(message);
        }
    }
    
    public static class TicketSessionData {
        public byte[] sessionKey;
        public byte[] clientSigKey;
    }
}
