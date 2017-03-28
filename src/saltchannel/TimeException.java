package saltchannel;

/**
 * Thrown to indicate that that a time delay was detected
 * based in message timestamps.
 *
 * @author Frans Lundberg
 */
public class TimeException extends ComException {
    private static final long serialVersionUID = 1L;
    
    public TimeException(String message) {
        super(message);
    }
    
    public TimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
