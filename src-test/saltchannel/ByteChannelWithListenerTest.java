package saltchannel;

import org.junit.Assert;
import org.junit.Test;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ByteChannelWithListenerTest {

    @Test
    public void testThatListenToReadWorks() {
        final byte[] array = new byte[3];
        
        ByteChannel ch1 = new ByteChannel() {
            public byte[] read() throws ComException {
                return new byte[]{1, 2, 3};
            }

            public void write(byte[]... messages) throws ComException {}
        };
        
        ByteChannelWithListener ch2 = new ByteChannelWithListener(ch1, new ByteChannelWithListener.Listener() {
            public void onPostWrite(byte[][] byteArrays) {}
            public void onPostRead(byte[] byteArray) {
                System.arraycopy(byteArray, 0, array, 0, byteArray.length);
            }
            
            public void onPreRead() {}
            public void onPreWrite(byte[][] byteArrays) {}
        });
        
        byte[] bytes = ch2.read();
        
        Assert.assertArrayEquals(new byte[]{1, 2, 3}, bytes);
        Assert.assertArrayEquals(new byte[]{1, 2, 3}, array);
    }
    
    @Test
    public void testThatListenToWriteWorks() {
        final byte[] array1 = new byte[3];
        
        ByteChannel ch1 = new ByteChannel() {
            public byte[] read() throws ComException {
                throw new NotImplementedException();
            }

            public void write(byte[]... messages) throws ComException {}
        };
        
        ByteChannel ch2 = new ByteChannelWithListener(ch1, new ByteChannelWithListener.Listener() {
            public void onPostWrite(byte[][] byteArrays) {
                System.arraycopy(byteArrays[0], 0, array1, 0, byteArrays[0].length);
            }
            
            public void onPostRead(byte[] byteArray) {}
            public void onPreRead() {}
            public void onPreWrite(byte[][] byteArrays) {}
        });
        
        ch2.write(new byte[][]{new byte[]{2, 4, 6}});
        
        Assert.assertArrayEquals(new byte[]{2, 4, 6}, array1);
    }
}
