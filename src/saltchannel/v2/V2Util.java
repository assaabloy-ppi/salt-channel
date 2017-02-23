package saltchannel.v2;

import saltchannel.TweetNaCl;
import saltchannel.util.KeyPair;

public class V2Util {
    
    public static byte[] createSignature(KeyPair sigKeyPair, byte[]... arrays) {
        byte[] message = concat(arrays);
        byte[] signedMessage = TweetNaCl.crypto_sign(message, sigKeyPair.sec());
        byte[] signature = new byte[64];
        System.arraycopy(signedMessage, 0, signature, 0, 64);
        return signature;
    }
    
    public static byte[] concat(byte[]... arrays) {
        int size = 0;
        for (int i = 0; i < arrays.length; i++) {
            size += arrays[i].length;
        }
        
        byte[] result = new byte[size];
        int offset = 0;
        
        for (int i = 0; i < arrays.length; i++) {
            byte[] array = arrays[i];
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        
        return result;
    }
}
