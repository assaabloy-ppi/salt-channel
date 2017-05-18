package saltchannel.v2;

import org.junit.Test;

import saltchannel.Tunnel;
import saltchannel.util.CryptoTestData;

/**
 * Note, most testing is done elsewhere together with SaltClientSession.
 */
public class SaltServerSessionTest {

    @Test(expected=IllegalStateException.class)
    public void testExceptionThrownWhenEncKeyPairForgotten() {
        Tunnel tunnel = new Tunnel();
        SaltServerSession s = new SaltServerSession(CryptoTestData.bSig, tunnel.channel2());
        s.handshake();
    }
}
