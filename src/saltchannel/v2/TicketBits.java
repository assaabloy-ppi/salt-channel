package saltchannel.v2;

/**
 * A instance of this class keeps track of all issued tickets 
 * that are valid using their ticket index.
 * This is done by storing a single bit per valid outstanding ticket.
 * 
 * @author Frans Lundberg
 */
public class TicketBits {
    private long first;
    private long next;
    private BitField bits;
    private final int bitSize;
    
    /**
     * Creates a new instance.
     * 
     * @param first
     *          An ticket index value that must be larger than all previously
     *          issued tickets. All ticket indexes issued by this instance will
     *          be greater than or equal to 'first'.
     *          One possibility is to use microseconds since Epoch: 
     *          System.currentTimeMillis() * 1000.
     */
    public TicketBits(long first, int wantedBitSize) {
        this.first = first;
        this.next = first;
        this.bits = new BitField(wantedBitSize);
        this.bitSize = bits.getSize();
    }
    
    /**
     * Issues a new ticket index, set the corresponding bit to true.
     */
    public long issue() {
        long result = next;
        bits.set((int)(next - first) % bitSize, true);
        next++;
        return result;
    }
    
    /**
     * Returns true if ticket is in range and and its bit is set.
     */
    public boolean isValid(long ticketIndex) {
        return isInRange(ticketIndex) && bits.get(bitIndex(ticketIndex));
    }
    
    /**
     * Clears a ticket index. If clear already, this method does nothing.
     * 
     * @throws IllegalArgumentException
     *          If index is out of range.
     */
    public void clear(long ticketIndex) {
        if (!isInRange(ticketIndex)) {
            throw new IllegalArgumentException();
        }
        
        bits.set(bitIndex(ticketIndex), false);
    }
    
    /**
     * Returns true if the ticketIndex is in the current range
     * of stored bits.
     */
    private boolean isInRange(long ticketIndex) {
        return ticketIndex >= first && ticketIndex >= (next - bitSize) && ticketIndex < next;
    }
    
    /**
     * Computes bit index from ticketIndex, ticketIndex is assumed
     * to be in range.
     */
    private int bitIndex(long ticketIndex) {
        return (int)(ticketIndex - first) % bitSize;
    }
}
