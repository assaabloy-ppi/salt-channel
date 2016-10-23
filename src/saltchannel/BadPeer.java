package saltchannel;

public class BadPeer extends ComException {
    private static final long serialVersionUID = 1L;
    
    public BadPeer(String message) {
        super(message);
    }
    
    public BadPeer(String message, Throwable cause) {
        super(message, cause);
    }
}
