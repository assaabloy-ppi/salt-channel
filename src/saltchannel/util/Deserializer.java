package saltchannel.util;

import java.io.UnsupportedEncodingException;

import saltchannel.v2.packets.PacketHeader;

/**
 * A deserializer that can deserialize (read) bytes created with 
 * Serializer.
 * 
 * @author Frans Lundberg
 */
public class Deserializer {
    private byte[] buffer;
    private int firstOffset;
    private int offset;
    private int bitOffset;

    private static final int[] BITS = {
            1, 2, 4, 8, 16, 32, 64, 128
    };
    
    private static final int INT4_MASK = 1 + 2 + 4 + 8;
    
    public Deserializer(byte[] buffer, int offset) {
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
    
    public byte readByte() {
        checkBitOffsetZero();
        byte result = buffer[offset];
        offset++;
        return result;
    }
    
    public int readUnsignedByte() {
        checkBitOffsetZero();
        byte b = buffer[offset];
        offset++;
        return Bytes.unsigned(b);
    }
    
    public byte[] readBytes(int size) {
        checkBitOffsetZero();
        if (size < 0) {
            throw new IllegalArgumentException("bad size");
        }
        
        if (size > buffer.length) {
            throw new IllegalArgumentException("size too large");
        }
        
        byte[] result = new byte[size];
        System.arraycopy(buffer, offset, result, 0, size);
        offset += size;
        
        return result;
    }
    
    /**
     * Reads a UTF-8 string of known byte size.
     */
    public String readString(int byteSize) {
        byte[] bytes = readBytes(byteSize);
        
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error("should not happen", e);
        }
    }
    
    public boolean readBit() {
        int value = Bytes.unsigned(buffer[offset]);
        int mask = BITS[bitOffset];
        incrementBitOffset();
        return (value & mask) != 0;
    }
    
    public int readBitAsInt() {
        return readBit() ? 1 : 0;
    }
    
    public PacketHeader readHeader() {
        PacketHeader header = new PacketHeader();
        System.arraycopy(buffer, offset, header.bytes(), 0, PacketHeader.SIZE);
        offset += PacketHeader.SIZE;
        return header;
    }
    
    /**
     * Reads a 4-bit unsigned integer, range 0-15, from the first 4 bits of the 
     * current byte.
     */
    public int readUint4() {
        checkBitOffsetZero();
        
        int result = Bytes.unsigned(buffer[offset]) & INT4_MASK;
        bitOffset += 4;
        return result;
    }
    
    public int readUint16() {
        int result = Bytes.bytesToUShortLE(buffer, offset);
        offset += 2;
        return result;
    }
    
    public int readInt32() {
        int result = Bytes.bytesToIntLE(buffer, offset);
        offset += 4;
        return result;
    }
    
    public long readInt64() {
        long result = Bytes.bytesToLongLE(buffer, offset);
        offset += 8;
        return result;
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
