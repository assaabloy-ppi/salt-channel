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

    @Test
    public void testSign() {
        byte[] data1 = new byte[]{1, 2, 3};
        byte[] signed = CryptoLib.sign(data1, CryptoTestData.aSig.sec());
        byte[] data2 = CryptoLib.signOpen(signed, CryptoTestData.aSig.pub());
        Assert.assertArrayEquals(data1, data2);
    }
}
