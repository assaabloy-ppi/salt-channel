package saltchannel.v2.packets;

import saltchannel.util.Bytes;

/**
 * Header of a packet.
 * 
 * @author Frans Lundberg
 */
public class PacketHeader {
    public static int SIZE = 2;
    private static int BIT_COUNT = 8;
    private static final int[] BIT_MASKS = {1, 2, 4, 8, 16, 32, 64, 128};
    
    private final byte[] bytes = new byte[SIZE];
    
    /**
     * Private, use other constructors or create functions.
     */
    private PacketHeader() {}
    
    public PacketHeader(int type) {
        this();
        setType(type);
    }
    
    /**
     * Reads header from 'bytes' at given offset.
     */
    public PacketHeader(byte[] bytes, int offset) {
        if (bytes.length < offset + SIZE) {
            throw new IllegalArgumentException("input 'bytes' parameter, array too small");
        }
        System.arraycopy(bytes, offset, this.bytes, 0, SIZE);
    }
    
    /**
     * Creates a new instance without any specified type.
     * Useful for testing.
     */
    static PacketHeader create() {
        return new PacketHeader();
    }
    
    /**
     * Returns the header serialized in a byte array.
     */
    public final byte[] bytes() {
        return bytes;
    }
    
    public void setType(int type) {
        if (type < 0 || type > 127) {
            throw new IllegalArgumentException("bad type value, " + type);
        }
        
        bytes[0] = (byte) type;
    }
    
    public int getType() {
        return bytes[0];
    }
    
    /**
     * Sets a bit indexed from 0 to 7.
     */
    public void setBit(int index, boolean booleanValue) {
        checkBitIndex(index);
        
        int byteValue = Bytes.unsigned(bytes[1]);
        int mask = BIT_MASKS[index];
        
        if (booleanValue == true) {
            byteValue = byteValue | mask;
        } else {
            byteValue = byteValue & ~mask;
        }
        
        bytes[1] = (byte) byteValue;
    }
    
    public boolean getBit(int index) {
        checkBitIndex(index);
        
        int value = Bytes.unsigned(bytes[1]);
        int mask = BIT_MASKS[index];
        return (value & mask) != 0;
    }
    
    public boolean lastFlag() {
        return getBit(7);
    }
    
    public void setLastFlag(boolean b) {
        setBit(7, b);
    }
    
    public String toString() {
        return "type:" + getType() + ", last:" + lastFlag();
    }
    
    private void checkBitIndex(int index) {
        if (!(index >= 0 && index < BIT_COUNT)) {
            throw new IllegalArgumentException("index is " + index);
        }
    }
}
