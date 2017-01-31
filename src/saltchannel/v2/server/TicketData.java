package saltchannel.v2.server;

/**
 * Inner ticket data; the data that is encrypted.
 */
public class TicketData {
    public static final int SESSION_KEY_SIZE = 32;
    public static final int CLIENT_SIG_KEY_SIZE = 32;
    
    public byte[] sessionKey;    // shared symmetric session key
    public byte[] clientSigKey;  // client's public signature key
    
    public int getSize() {
        return 32 + 32;
    }
    
    public byte[] toBytes() {
        if (sessionKey == null || sessionKey.length != 32) {
            throw new IllegalStateException("bad sessionKey value");
        }
        
        if (clientSigKey == null || clientSigKey.length != 32) {
            throw new IllegalStateException("bad clientSigKey value");
        }
        
        int off = 0;
        byte[] result = new byte[getSize()];
        
        System.arraycopy(sessionKey, 0, result, off, 32);
        off += 32;
        
        System.arraycopy(clientSigKey, 0, result, off, 32);
        off += 32;
        
        if (off != getSize()) {
            throw new IllegalStateException("off did not match getSize()");
        }
        
        return result;
    }
    
    public static TicketData fromBytes(byte[] bytes, int offset) {
        TicketData d = new TicketData();
        d.sessionKey = new byte[32];
        d.clientSigKey = new byte[32];
        int off = offset;
        
        System.arraycopy(bytes, off, d.sessionKey, 0, 32);
        off += 32;
        
        System.arraycopy(bytes, off, d.clientSigKey, 0, 32);
        off += 32;
        
        return d;
    }
}
