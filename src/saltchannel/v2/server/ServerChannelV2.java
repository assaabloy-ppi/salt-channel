package saltchannel.v2.server;

import saltchannel.ByteChannel;
import saltchannel.CryptoLib;
import saltchannel.util.KeyPair;
import saltchannel.util.Rand;
import saltchannel.v2.packets.M1Packet;

/**
 * Server-side implementation of Salt Channel v2.
 * Usage: create object, set or crate ephemeral key, 
 * call handshake(), get resulting encrypted ByteChannel to use by
 * application layer.
 * Do not reuse the object for more than one Salt Channel session.
 * 
 * @author Frans Lundberg
 */
public class ServerChannelV2 {
    private final ByteChannel clearChannel;
    private KeyPair sigKeyPair;
    private KeyPair encKeyPair;
    private ResumeHandler resumeHandler;
    private byte[] clientSigKey;
    private byte[] symmetricKey;
    
    public ServerChannelV2(KeyPair sigKeyPair, ByteChannel clearChannel) {
        this.clearChannel = clearChannel;
        this.sigKeyPair = sigKeyPair;
    }
    
    public void setResumeHandler(ResumeHandler resumeHandler) {
        this.resumeHandler = resumeHandler;
    }
    
    /**
     * Sets the ephemeral encryption key pair.
     */
    public void setEncKeyPair(KeyPair encKeyPair) {
        this.encKeyPair = encKeyPair;
    }
    
    /**
     * Generates the ephemeral encryption key pair based on the
     * supplied source of randomness.
     */
    public void setEncKeyPair(Rand rand) {
        this.encKeyPair = CryptoLib.createEncKeys(rand);
    }
    
    public void handshake() {
        if (encKeyPair == null) {
            throw new IllegalStateException("encKeyPair must be set before calling handshake()");
        }
        
        M1Packet m1 = M1Packet.fromBytes(clearChannel.read());
        
        if (m1.hasTicket()) {
            TicketSessionData sessionData = null;
            
            try {
                sessionData = resumeHandler.checkTicket(m1.getTicket());
                this.clientSigKey = sessionData.clientSigKey;
                this.symmetricKey = sessionData.sessionKey;
            } catch (ResumeHandler.InvalidTicket e) {
                // empty
            }
        }
        
        if (symmetricKey != null) {
            return;  // we are done, ticket worked.
        }
    }
}
