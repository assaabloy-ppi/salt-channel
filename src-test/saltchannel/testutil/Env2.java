package saltchannel.testutil;

import saltchannel.ByteChannel;
import saltchannel.Tunnel;
import saltchannel.util.CryptoTestData;
import saltchannel.v2.SaltClientSession;
import saltchannel.v2.SaltServerSession;

/** 
 * Client-server test environment, client run in separate thread.
 * 
 * @author Frans Lundberg
 */
public class Env2 {
    public Tunnel tunnel = new Tunnel();
    public ByteChannel ch1 = tunnel.channel1();
    public ByteChannel ch2 = tunnel.channel2();
    public SaltClientSession cs;
    public SaltServerSession ss;
    public ByteChannel serverAppChannel;
    
    public Env2() {
    }
    
    /**
     * Sets up client/server channels, starts client in new thread,
     * does handshake.
     */
    public void start() {
        cs = new SaltClientSession(CryptoTestData.aSig, ch1);
        cs.setEncKeyPair(CryptoTestData.aEnc);
        ss = new SaltServerSession(CryptoTestData.bSig, ch2);
        ss.setEncKeyPair(CryptoTestData.bEnc);
        
        Thread t = new Thread(new Runnable() {
            public void run() {
                cs.handshake();
                ByteChannel app = cs.getChannel();
                app.write(false, new byte[]{1, 5, 5, 5, 5, 5});
            }
        });
        t.setName("Env1-client");
        t.start();
        
        ss.handshake();
        serverAppChannel = ss.getChannel();
    }
}
