package saltaa;

/**
 * Thrown to indicate that encrypted and authenticated data was not valid.
 * The authentication tag (MAC) was invalid.
 * 
 * @author Frans Lundberg
 */
public class BadEncryptedDataException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public BadEncryptedDataException() {}
}
