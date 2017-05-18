package saltchannel.testutil;

import saltchannel.ByteChannel;
import saltchannel.Tunnel;
import saltchannel.util.CryptoTestData;
import saltchannel.v2.SaltClientSession;
import saltchannel.v2.SaltServerSession;

/** 
 * Client-server test environment, server run in separate thread.
 * 
 * @author Frans Lundberg
 */
public class Env1 {
    public Tunnel tunnel = new Tunnel();
    public ByteChannel ch1 = tunnel.channel1();
    public ByteChannel ch2 = tunnel.channel2();
    public SaltClientSession cs;
    public SaltServerSession ss;
    
    /** Client's application channel. */
    public ByteChannel appChannel;
    
    public Env1() {
    }
    
    /**
     * Setsup client/server channels, starts echo server that echoes once,
     * calls handshake.
     */
    public void start() {
        cs = new SaltClientSession(CryptoTestData.aSig, ch1);
        cs.setEncKeyPair(CryptoTestData.aEnc);
        ss = new SaltServerSession(CryptoTestData.bSig, ch2);
        ss.setEncKeyPair(CryptoTestData.bEnc);
        
        Thread t = new Thread(new Runnable() {
            public void run() {
                ss.handshake();
                ByteChannel ac = ss.getChannel();
                ac.write(ac.read());
            }
        });
        t.setName("Env1-server");
        t.start();
        
        cs.handshake();
        appChannel = cs.getChannel();
    }
}
