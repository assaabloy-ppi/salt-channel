package saltchannel.v2.packets;

import saltchannel.util.Bytes;

/**
 * Header of a packet, two bytes long.
 * 
 * @author Frans Lundberg
 */
public class PacketHeader {
    public static int SIZE = 2;
    private static int BIT_COUNT = SIZE * 8 - 4;
    private static final int[] BITS = {1, 2, 4, 8, 16, 32, 64, 128};
    private static final int TYPE_MASK = 1 + 2 + 4 + 8;    // four least significant bits
    
    public byte[] bytes = new byte[SIZE];
    
    public PacketHeader() {}
    
    public PacketHeader(int type) {
        setType(type);
    }
    
    public void setType(int type) {
        if (type < 0 || type > 15) {
            throw new IllegalArgumentException("bad type value, " + type);
        }
        
        int b0 = Bytes.unsigned(bytes[0]);
        bytes[0] = (byte) ((b0 & ~TYPE_MASK) | type);
    }
    
    public int getType() {
        return Bytes.unsigned(bytes[0]) & TYPE_MASK;
    }
    
    public void setBit(int index, boolean value) {
        checkBitIndex(index);
        int index2 = index + 4;
        int byteOffset = index2 / 8;
        int bitOffset = index2 - 8 * byteOffset;
        
        int byteValue = Bytes.unsigned(bytes[byteOffset]);
        int mask = BITS[bitOffset];
        
        if (value == true) {
            byteValue = byteValue | mask;
        } else {
            byteValue = byteValue & ~mask;
        }
        
        bytes[byteOffset] = (byte) byteValue;
    }
    
    public boolean getBit(int index) {
        checkBitIndex(index);
        int index2 = index + 4;
        int byteOffset = index2 / 8;
        int bitOffset = index2 - 8 * byteOffset;
        
        int value = Bytes.unsigned(bytes[byteOffset]);
        int mask = BITS[bitOffset];
        return (value & mask) != 0;
    }
    
    private void checkBitIndex(int index) {
        if (index < 0 || index > BIT_COUNT) {
            throw new IllegalArgumentException("index = " + index);
        }
    }
}
