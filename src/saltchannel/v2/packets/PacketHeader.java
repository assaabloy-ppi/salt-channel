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
    
    public boolean eosFlag() {
        return getBit(7);
    }
    
    public void setEosFlag(boolean b) {
        setBit(7, b);
    }
    
    private void checkBitIndex(int index) {
        if (!(index >= 0 && index < BIT_COUNT)) {
            throw new IllegalArgumentException("index is " + index);
        }
    }
}
