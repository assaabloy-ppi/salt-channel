package saltchannel.v2;

/**
 * A instance of this class keeps track of all issued ticket indexes
 * that are valid.
 * 
 * @author Frans Lundberg
 */
public class TicketIndexes {
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
    public TicketIndexes(long first, int wantedBitSize) {
        this.first = first;
        this.next = first;
        this.bits = new BitField(wantedBitSize);
        this.bitSize = bits.getSize();
    }
    
    /**
     * Issues a new ticket index, set the corresponding bit to true.
     */
    public long next() {
        long result = next;
        bits.set((int)(next - first) % bitSize, true);
        next++;
        return result;
    }
    
    /**
     * Checks whether the ticketIndex is valid, if so, 
     * it is cleared.
     * 
     * @param index
     *          The ticket index.
     * @return true 
     *          if the ticketIndex is valid
     */
    public boolean checkIsValidAndClear(long index) {
        boolean isValid = false;
        
        if (index >= (next - bitSize) && index < next) {
            int bitIndex = (int)(index - first) % bitSize;
            isValid = bits.get(bitSize);
            if (isValid) {
                bits.set(bitIndex, false);
            }
        }
        
        return isValid;
    }
}
