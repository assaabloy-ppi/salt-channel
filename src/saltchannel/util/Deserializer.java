package saltchannel.util;

import java.io.UnsupportedEncodingException;
import saltchannel.BadPeer;
import saltchannel.v2.packets.PacketHeader;

/**
 * A deserializer that can deserialize (read) bytes created with 
 * Serializer. Throws BadPeer when deserialization cannot be done.
 * 
 * @author Frans Lundberg
 */
public class Deserializer {
    private byte[] buffer;
    private int firstOffset;
    private int offset;
    
    public Deserializer(byte[] buffer, int offset) {
        if (offset > buffer.length) {
            throw new BadPeer("offset beyond buffer length");
        }
        
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
    
    public byte readByte() {
        if (bytesLeft() < 1) {
            throw new BadPeer("no bytes left, " + offset);
        }
        
        byte result = buffer[offset];
        offset++;
        return result;
    }
    
    public int readUnsignedByte() {
        return Bytes.unsigned(readByte());
    }
    
    public byte[] readBytes(int size) {
        if (bytesLeft() < size) {
            throw new BadPeer("out of bounds, cannot read " + size + " bytes");
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
    
    public PacketHeader readHeader() {
        if (bytesLeft() < PacketHeader.SIZE) {
            throw new BadPeer("out of bounds, cannot read header");
        }
        PacketHeader header = new PacketHeader(buffer, offset);
        offset += PacketHeader.SIZE;
        return header;
    }
    
    public int readUint16() {
        if (bytesLeft() < 2) {
            throw new BadPeer("out of bounds, cannot read uint16");
        }
        
        int result = Bytes.bytesToUShortLE(buffer, offset);
        offset += 2;
        return result;
    }
    
    public int readInt32() {
        if (bytesLeft() < 4) {
            throw new BadPeer("out of bounds, cannot read int32");
        }
        
        int result = Bytes.bytesToIntLE(buffer, offset);
        offset += 4;
        return result;
    }
    
    public long readInt64() {
        if (bytesLeft() < 4) {
            throw new BadPeer("out of bounds, cannot read int64");
        }
        
        long result = Bytes.bytesToLongLE(buffer, offset);
        offset += 8;
        return result;
    }
    
    private int bytesLeft() {
        return buffer.length - offset;
    }
}
