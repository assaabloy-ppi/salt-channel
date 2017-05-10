package saltchannel;

import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Test;

import saltchannel.util.CryptoTestData;
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
    
    @Test
    public void testEncryptDecrypt() throws UnsupportedEncodingException {
        
        byte[] key = new byte[32];
        key[30] = 30;
        
        byte[] nonce = new byte[24];
        nonce[1] = 1;
        
        byte[] encrypted = CryptoLib.encrypt(key, nonce, "hello world".getBytes("UTF-8"));
        byte[] clear = CryptoLib.decrypt(key, nonce, encrypted);
        
        String clearString = new String(clear, "UTF-8");
        
        Assert.assertEquals("hello world", clearString);
    }
}
