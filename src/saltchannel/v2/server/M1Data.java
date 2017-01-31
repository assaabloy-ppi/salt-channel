package saltchannel.v2.server;

import saltchannel.BadPeer;
import saltchannel.util.Bytes;

/**
 * Data of the M1 message, low-level serialization/parsing between fields and byte array.
 * 
 * @author Frans Lundberg
 */
public class M1Data {
    public int header;
    public byte[] clientEncKey;
    public byte[] serverSigKey;
    public EncryptedTicketData resumeTicket;
    public int byteSize;
   
    public static final int SERVER_SIG_KEY_BIT = 0x01;
    public static final int RESUME_BIT = 0x02;
    public static final int CLIENT_ENC_KEY_BIT = 0x04;
    private static final int TWO_TO_16 = 65536;    // 2^16
    
    public EncryptedTicketData getResumeTicket() {
        return resumeTicket;
    }
    
    public boolean hasServerSigKey() {
        return (header & SERVER_SIG_KEY_BIT) != 0;
    }
    
    public boolean hasResumeTicket() {
        return (header & RESUME_BIT) != 0;
    }
    
    public boolean hasClientEncKey() {
        return (header & CLIENT_ENC_KEY_BIT) != 0;
    }
    
    /**
     * Returns the total byte size.
     */
    public int getSize() {
        return 2 + 2 
                + (hasClientEncKey() ? 32 : 0) 
                + (hasServerSigKey() ? 32 : 0)
                + (hasResumeTicket() ? this.resumeTicket.getSize() : 0);
    }
    
    public byte[] toBytes() {
        if (this.header < 0 || this.header >= TWO_TO_16) {
            throw new IllegalStateException("bad this.header value, " + this.header);
        }
        
        if (!this.hasClientEncKey() && !this.hasResumeTicket()) {
            throw new IllegalStateException("bad M1 message, no ClientEncKey and no ResumeTicket");
        }
        
        byte[] result = new byte[getSize()];
        int off = 0;
        
        result[off]     = (byte) 'S';
        result[off + 1] = (byte) '2';
        off += 2;
        
        Bytes.intToBytesLE(this.header, result, off);
        off += 2;
        
        if (this.hasClientEncKey()) {
            System.arraycopy(this.clientEncKey, 0, result, off, 32);
            off += 32;
        }
        
        if (this.hasServerSigKey()) {
            System.arraycopy(this.serverSigKey, 0, result, off, 32);
            off += 32;
        }
        
        if (this.hasResumeTicket()) {
            byte[] ticketBytes = this.resumeTicket.toBytes();
            System.arraycopy(ticketBytes, 0, result, off, ticketBytes.length);
            off += ticketBytes.length;
        }
        
        if (off != getSize()) {
            throw new Error("bug, bad off vs size, " + off + ", " + getSize());
        }
        
        return result;
    }
    
    public static M1Data fromBytes(byte[] bytes) {
        int off = 0;
        M1Data m = new M1Data();
        
        if (bytes[off] != 'S' || bytes[off+1] != '2') {
            throw new BadPeer("bad protocol indicator, " + (int)bytes[off] + " " + (int)bytes[off+1]);
        }
        off += 2;
        
        m.header = Bytes.unsigned(bytes[off]) + 256 * Bytes.unsigned(bytes[off + 1]);
        off += 2;
        
        if (m.hasClientEncKey()) {
            m.clientEncKey = new byte[32];
            System.arraycopy(bytes, off, m.clientEncKey, 0, m.clientEncKey.length);
            off += 32;
        }
        
        if (m.hasServerSigKey()) {
            m.serverSigKey = new byte[32];
            System.arraycopy(bytes, off, m.serverSigKey, 0, m.serverSigKey.length);
            off += 32;
        }
        
        if (m.hasResumeTicket()) {
            m.resumeTicket = EncryptedTicketData.fromBytes(bytes, off);
        }
        
        if (!m.hasClientEncKey() && !m.hasResumeTicket()) {
            throw new BadPeer("bad M1 message, no ClientEncKey and no ResumeTicket");
        }
        
        return m;
    }
}
