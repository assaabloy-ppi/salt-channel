package saltchannel.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for Serializer and Deserializer.
 */
public class SerializerTest {
    
    @Test
    public void testNonZeroOffset() {
        byte[] buffer = new byte[3];
        Serializer s = new Serializer(buffer, 1);
        s.writeByte(10).writeByte(250);
        
        Assert.assertEquals(0, buffer[0]);
        Assert.assertEquals(10, buffer[1]);
        Assert.assertEquals((byte) 250, buffer[2]);
    }

    @Test
    public void testByte() {
        byte[] buffer = new byte[3];
        Serializer s = new Serializer(buffer, 0);
        s.writeByte(10).writeByte(250);
        
        Deserializer d = new Deserializer(buffer, 0);
        byte b1 = d.readByte();
        byte b2 = d.readByte();
        byte b3 = d.readByte();
        
        Assert.assertEquals(10, b1);
        Assert.assertEquals((byte) 250, b2);
        Assert.assertEquals(0, b3);
    }
    
    @Test
    public void testUnsignedByte() {
        byte[] buffer = new byte[3];
        Serializer s = new Serializer(buffer, 0);
        s.writeByte(10).writeByte(250);
        
        Deserializer d = new Deserializer(buffer, 0);
        int b1 = d.readUnsignedByte();
        int b2 = d.readUnsignedByte();
        int b3 = d.readUnsignedByte();
        
        Assert.assertEquals(10, b1);
        Assert.assertEquals(250, b2);
        Assert.assertEquals(0, b3);
    }
    
    @Test
    public void testWriteBytes() {
        byte[] buffer = new byte[4];
        Serializer s = new Serializer(buffer, 0);
        s.writeBytes(new byte[]{1, 2});
        
        Assert.assertEquals(1, buffer[0]);
        Assert.assertEquals(2, buffer[1]);
        Assert.assertEquals(0, buffer[2]);
        Assert.assertEquals(0, buffer[3]);
    }
    
    @Test
    public void testBit1() {
        byte[] buffer = new byte[2];
        Serializer s = new Serializer(buffer, 0);
        s.writeBit(0);
        s.writeBit(1);
        s.writeBit(1);
        s.writeBit(0);
        Assert.assertEquals(2 + 4, buffer[0]);
    }
    
    @Test
    public void testBit2() {
        byte[] buffer = new byte[2];
        Serializer s = new Serializer(buffer, 0);
        s.writeBit(0);
        s.writeBit(1);
        s.writeBit(1);
        s.writeBit(0);
        
        Deserializer d = new Deserializer(buffer, 0);
        Assert.assertEquals(0, d.readBitAsInt());
        Assert.assertEquals(1, d.readBitAsInt());
        Assert.assertEquals(1, d.readBitAsInt());
        Assert.assertEquals(0, d.readBitAsInt());
    }
    
    @Test
    public void testManyBits() {
        byte[] buffer = new byte[3];
        Serializer s = new Serializer(buffer, 0);
        
        s.writeBit(0);
        s.writeBit(1);
        s.writeBit(1);
        s.writeBit(0);
        
        s.writeBit(1);
        s.writeBit(1);
        s.writeBit(1);
        s.writeBit(1);
        
        s.writeBit(1);
        s.writeBit(0);
        s.writeBit(0);
        s.writeBit(1);
        
        Deserializer d = new Deserializer(buffer, 0);
        
        Assert.assertEquals(0, d.readBitAsInt());
        Assert.assertEquals(1, d.readBitAsInt());
        Assert.assertEquals(1, d.readBitAsInt());
        Assert.assertEquals(0, d.readBitAsInt());
        
        Assert.assertEquals(1, d.readBitAsInt());
        Assert.assertEquals(1, d.readBitAsInt());
        Assert.assertEquals(1, d.readBitAsInt());
        Assert.assertEquals(1, d.readBitAsInt());
        
        Assert.assertEquals(1, d.readBitAsInt());
        Assert.assertEquals(0, d.readBitAsInt());
        Assert.assertEquals(0, d.readBitAsInt());
        Assert.assertEquals(1, d.readBitAsInt());
    }
    
    @Test
    public void testUint16() {
        byte[] buffer = new byte[8];
        Serializer s = new Serializer(buffer, 0);
        s.writeUint16(23456);
        s.writeUint16(64000);
        
        Deserializer d = new Deserializer(buffer, 0);
        Assert.assertEquals(23456, d.readUint16());
        Assert.assertEquals(64000, d.readUint16());
    }
    
    @Test
    public void testInt64() {
        byte[] buffer = new byte[16];
        Serializer s = new Serializer(buffer, 0);
        s.writeInt64(-23456);
        s.writeInt64(40*1000L*1000L*1000L);
        
        Deserializer d = new Deserializer(buffer, 0);
        Assert.assertEquals(-23456, d.readInt64());
        Assert.assertEquals(40*1000L*1000L*1000L, d.readInt64());
    }
}
