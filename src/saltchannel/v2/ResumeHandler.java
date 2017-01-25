package saltchannel.v2;

import saltchannel.util.Bytes;

/**
 * Handles the resume-feature on the server-side.
 * Stores a bit map to avoid replay attacks.
 * All data is stored in memory. First implementation is very simple.
 * 
 * @author Frans Lundberg
 */
public class ResumeHandler {
    private final BigBitField bits;
    
    /**
     * Creates a new instance with a given wanted capacity.
     * The capacity is the maximum number of resume tickets that can
     * be handled. The memory consumption is wantedSize / 8 bytes 
     * plus overhead.
     */
    public ResumeHandler(long firstTicketIndex, int wantedSize) {
        if (wantedSize < 0) {
            throw new IllegalArgumentException("negative wantedSize not allowed");
        }
        
        this.bits = new BigBitField(firstTicketIndex, wantedSize);
    }
    
    /**
     * Returns true if the ticket with the given index is valid.
     * If is considered valid of the bit is set to 1.
     */
    public boolean isValid(long ticketIndex) {
        return bits.get(ticketIndex);
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
        
        if (!isValid(ticketIndex)) {
            throw new BadTicket("ticket index not valid");
        }
        
        // TODO decrypt, check it, clear bit, return session info.
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
