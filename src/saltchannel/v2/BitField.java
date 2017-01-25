package saltchannel.v2;

/**
 * Simple bit field implementation, fixed size.
 * 
 * @author Frans Lundberg
 */
public class BitField {
    private long[] field;
    private final int size;
    
    public BitField(int size) {
        this.size = size;
        int longCount = size / 64;
        if (size % 64 != 0) {
            longCount += 1;
        }
        
        field = new long[longCount];
    }

    public int getSize() {
        return size;
    }
    
    public boolean get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index);
        }
        
        int longIndex = index / 64;
        int bitIndex = index - longIndex * 64;
        long mask = 1 << bitIndex;
        
        return (field[longIndex] & mask) != 0;
    }
    
    public synchronized boolean getAndClear(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index);
        }
        
        int longIndex = index / 64;
        int bitIndex = index - longIndex * 64;
        long mask = 1 << bitIndex;
        
        boolean result = (field[longIndex] & mask) != 0;
        if (result) {
            field[longIndex] = field[longIndex] & ~mask;
        }
        return result;
    }
    
    public void set(int index, boolean value) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index);
        }
        
        int longIndex = index / 64;
        int bitIndex = index - longIndex * 64;
        long mask = 1 << bitIndex;
        
        if (value == true) {
            field[longIndex] = field[longIndex] | mask;
        } else {
            field[longIndex] = field[longIndex] & ~mask;
        }
    }
}
