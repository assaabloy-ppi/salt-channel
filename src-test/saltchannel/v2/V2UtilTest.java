package saltchannel.v2;

import java.io.UnsupportedEncodingException;
import org.junit.Assert;
import org.junit.Test;

public class V2UtilTest {
    @Test
    public void testSig1Prefix() throws UnsupportedEncodingException {
        byte[] bytes = "SALTSIG1".getBytes("US-ASCII");
        Assert.assertArrayEquals(V2Util.SIG1_PREFIX, bytes);
    }
    
    @Test
    public void testSig2Prefix() throws UnsupportedEncodingException {
        byte[] bytes = "SALTSIG2".getBytes("US-ASCII");
        Assert.assertArrayEquals(V2Util.SIG2_PREFIX, bytes);
    }
}
