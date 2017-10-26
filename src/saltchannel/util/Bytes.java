// Copyright Frans Lundberg, Stockholm 2016.
// Public domain code copied from https://github.com/franslundberg/java-cut/ 
// October 2016. Tested there.

package saltchannel.util;

import java.io.UnsupportedEncodingException;

/**
 * Utility class for storing primitive types: long, double, int, short, ushort in byte arrays.
 * The implementation here was developed as a performance optimization; Java's ByteBuffer
 * was significantly slower when tested.
 * 
 * Originally taken from BergDB (bergdb.com), December 2015.
 * 
 * @author Frans Lundberg
 */
public class Bytes {
    //
    // Testing (bergdb.common.perf.BytesToLong) shows that this class is faster
    // then Java's ByteBuffer.
    //
    
    /** No reason to create instances. */
    private Bytes() {}
    
    // --- long ----
    
    /**
     * Creates a long from an array of bytes. Little-endian-first byte order is
     * assumed.
     */
    public static final long bytesToLongLE(byte[] arr, int offset) {
        int i = offset;
        long result = (arr[i++] & 0x000000ffL);
        result |= (arr[i++] & 0x000000ffL) << 8;
        result |= (arr[i++] & 0x000000ffL) << 16;
        result |= (arr[i++] & 0x000000ffL) << 24;
        result |= (arr[i++] & 0x000000ffL) << 32;
        result |= (arr[i++] & 0x000000ffL) << 40;
        result |= (arr[i++] & 0x000000ffL) << 48;
        result |= (arr[i]   & 0x000000ffL) << 56;
        return result;
    }
    
    /**
     * Writes a long to a byte array. Little endian first byte order is used.
     */
    public static final void longToBytesLE(final long value, final byte[] arr, int offset) {
        int i = offset;
        arr[i++] = (byte) value;
        arr[i++] = (byte) (value >>> 8);
        arr[i++] = (byte) (value >>> 16);
        arr[i++] = (byte) (value >>> 24);
        arr[i++] = (byte) (value >>> 32);
        arr[i++] = (byte) (value >>> 40);
        arr[i++] = (byte) (value >>> 48);
        arr[i]   = (byte) (value >>> 56);
    }

    /**
     * Creates a long from an array of bytes. Big-endian-first byte order is
     * assumed.
     */
    public static final long bytesToLongBE(byte[] arr, int offset) {
        int i = offset;
        long result = (arr[i++] & 0x000000ffL) << 56;
        result |= (arr[i++] & 0x000000ffL) << 48;
        result |= (arr[i++] & 0x000000ffL) << 40;
        result |= (arr[i++] & 0x000000ffL) << 32;
        result |= (arr[i++] & 0x000000ffL) << 24;
        result |= (arr[i++] & 0x000000ffL) << 16;
        result |= (arr[i++] & 0x000000ffL) << 8;
        result |= (arr[i++] & 0x000000ffL);
        return result;
    }
    
    /**
     * Writes a long to a byte array. Big-endian byte order is used.
     */
    public static final void longToBytesBE(final long v, final byte[] bytes, int offset) {
        // Inspired by Sun's implementation of java.io.DataInputStream.
        int i = offset;
        bytes[i++] = (byte)(v >>> 56);
        bytes[i++] = (byte)(v >>> 48);
        bytes[i++] = (byte)(v >>> 40);
        bytes[i++] = (byte)(v >>> 32);
        bytes[i++] = (byte)(v >>> 24);
        bytes[i++] = (byte)(v >>> 16);
        bytes[i++] = (byte)(v >>>  8);
        bytes[i] = (byte) v;
    }
    
    // ---- float ----
    
    public static void floatToBytesLE(final float v, final byte[] bytes, final int offset) {
        Bytes.intToBytesLE(Float.floatToRawIntBits(v), bytes, offset);
    }
    
    public static float bytesToFloatLE(byte[] arr, int offset) {
        int myInt = Bytes.bytesToIntLE(arr, offset);
        return Float.intBitsToFloat(myInt);
    }
    
    
    // ---- double ----
    
    public static void doubleToBytesLE(final double v, final byte[] bytes, final int offset) {
        Bytes.longToBytesLE(Double.doubleToRawLongBits(v), bytes, offset);
    }
    
    public static double bytesToDoubleLE(byte[] arr, int offset) {
        long myLong = Bytes.bytesToLongLE(arr, offset);
        return Double.longBitsToDouble(myLong);
    }
    
    // ---- short ----
    
    /**
     * Creates a short from an array of bytes. Little-endian-first byte order is
     * assumed.
     */
    public static final short bytesToShortLE(final byte[] arr, int offset) {
        int off = offset;
        int result = (arr[off++] & 0x00ff);
        result |= (arr[off++] & 0x00ff) << 8;
        return (short) result;
    }
    
    
    public static final void shortToBytesLE(final long value, final byte[] arr, int offset) {
        int i = offset;
        arr[i++] = (byte) value;
        arr[i++] = (byte) (value >>> 8);
    }
    
    // shortBE not implemented
    
    // ---- ushort ----
    // Unsigned short, value range: [0, 2^16-1].
    
    public static final int bytesToUShortLE(byte[] arr, int offset) {
        int result = 0;
        int b0 = arr[offset];
        int b1 = arr[offset + 1];

        if (b0 < 0) {
            b0 += 256;
        }
        if (b1 < 0) {
            b1 += 256;
        }
        result = b0 + 256 * b1;
        return result;
    }

    /**
     * Writes an unsigned short integer (0..2^16-1) to a byte array using little
     * endian first byte order.
     * 
     * @param value
     *            Integer value, must be between 0 and 2^16-1 (65536-1)
     * @throws IllegalArgumentException
     *             if value is not in the valid range.
     */
    public static final void ushortToBytesLE(int value, byte[] arr, int offset) 
            throws IllegalArgumentException {
        if (value < 0 || value >= 65536) {
            throw new IllegalArgumentException("bad value, " + value);
        }
        arr[offset] = (byte) (value % 256);
        arr[offset + 1] = (byte) (value / 256);
    }
    
    // ---- int ----

    /**
     * Creates an integer from a byte array. Little endian byte order is used.
     */
    public static final int bytesToIntLE(byte[] arr, int offset) {
        int i = offset;
        int result = (arr[i++] & 0x00ff);
        result |= (arr[i++] & 0x00ff) << 8;
        result |= (arr[i++] & 0x00ff) << 16;
        result |= (arr[i] & 0x00ff) << 24;
        return result;
    }
    
    /**
     * Converts an int to four bytes in a byte array. Little endian
     * first byte order is used.
     */
    public static final void intToBytesLE(int value, byte[] arr, int offset) {
        arr[offset++] = (byte) value;
        arr[offset++] = (byte) (value >>> 8);
        arr[offset++] = (byte) (value >>> 16);
        arr[offset] = (byte) (value >>> 24);
    }

    /**
     * Creates an integer from a byte array. Big endian byte order is used.
     */
    public static final int bytesToIntBE(byte[] arr, int offset) {
        int off = offset;
        int result = (arr[off++] & 0x00ff) << 24;
        result |= (arr[off++] & 0x00ff) << 16;
        result |= (arr[off++] & 0x00ff) << 8;
        result |= (arr[off++] & 0x00ff);
        return result;
    }

    /**
     * Converts a non-negative int to four bytes in a byte array. Little endian
     * first byte order is used.
     */
    public static void intToBytesBE(int v, byte[] arr, int offset) {
        // According to the Java VM specification
        // the (byte) cast simply discards all but the
        // least significant 8 bits.*/
        
        int off = offset;
        arr[off++] = (byte)(v >>> 24);
        arr[off++] = (byte)(v >>> 16);
        arr[off++] = (byte)(v >>>  8);
        arr[off] = (byte) v;
    }
    
    // ---- string ----
    // Not complete. Do we need it?
    
    public static void stringToBytes(String s, byte[] arr, int offset) {
        byte[] bytes;
        try {
            bytes = s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error("UTF-8 should always be available.");
        }
        System.arraycopy(bytes, 0, arr, offset, bytes.length);
    }
    
    /**
     * Copies data from longs to bytes. Little-endian byte order is used.
     * 
     * @param longs
     * @param longOffset
     * @param longCopySize  Number of longs to copy.
     * @param bytes
     * @param byteOffset
     */
    public static void longsToBytesLE(long[] longs, int longOffset, int longCopySize, byte[] bytes, int byteOffset) {
        int byteOff = byteOffset;
        
        for (int i = 0; i < longCopySize; i++) {
            Bytes.longToBytesLE(longs[longOffset + i], bytes, byteOff);
            byteOff += 8;
        }
    }
    
    public static byte[] longsToBytesLE(long[] longs) {
        byte[] bytes = new byte[longs.length * 8];
        longsToBytesLE(longs, 0, longs.length, bytes, 0);
        return bytes;
    }

    /**
     * Returns an int in range [0, 255] given a byte.
     * The byte is considered unsigned instead of the ordinary signed 
     * interpretation in Java.
     */
    public static int unsigned(byte b) {
        return b & 0x000000ff;
    }
    
    /**
     * Performance testing.
     */
    public static void main(String[] args) {
        byte[] arr = new byte[]{0, 0, 1, 2, 3, 4, 5, 6, 3, 3, 3, 3, 3, 3, 3, 3};
        long t0, t1;
        int n = 100*1000*1000;
        
        t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            Bytes.bytesToLongBE(arr, 0);
            arr[0] = (byte) i;
        }
        t1 = System.nanoTime();
        System.out.println("bytesToLongBE " + 1e-9*(t1-t0)/n);
        
        t0 = System.nanoTime();
        for (int i = 0; i < n/5; i++) {
            Bytes.bytesToLongLE(arr, 0);
            Bytes.bytesToLongLE(arr, 1);
            Bytes.bytesToLongLE(arr, 2);
            Bytes.bytesToLongLE(arr, 3);
            Bytes.bytesToLongLE(arr, 4);
            arr[0] = (byte) i;
        }
        t1 = System.nanoTime();
        System.out.println("bytesToLongLE " + 1e-9*(t1-t0)/n);
        
        
        t0 = System.nanoTime();
        for (int i = 0; i < n/5; i++) {
            Bytes.longToBytesLE(444333222111L, arr, 0);
            Bytes.longToBytesLE(444333222111L, arr, 1);
            Bytes.longToBytesLE(444333222111L, arr, 2);
            Bytes.longToBytesLE(444333222111L, arr, 3);
            Bytes.longToBytesLE(444333222111L, arr, 4);
            arr[0] = (byte) i;
        }
        t1 = System.nanoTime();
        System.out.println("longToBytesLE " + 1e-9*(t1-t0)/n);
        
        t0 = System.nanoTime();
        for (int i = 0; i < n/5; i++) {
            Bytes.longToBytesBE(444333222111L, arr, 0);
            Bytes.longToBytesBE(444333222111L, arr, 1);
            Bytes.longToBytesBE(444333222111L, arr, 2);
            Bytes.longToBytesBE(444333222111L, arr, 3);
            Bytes.longToBytesBE(444333222111L, arr, 4);
            arr[0] = (byte) i;
        }
        t1 = System.nanoTime();
        System.out.println("longToBytesBE " + 1e-9*(t1-t0)/n);
    }
}
