package saltchannel.util;

/**
 * Low-level, generic serializer. Serializes data (integers etc) to a 
 * given byte array.
 * 
 * @author Frans Lundberg
 */
public class Serializer {
    private byte[] buffer;
    private int firstOffset;
    private int offset;
    private int bitOffset;
    
    private static final int[] BITS = {
            1, 2, 4, 8, 16, 32, 64, 128
    };
    
    private static final int POW16 = 65536;
    
    public Serializer(byte[] buffer, int offset) {
        this.buffer = buffer;
        this.firstOffset = offset;
        this.offset = offset;
        this.bitOffset = 0;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public int getFirstOffset() {
        return firstOffset;
    }
    
    public Serializer writeByte(int value) {
        if (value < 0 || value >= 255) {
            throw new IllegalArgumentException("out of range");
        }
        return writeByte((byte) value);
    }
    
    public Serializer writeByte(byte value) {
        checkBitOffsetZero();
        buffer[offset] = value;
        offset++;
        return this;
    }
    
    public Serializer writeBytes(byte[] bytes) {
        checkBitOffsetZero();
        System.arraycopy(bytes, 0, this.buffer, offset, bytes.length);
        return this;
    }
    
    /**
     * Writes a 4-bit unsigned integer, range 0-15, to the first 4 bits of the 
     * current byte.
     */
    public Serializer writeUint4(int value) {
        checkBitOffsetZero();
        
        if (value < 0 || value > 16) {
            throw new IllegalArgumentException("out of bounds, " + value);
        }
        
        buffer[offset] = (byte) value;
        bitOffset += 4;
        
        return this;
    }
    
    /**
     * @param value
     *          Value must be 0 or 1.
     */
    public Serializer writeBit(int value) {
        if (value != 0 && value != 1) {
            throw new IllegalArgumentException("illegal value, " + value);
        }
        
        if (value == 0) {
            return writeBit(false);
        } else {
            return writeBit(true);
        }
    }
    
    public Serializer writeBit(boolean b) {
        int value = Bytes.unsigned(buffer[offset]);
        int mask = BITS[bitOffset];
        
        if (b == true) {
            value = value | mask;
        } else {
            value = value & ~mask;
        }
        
        buffer[offset] = (byte) value;
        
        incrementBitOffset();
        
        return this;
    }
    
    public Serializer writeUint16(int value) {
        checkBitOffsetZero();
        
        if (value < 0 || value >= POW16) {
            throw new IllegalArgumentException("out of range");
        }
        
        Bytes.ushortToBytesLE(value, buffer, offset);
        offset += 2;
        
        return this;
    }
    
    public Serializer writeInt64(long value) {
        checkBitOffsetZero();
        
        Bytes.longToBytesLE(value, buffer, offset);
        offset += 8;
        
        return this;
    }
    
    private void checkBitOffsetZero() {
        if (bitOffset != 0) {
            throw new IllegalStateException("bitOffset not zero, " + bitOffset);
        }
    }
    
    private void incrementBitOffset() {
        bitOffset += 1;
        if (bitOffset == 8) {
            bitOffset = 0;
            offset += 1;
        }
    }
}
