package saltchannel.v2;

import saltchannel.BadPeer;
import saltchannel.util.Bytes;

/**
 * Data of the M1 message.
 * 
 * @author Frans Lundberg
 */
public class M1 {
    public int header;
    public byte[] clientEncKey;
    public byte[] serverSigKey;
    public ResumeTicket resumeTicket;
    public int byteSize;
   
    public static final int SERVER_SIG_KEY_BIT = 0x01;
    public static final int RESUME_BIT = 0x02;
    public static final int CLIENT_ENC_KEY_BIT = 0x04;
    
    public static M1 fromBytes(byte[] bytes) {
        int off = 0;
        M1 m = new M1();
        
        if (bytes[off] != 'S' || bytes[off+1] != '2') {
            throw new BadPeer("bad protocol indicator");
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
            m.resumeTicket = ResumeTicket.fromBytes(bytes, off);
        }
        
        if (!m.hasClientEncKey() && !m.hasResumeTicket()) {
            throw new BadPeer("bad M1 message, no ClientEncKey and no ResumeTicket");
        }
        
        return m;
    }
    
    public ResumeTicket getResumeTicket() {
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
}
