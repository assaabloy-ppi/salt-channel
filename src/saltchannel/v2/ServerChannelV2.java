package saltchannel.v2;

import saltchannel.ByteChannel;
import saltchannel.CryptoLib;
import saltchannel.util.KeyPair;
import saltchannel.util.Rand;

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
        
        M1 m1 = M1.fromBytes(clearChannel.read());
        if (m1.hasResumeTicket()) {
            resumeHandler.checkTicket(m1.getResumeTicket());
        }
    }
}
