package saltchannel.v2;

import org.junit.Test;

import saltchannel.Tunnel;
import saltchannel.util.CryptoTestData;

/**
 * Note, most testing is done elsewhere together with SaltServerSession.
 */
public class SaltClientSessionTest {

    @Test(expected=IllegalStateException.class)
    public void testExceptionThrownWhenEncKeyPairForgotten() {
        Tunnel tunnel = new Tunnel();
        SaltClientSession s = new SaltClientSession(CryptoTestData.aSig, tunnel.channel1());
        s.handshake();
    }
}
