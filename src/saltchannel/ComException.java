package saltchannel;

/**
 * Communication exception.
 * 
 * @author Frans Lundberg
 */
public class ComException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public ComException(String message) {
        super(message);
    }
    
    public ComException(String message, Throwable cause) {
        super(message, cause);
    }
}
