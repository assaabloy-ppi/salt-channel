package saltchannel.v2.server;

import org.junit.Assert;
import org.junit.Test;

public class EncryptedTicketDataTest {

    @Test
    public void testLength() {
        EncryptedTicketData data = new EncryptedTicketData();
        data.header = 0;
        data.hostData = new byte[EncryptedTicketData.HOST_DATA_SIZE];
        data.encryptedBytes = new byte[16 + 22];
        
        byte[] bytes = data.toBytes();
        
        Assert.assertEquals(bytes.length, data.getSize());
        Assert.assertEquals(16+22, data.sizeOfEncryptedBytes);
    }
    
    @Test
    public void testToBytesAndBack() {
        EncryptedTicketData d1 = new EncryptedTicketData();
        d1.header = 15;
        d1.hostData = new byte[EncryptedTicketData.HOST_DATA_SIZE];
        d1.hostData[3] = 3;        
        d1.encryptedBytes = new byte[16 + 2];
        d1.encryptedBytes[4] = 4;
        
        byte[] bytes = d1.toBytes();
        
        EncryptedTicketData d2 = EncryptedTicketData.fromBytes(bytes, 0);
        
        Assert.assertEquals(15, d2.header);
        Assert.assertEquals(d1.sizeOfEncryptedBytes, d2.sizeOfEncryptedBytes);
        Assert.assertArrayEquals(d1.hostData, d2.hostData);
        Assert.assertArrayEquals(d1.encryptedBytes, d2.encryptedBytes);
    }
}
