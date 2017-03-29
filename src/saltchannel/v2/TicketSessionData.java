package saltchannel.v2;

public class TicketSessionData {
    public long ticketId;
    public byte[] sessionKey;
    public byte[] sessionNonce;
    public byte[] clientSigKey;    
}
