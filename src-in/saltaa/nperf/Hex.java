package saltaa.nperf;

import java.util.Locale;

//Copyright Frans Lundberg, Stockholm, 2017.
//Public domain code copied from https://github.com/franslundberg/java-cut/.

/**
 * Utility methods for HEX strings.
 * 
 * @author Frans Lundberg
 */
public class Hex {

    private static final char[] HEX_ARRAY = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
            'f' };

    /** No instances should be created. */
    private Hex() {
    }

    /**
     * Returns a hex string (two lowercase hex chars per byte) given a byte
     * array.
     */
    public static String create(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes == null not allowed");
        }
        return create(bytes, 0, bytes.length);
    }

    /**
     * Returns a hex string (two lowercase hex chars per byte) given a byte
     * array.
     */
    public static String create(byte[] bytes, int offset, int length) {
        return new String(toHexCharArray(bytes, offset, length));
    }

    /**
     * Returns a char array (string) with hex chars of given a byte array.
     */
    public static char[] toHexCharArray(byte[] bytes, int offset, int length) {
        char[] hexChars = new char[length * 2];
        int v;
        for (int i = 0; i < length; i++) {
            v = bytes[offset + i] & 0xff;
            hexChars[i * 2] = HEX_ARRAY[v / 16];
            hexChars[i * 2 + 1] = HEX_ARRAY[v % 16];
        }
        return hexChars;
    }

    /**
     * Converts hex string to bytes.
     * 
     * @throws IllegalArgumentException
     *             If argument 'hexString' is not a valid hex string.
     */
    public static byte[] toBytes(String hexString) {
        if (hexString == null) {
            return null;
        }

        if (hexString.length() % 2 != 0) {
            throw new IllegalArgumentException("Bad length of hexString, was: " + hexString);
        }

        hexString = hexString.toLowerCase(Locale.US);

        int byteCount = hexString.length() / 2;
        byte[] result = new byte[byteCount];
        for (int i = 0; i < byteCount; i++) {
            int c0 = hexCharToInt(hexString.charAt(i * 2));
            int c1 = hexCharToInt(hexString.charAt(i * 2 + 1));

            if (c0 == -1 || c1 == -1) {
                throw new IllegalArgumentException("String has an invalid char (not hex char): " + hexString);
            }

            result[i] = (byte) (c0 * 16 + c1);
        }

        return result;
    }

    private static int hexCharToInt(char c) {
        int result = -1;

        if ((c >= '0') && (c <= '9')) {
            result = c - '0';
        } else if (c >= 'a' && c <= 'f') {
            result = 10 + c - 'a';
        }

        return result;
    }

    public String toCBytes(byte[] bytes) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            b.append("0x" + Hex.create(new byte[] { bytes[i] }));
            if (i != bytes.length - 1) {
                b.append(",");
            }

            if (i % 16 == 15) {
                b.append("\n");
            } else {
                b.append(" ");
            }
        }
        return b.toString();
    }
}
