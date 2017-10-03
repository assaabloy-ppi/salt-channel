package saltchannel.dev;

import java.util.HashMap;
import java.util.Map;
import saltchannel.ByteChannel;
import saltchannel.ComException;

/**
 * Decorates an existing ByteChannel by adding the possibility of 
 * manipulating byte messages.
 * 
 * @author Frans Lundberg
 */
public class ManipulableByteChannel implements ByteChannel {
    private int index = 0;
    private Map<Integer, Manipulation> map;
    private ByteChannel inner;
    
    public ManipulableByteChannel(ByteChannel inner) {
        map = new HashMap<Integer, Manipulation>();
        this.inner = inner;
    }
    
    public void addManipulation(int index, Manipulation manipulation) {
        this.map.put(index, manipulation);
    }

    @Override
    public byte[] read() throws ComException {
        byte[] original = inner.read();
        byte[] manipulated = manipulate(index, original);
        index++;
        return manipulated;
    }
    
    @Override
    public void write(byte[]... messages) throws ComException {
        write(false, messages);
    }

    @Override
    public void write(boolean isLast, byte[]... messages) throws ComException {
        byte[][] manipulated = new byte[messages.length][];
        
        for (int i = 0; i < messages.length; i++) {
            manipulated[i] = manipulate(index, messages[i]);
            index++;
        }
        
        inner.write(isLast, manipulated);
    }
    
    private byte[] manipulate(int index, byte[] original) {        
        Manipulation m = map.get(index);
        byte[] result;
        
        if (m == null) {
            result = original;
        } else {
            result = m.manipulate(index, original);
        }
        
        return result;
    }
    
    public interface Manipulation {
        
        /**
         * Returns the manipulated package.
         */
        byte[] manipulate(int packetIndex, byte[] originalBytes);
    }
}
