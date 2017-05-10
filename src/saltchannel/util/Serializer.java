package saltchannel.util;

import java.io.UnsupportedEncodingException;
import saltchannel.v2.packets.PacketHeader;

/**
 * Low-level, serializer. Serializes data (integers etc) to a given byte array.
 * 
 * @author Frans Lundberg
 */
public class Serializer {
    private byte[] buffer;
    private int firstOffset;
    private int offset;
    private static final int POW16 = 65536;
    
    public Serializer(byte[] buffer, int offset) {
        this.buffer = buffer;
        this.firstOffset = offset;
        this.offset = offset;
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
    
    /**
     * Writes a string as UTF-8.
     */
    public Serializer writeString(String s) {
        byte[] ascii;
        
        try {
            ascii = s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error("should not happen", e);
        }
        
        return writeBytes(ascii);
    }
    
    public Serializer writeByte(byte value) {
        buffer[offset] = value;
        offset++;
        return this;
    }
    
    public Serializer writeBytes(byte[] bytes) {
        System.arraycopy(bytes, 0, this.buffer, offset, bytes.length);
        offset += bytes.length;
        return this;
    }
    
    public Serializer writeHeader(PacketHeader header) {
        int size = PacketHeader.SIZE;
        System.arraycopy(header.bytes(), 0, this.buffer, offset, size);
        offset += size;
        return this;
    }
    
    public Serializer writeUint16(int value) {
        if (value < 0 || value >= POW16) {
            throw new IllegalArgumentException("out of range");
        }
        
        Bytes.ushortToBytesLE(value, buffer, offset);
        offset += 2;
        
        return this;
    }
    
    public Serializer writeInt32(int value) {
        Bytes.intToBytesLE(value, buffer, offset);
        offset += 4;
        return this;
    }
    
    public Serializer writeInt64(long value) {
        Bytes.longToBytesLE(value, buffer, offset);
        offset += 8;
        return this;
    }
}
