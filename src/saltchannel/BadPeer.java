package saltchannel;

/**
 * Thrown to indicate that the peer send bad data, data that does not 
 * follow spec.
 * 
 * @author Frans Lundberg
 */
public class BadPeer extends ComException {
    private static final long serialVersionUID = 1L;
    
    public BadPeer(String message) {
        super(message);
    }
    
    public BadPeer(String message, Throwable cause) {
        super(message, cause);
    }
}
