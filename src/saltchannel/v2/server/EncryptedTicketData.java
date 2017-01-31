package saltchannel.v2.server;

import saltchannel.BadPeer;

/**
 * Raw data of a encrypted resume ticket.
 * 
 * @author Frans Lundberg
 */
public class EncryptedTicketData {
    public static final int HOST_DATA_SIZE = 10;
    
    public byte header;
    public byte sizeOfEncryptedBytes;
    public byte[] hostData;
    public byte[] encryptedBytes;
    
    /**
     * Creates a new object; fields must be set manually.
     */
    public EncryptedTicketData() {
        hostData = new byte[10];
    }
    
    public static EncryptedTicketData fromBytes(byte[] bytes, final int offset) {
        int off = offset;
        EncryptedTicketData t = new EncryptedTicketData();
        
        t.header = bytes[off];
        off += 1;
        
        t.sizeOfEncryptedBytes = bytes[off];
        off += 1;
        if (t.sizeOfEncryptedBytes < 0) {
            throw new BadPeer("bad ticket size, " + t.sizeOfEncryptedBytes);
        }
        
        System.arraycopy(bytes, off, t.hostData, 0, HOST_DATA_SIZE);
        off += HOST_DATA_SIZE;
        
        t.encryptedBytes = new byte[t.sizeOfEncryptedBytes];
        System.arraycopy(bytes, off, t.encryptedBytes, 0, t.sizeOfEncryptedBytes);
        off += t.sizeOfEncryptedBytes;
        
        return t;
    }
    
    /**
     * Creates bytes from the fields. this.size is computed from the other fields.
     */
    public byte[] toBytes() {
        int size = encryptedBytes.length;
        if (size > 127) {
            throw new IllegalStateException("encryptedTicket too large");
        }
        
        this.sizeOfEncryptedBytes = (byte) size;
        
        byte[] result = new byte[1 + 1 + HOST_DATA_SIZE + size];
        int off = 0;
        
        result[off] = this.header;
        off += 1;
        
        result[off] = this.sizeOfEncryptedBytes;
        off += 1;
        
        System.arraycopy(this.hostData, 0, result, off, HOST_DATA_SIZE);
        off += HOST_DATA_SIZE;
        
        System.arraycopy(this.encryptedBytes, 0, result, off, size);
        off += size;
        
        return result;
    }
    
    /**
     * Returns the byte size of the whole message.
     */
    public int getSize() {
        return 1 + 1 + HOST_DATA_SIZE + encryptedBytes.length;
    }
}
