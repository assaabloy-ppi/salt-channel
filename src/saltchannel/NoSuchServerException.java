package saltchannel;

/**
 * Thrown to indicate that the server with the given pubkey is
 * not available.
 *
 * @author Frans Lundberg
 */
public class NoSuchServerException extends ComException {
    private static final long serialVersionUID = 1L;

    public NoSuchServerException(String message) {
        super(message);
    }
}
