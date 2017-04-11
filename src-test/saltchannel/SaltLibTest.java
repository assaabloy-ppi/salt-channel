package saltchannel;

import org.junit.Assert;
import org.junit.Test;

import saltchannel.util.Hex;
import saltaa.*;

public class SaltLibTest {

    @Test
    public void testHashOfEmptyStringJavaMode() {
        // Tests sha512 hash. Data taken from https://en.wikipedia.org/wiki/SHA-2.
        
        SaltLib salt = SaltLibFactory.getLib(SaltLibFactory.LibType.JAVA);

        byte[] m = new byte[0];
        int n = 0;
        byte[] out = new byte[64];
        
        salt.crypto_hash(out, m);
        
        byte[] expected = Hex.toBytes("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e");
        
        Assert.assertArrayEquals(expected, out);
    }

    @Test
    public void testHashOfEmptyStringNativeMode() {
        // Tests sha512 hash. Data taken from https://en.wikipedia.org/wiki/SHA-2.
        
        SaltLib salt = SaltLibFactory.getLib(SaltLibFactory.LibType.NATIVE);

        byte[] m = new byte[0];
        int n = 0;
        byte[] out = new byte[64];
        
        salt.crypto_hash(out, m);
        
        byte[] expected = Hex.toBytes("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e");
        
        Assert.assertArrayEquals(expected, out);
    }

}
