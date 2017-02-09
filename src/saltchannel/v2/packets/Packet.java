package saltchannel.v2.packets;

/**
 * A low-level packet.
 */
public interface Packet {
    
    /**
     * Returns byte size of packet when serialized to a byte array.
     */
    public int getSize();
    
    /**
     * Serializes packet to bytes.
     */
    public void toBytes(byte[] destination, int offset);
}
