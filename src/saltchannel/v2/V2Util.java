package saltchannel.v2;

import saltaa.*;
import saltchannel.BadPeer;
import saltchannel.util.Deserializer;
import saltchannel.util.KeyPair;
import saltchannel.v2.packets.PacketHeader;

public class V2Util {
    private static SaltLib salt = SaltLibFactory.getLib();
    
    public static final byte[] SIG1_PREFIX = new byte[] {
            0x53, 0x43, 0x2d, 0x53, 0x49, 0x47, 0x30, 0x31 }; 
            // "SC-SIG01" in ASCII
    public static final byte[] SIG2_PREFIX = new byte[] {
            0x53, 0x43, 0x2d, 0x53, 0x49, 0x47, 0x30, 0x32 };
            // "SC-SIG02" in ASCII

    public static byte[] createSignature(KeyPair sigKeyPair, byte[]... arrays) {
        byte[] message = concat(arrays);
        byte[] signedMessage = new byte[message.length + 64];
        salt.crypto_sign(signedMessage, message, sigKeyPair.sec());

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

    public static PacketHeader parseHeader(byte[] messageBytes) {
        if (messageBytes.length < PacketHeader.SIZE) {
            throw new BadPeer("cannot read header, message too small, " + messageBytes.length);
        }
        return new Deserializer(messageBytes, 0).readHeader();
    }
}
