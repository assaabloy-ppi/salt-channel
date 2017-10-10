package saltchannel.v2.packets;

/**
 * A low-level packet definition.
 * 
 * Beyond the methods defined by this interface, implementing classes should
 * have a public static fromBytes(byte[] data, int offset) method that parses the
 * data. The fromBytes() method throws BadPeer if the bytes are not correctly
 * formatted to parse the packet.
 * 
 * @author Frans Lundberg
 */
public interface Packet {
    public static final int TYPE_M1 = 1;
    public static final int TYPE_M2 = 2;
    public static final int TYPE_M3 = 3;
    public static final int TYPE_M4 = 4;
    public static final int TYPE_APP_PACKET = 5;
    public static final int TYPE_ENCRYPTED_MESSAGE = 6;
    public static final int TYPE_TICKET = 7;
    public static final int TYPE_A1 = 8;
    public static final int TYPE_A2 = 9;
    public static final int TYPE_TT = 10;
    public static final int TYPE_MULTI_APP_PACKET = 11;
    
    /**
     * Returns size of packet when serialized to a byte array.
     * 
     * @return The number of bytes of the packet.
     */
    public int getSize();
    
    /**
     * Serializes packet to bytes.
     * 
     * @param destination  Buffer to write the packet to.
     * @param offset  Offset in buffer to start at.
     * @throws IllegalStateException
     *          If the state of the in-memory representation of the packet is illegal.
     */
    public void toBytes(byte[] destination, int offset);
    
    /**
     * Returns the packet type.
     * 
     * @return Packet type.
     */
    public int getType();
    
}
