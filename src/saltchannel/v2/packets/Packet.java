package saltchannel.v2.packets;

/**
 * A low-level packet.
 */
public interface Packet {
    public static final int TYPE_M1 = 1;
    public static final int TYPE_M2 = 2;
    public static final int TYPE_M3 = 3;
    public static final int TYPE_M4 = 4;
    public static final int TYPE_APP_MESSAGE = 5;
    public static final int TYPE_ENCRYPTED_MESSAGE = 6;
    public static final int TYPE_TICKET = 7;
    public static final int TYPE_A1 = 8;
    public static final int TYPE_A2 = 9;
    public static final int TYPE_TT = 10;
    
    /**
     * Returns byte size of packet when serialized to a byte array.
     */
    public int getSize();
    
    /**
     * Serializes packet to bytes.
     */
    public void toBytes(byte[] destination, int offset);
    
    /**
     * Returns the packet type.
     */
    public int getType();
}
