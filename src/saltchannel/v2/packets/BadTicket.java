package saltchannel.v2.packets;

public class BadTicket extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public BadTicket(String message) {
        super(message);
    }
}
