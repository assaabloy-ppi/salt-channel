package saltchannel.v2;

/**
 * A bit field in a circular buffer of fixed size.
 * 
 * @author Frans Lundberg
 */
public class BigBitField {
    private long firstIndex;
    private long lastIndex;    // range is [firstIndex, lastIndex)
    private final BitField[] fields;
    private final int bitsPerField;
    private final int bitCapacity;
    
    public BigBitField(long firstIndex, int wantedBitSize) {
        this.firstIndex = firstIndex;
        if (wantedBitSize < 4) {
            throw new IllegalArgumentException("bad param wantedBitSize");
        }
        
        this.lastIndex = firstIndex;
        
        int n = 4;
        fields = new BitField[n];
        bitsPerField = wantedBitSize / n;
        bitCapacity = fields.length * bitsPerField;
        
        for (int i = 0; i < n; i++) {
            fields[i] = new BitField(bitsPerField);
        }
    }
    
    public int getBitCapacity() {
        return bitCapacity;
    }
    
    public synchronized boolean get(long index) {
        if (index < 0) {
            throw new IllegalArgumentException("negative index not allowed");
        }
        
        if (index < firstIndex || index >= lastIndex) {
            return false;
        }
        
        long diff = index - firstIndex;
        int fieldIndex = (int) (diff / bitsPerField);
        int bitIndex = (int) (diff % bitsPerField);
        
        return fields[fieldIndex].get(bitIndex);
    }
    
    public synchronized void set(long index, boolean value) {
        if (index < 0) {
            throw new IllegalArgumentException("negative index not allowed");
        }
        
        if (index < firstIndex || index >= lastIndex) {
            throw new IllegalArgumentException("index out of range");
        }
        
        long diff = index - firstIndex;
        int fieldIndex = (int) (diff / bitsPerField);
        int bitIndex = (int) (diff % bitsPerField);
        
        fields[fieldIndex].set(bitIndex, value);
    }
    
    /**
     * Adds a bit that is set to true. Returns its index.
     */
    public synchronized long addTrueBit() {
        // TODO implement new BitField "roll" here when needed.
        
        long index = lastIndex;
        set(index, true);
        lastIndex++;
        
        return index;
    }
}
