package saltchannel.v2.packets;

import saltchannel.util.Bytes;

/**
 * Header of a packet, two bytes long.
 * 
 * @author Frans Lundberg
 */
public class PacketHeader {
    public static int SIZE = 2;
    private static int BIT_COUNT = 8;
    private static final int[] BIT_MASKS = {1, 2, 4, 8, 16, 32, 64, 128};
    
    private final byte[] bytes = new byte[SIZE];
    
    public PacketHeader() {
    }
    
    public PacketHeader(int type) {
        this();
        setType(type);
    }
    
    /**
     * Returns the header serialized in a byte array.
     */
    public final byte[] bytes() {
        return bytes;
    }
    
    public void setType(int type) {
        if (type < 0 || type > 15) {
            throw new IllegalArgumentException("bad type value, " + type);
        }
        
        //int b0 = Bytes.unsigned(bytes[0]);
        //bytes[0] = (byte) ((b0 & ~TYPE_MASK) | type);
        
        bytes[0] = (byte) type;
    }
    
    public int getType() {
        //return Bytes.unsigned(bytes[0]) & TYPE_MASK;
        return bytes[0];
    }
    
    /**
     * Sets a bit indexed from 0 to 11.
     */
    public void setBit(int index, boolean booleanValue) {
        checkBitIndex(index);
        
        //int index2 = index + 4;
        //int byteOffset = index2 / 8;
        //int bitOffset = index2 - 8 * byteOffset;
        
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
    
    private void checkBitIndex(int index) {
        if (!(index >= 0 && index < BIT_COUNT)) {
            throw new IllegalArgumentException("index is " + index);
        }
    }
}
