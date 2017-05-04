package saltaa;

/**
 * Thrown to indicate that no such library exists and is operational.
 * 
 * @author Frans Lundberg
 */
public class NoSuchLibException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public NoSuchLibException() {}
    
    public NoSuchLibException(Throwable cause) {
        super(cause);
    }
}
