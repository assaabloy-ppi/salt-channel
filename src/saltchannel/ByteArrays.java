package saltchannel;

/**
 * Public static methods with functions working on byte arrays.
 */
public class ByteArrays {
    
    /**
     * Concatenates two byte arrays.
     * Input is not checked. 
     */
    public static byte[] concat(byte[] arr1, byte[] arr2) {
        byte[] result = new byte[arr1.length + arr2.length];
        System.arraycopy(arr1, 0, result, 0, arr1.length);
        System.arraycopy(arr2, 0, result, arr1.length, arr2.length);
        return result;
    }
    
    /**
     * Returns the range [from, to) of the input array.
     * 'from' is inclusive, 'to' is exclusive.
     * Input is not checked. 
     */
    public static byte[] range(byte[] in, int from, int to) {
        byte[] result = new byte[to - from];
        System.arraycopy(in, from, result, 0, result.length);
        return result;
    }
}
