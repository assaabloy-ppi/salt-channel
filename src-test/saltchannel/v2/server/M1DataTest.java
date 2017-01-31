package saltchannel.v2.server;

import org.junit.Assert;
import org.junit.Test;

public class M1DataTest {
    @Test
    public void testFlags1() {
        M1Data d1 = new M1Data();
        d1.clientEncKey = new byte[32];
        d1.header = M1Data.CLIENT_ENC_KEY_BIT | M1Data.RESUME_BIT;
        d1.resumeTicket = ticket1();
        
        Assert.assertEquals(true, d1.hasClientEncKey());
        Assert.assertEquals(true, d1.hasResumeTicket());
        Assert.assertEquals(false, d1.hasServerSigKey());
    }
    
    @Test
    public void testToBytesAndBack() {
        M1Data d1 = new M1Data();
        d1.clientEncKey = new byte[32];
        d1.header = M1Data.CLIENT_ENC_KEY_BIT | M1Data.RESUME_BIT;
        d1.resumeTicket = ticket1();
        
        byte[] bytes1 = d1.toBytes();
        M1Data d2 = M1Data.fromBytes(bytes1);
        byte[] bytes2 = d2.toBytes();
        
        Assert.assertArrayEquals(bytes1, bytes2);
    }
    
    private static EncryptedTicketData ticket1() {
        EncryptedTicketData t = new EncryptedTicketData();
        t.encryptedBytes = new byte[16+2];
        t.hostData = new byte[EncryptedTicketData.HOST_DATA_SIZE];
        return t;
    }
}
