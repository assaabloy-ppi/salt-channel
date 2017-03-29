package saltchannel.v2;

/**
 * Data that the client needs to store between sessions
 * to use the resume feature.
 * 
 * @author Frans Lundberg
 */
public class ClientTicketData {
    /** Ticket bytes received from the server. */
    public byte[] ticket;
    
    /** The session key, 32 bytes. */
    public byte[] sessionKey;
    
    /** 
     * 8-byte session nonce. Part of actual encrypt/decrypt nonce. 
     * Guaranteed to be unique for every session with a particular
     * client-server-sessionKey tuple.
     */
    public byte[] sessionNonce;
}
