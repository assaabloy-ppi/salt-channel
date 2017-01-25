package saltchannel.v2;

import saltchannel.BadPeer;

/**
 * Raw data of a resume ticket, still encrypted.
 * 
 * @author Frans Lundberg
 */
public class ResumeTicket {
    public byte header;
    public byte size;
    public byte[] hostData;
    public byte[] encryptedTicket;
    
    public ResumeTicket() {
        hostData = new byte[10];
    }
    
    public static ResumeTicket fromBytes(byte[] bytes, final int offset) {
        int off = offset;
        ResumeTicket t = new ResumeTicket();
        
        t.header = bytes[off];
        off += 1;
        
        t.size = bytes[off];
        off += 1;
        if (t.size < 0) {
            throw new BadPeer("bad ticket size, " + t.size);
        }
        
        System.arraycopy(bytes, off, t.hostData, 0, 10);
        off += 10;
        
        t.encryptedTicket = new byte[t.size];
        System.arraycopy(bytes, off, t.encryptedTicket, 0, t.size);
        off += t.size;
        
        return t;
    }
    
    public void toBytes(byte[] dest, int destOffset) {
        // TODO implement
    }
}
