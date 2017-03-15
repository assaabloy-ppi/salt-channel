package saltchannel;

import org.junit.Assert;
import org.junit.Test;

import saltchannel.util.KeyPair;

public class CryptoLibTest {

    @Test
    public void testCreateSigKeysFromSec() {
        byte[] sec = CryptoTestData.aSig.sec();
        KeyPair sig = CryptoLib.createSigKeysFromSec(sec);
        Assert.assertArrayEquals(sec, sig.sec());
        Assert.assertArrayEquals(CryptoTestData.aSig.pub(), sig.pub());
    }
}
