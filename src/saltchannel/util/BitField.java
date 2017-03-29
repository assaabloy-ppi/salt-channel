package saltchannel.util;

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
        checkIndex(index);
        return (field[longIndex(index)] & mask(index)) != 0;
    }
    
    public void set(int index, boolean value) {
        checkIndex(index);
        int longIndex = longIndex(index);
        long mask = mask(index);
        
        if (value == true) {
            field[longIndex] = field[longIndex] | mask;
        } else {
            field[longIndex] = field[longIndex] & ~mask;
        }
    }
    
    private final void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index);
        }
    }
    
    private final int longIndex(int index) {
        return index / 64;
    }
    
    private final long mask(int index) {
        int bitIndex = index - longIndex(index) * 64;
        return 1 << bitIndex;
    }
}
