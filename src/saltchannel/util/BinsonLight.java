// Copyright Frans Lundberg, Stockholm, 2016.
// This code is public domain. Use it as you please.
// Source was copied from https://github.com/franslundberg/binson-java-light, October 2016.

package saltchannel.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * A small, high-performance implementation of Binson, see binson.org.
 * 
 * Binson.Parser is used to parse a byte array to a sequence of Binson fields.
 * The parser uses a fixed and small amount of memory, no memory is allocated 
 * dynamically while parsing.
 * 
 * Binson.Writer is used to write a Binson object to an OutputStream.
 * 
 * In general, this implementation is indented to be small and high performance.
 * It is suitable for applications on small devices, for high-performance implementations, 
 * and for cases when a single public domain java source file is all that is needed instead
 * of a library dependency.
 * 
 * @author Frans Lundberg
 */
public class BinsonLight {
    public static enum ValueType {
        BOOLEAN, INTEGER, DOUBLE, STRING, BYTES, ARRAY, OBJECT
    }
    
    private static final byte BEGIN=0x40, END=0x41, BEGIN_ARRAY=0x42, END_ARRAY=0x43, 
        TRUE=0x44, FALSE=0x45, INTEGER1=0x10, INTEGER2=0x11, INTEGER4=0x12, INTEGER8=0x13,
        DOUBLE=0x46, STRING1=0x14, STRING2=0x15, STRING4=0x16, 
        BYTES1=0x18, BYTES2=0x19, BYTES4=0x1a;
    
    private static final int INT_LENGTH_MASK = 0x03;
    private static final int ONE_BYTE = 0x00, TWO_BYTES = 0x01, FOUR_BYTES = 0x02, EIGHT_BYTES = 0x03;
    private static final long TWO_TO_7 = 128, TWO_TO_15 = 32768, TWO_TO_31 = 2147483648L;
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * Parses a Binson object in a byte array (byte[]) to a sequence of Binson 
     * fields. A "stream parser"; no in-memory representation is built.
     */
    public static class Parser {
        private static final int STATE_ZERO = 200;
        private static final int STATE_BEFORE_FIELD = 201;
        private static final int STATE_BEFORE_ARRAY_VALUE = 202;
        private static final int STATE_BEFORE_ARRAY = 203;
        private static final int STATE_END_OF_ARRAY = 204;
        private static final int STATE_BEFORE_OBJECT = 205;
        private static final int STATE_END_OF_OBJECT = 206;

        /** Type of last value parsed. One of TYPE_BOOLEAN, TYPE_INTEGER, ... */
        private ValueType type;
        private StringValue name;
        private boolean booleanValue;
        private long integerValue;
        private double doubleValue;
        private StringValue stringValue;
        private BytesValue bytesValue;
        private int state = STATE_ZERO;
        private byte[] buffer;
        private int offset;

        /**
         * Creates a new Parser to parse the bytes in 'buffer'
         * starting from the first byte of the array.
         */
        public Parser(byte[] buffer) {
            this(buffer, 0);
        }

        /**
         * Creates a new Parser to parse the bytes in 'buffer'
         * starting at the given 'offset'.
         */
        public Parser(byte[] buffer, int offset) {
             this.stringValue = new StringValue();
             this.bytesValue = new BytesValue();
             this.name = new StringValue();
             this.buffer = buffer;
            this.offset = offset;
        }

        /**
         * Parses until an expected field with the given name is found
         * (without considering fields of inner objects).
         * 
         * @throws FormatException 
         *         If a field with the expected name was not found.
         */
        public void field(String name) {
            while (nextField()) {
                if (nameEquals(name)) {
                    return;
                }
            }
            
            throw new FormatException("no field named '" + name + "'");
        }
        
        /**
         * Reads next field, returns true if a field was found and false
         * if end-of-object was reached.
         * If boolean/integer/double/bytes/string was found, the value is also read
         * and is available in one of the fields:
         * field booleanValue, integerValue, doubleValue, bytesValue, stringValue.
         * 
         * @throws IllegalStateException if end-of-object was reached already.
         */
        public boolean nextField() {
            if (state == STATE_ZERO) {
                parseBegin();
            } else if (state == STATE_END_OF_OBJECT) {
                throw new IllegalStateException("reached end-of-object");
            } else if (state == STATE_BEFORE_OBJECT) {
                state = STATE_BEFORE_FIELD;
                while (nextField()) {}
                state = STATE_BEFORE_FIELD;
            } else if (state == STATE_BEFORE_ARRAY) {
                state = STATE_BEFORE_ARRAY_VALUE;
                while (nextArrayValue()) {}         
                state = STATE_BEFORE_FIELD;
            }
            
            if (state != STATE_BEFORE_FIELD) {
                throw new IllegalStateException("not ready to read a field, state: " + state);
            }
            
            byte typeBeforeName = readOne();
            if (typeBeforeName == END) {
                state = STATE_END_OF_OBJECT;
                return false;
            }
            parseFieldName(typeBeforeName);
            
            byte typeBeforeValue = readOne();
            parseValue(typeBeforeValue, STATE_BEFORE_FIELD);
            
            return true;
        }

        public boolean nextArrayValue() {
            if (state == STATE_BEFORE_ARRAY) {
                state = STATE_BEFORE_ARRAY_VALUE;
                while (nextArrayValue()) {}
                state = STATE_BEFORE_ARRAY_VALUE;
            }
            
            if (state == STATE_BEFORE_OBJECT) {
                state = STATE_BEFORE_FIELD;
                while (nextField()) {}
                state = STATE_BEFORE_ARRAY_VALUE;
            }
        
            if (state != STATE_BEFORE_ARRAY_VALUE) {
                throw new IllegalStateException("not before array value, " + state);
            }
        
            byte typeByte = readOne();
            if (typeByte == END_ARRAY) {
                state = STATE_END_OF_ARRAY;
                return false;
            }
        
            parseValue(typeByte, STATE_BEFORE_ARRAY_VALUE);
            return true;
        }

        /**
         * Checks whether current field name equals a provided one.
         */
        public boolean nameEquals(StringValue name) {
            return this.getName() == null ? false : this.getName().equals(name);
        }

        /**
         * Checks whether current field name equals the provided name.
         * Note, this method does allocate memory. Use nameEquals(StringValue)
         * or nameEquals(byte[]) for highest performance.
         */
        public boolean nameEquals(String name) {
            return this.name.toString().equals(name);
        }

        public void goIntoObject() {
            if (state != STATE_BEFORE_OBJECT) {
                throw new IllegalStateException("unexpected parser state, not an object field");
            }
            state = STATE_BEFORE_FIELD;
        }

        public void goIntoArray() {
            if (state != STATE_BEFORE_ARRAY) {
                throw new IllegalStateException("unexpected parser state, not an array field");
            }
            state = STATE_BEFORE_ARRAY_VALUE;
        }

        public void goUpToObject() {
            if (state == STATE_BEFORE_ARRAY_VALUE) {
                while (nextArrayValue()) {}
            }
            
            if (state == STATE_BEFORE_FIELD) {
                while (nextField()) {}
            }
            
            if (state != STATE_END_OF_OBJECT && state != STATE_END_OF_ARRAY) {
                throw new IllegalStateException("unexpected parser state, " + state);
            }
            
            state = STATE_BEFORE_FIELD;
        }

        public void goUpToArray() {
            if (state == STATE_BEFORE_ARRAY_VALUE) {
                while (nextArrayValue()) {}
            }
            
            if (state == STATE_BEFORE_FIELD) {
                while (nextField()) {}
            }
            
            if (state != STATE_END_OF_OBJECT && state != STATE_END_OF_ARRAY) {
                throw new IllegalStateException("unexpected parser state, " + state);
            }
            
            state = STATE_BEFORE_ARRAY_VALUE;
        }

        /** Returns the type of the last value parsed. */
        public final ValueType getType() {
            return type;
        }

        /** Returns the name of the last field parsed. */
        public final StringValue getName() {
            return name;
        }

        /** Returns the last boolean value parsed. */
        public final boolean getBoolean() {
            return booleanValue;
        }

        /** Returns the last integer value parsed. */
        public final long getInteger() {
            return integerValue;
        }

        /** Returns the last double value parsed. */
        public final double getDouble() {
            return doubleValue;
        }

        /** Returns the last string value parsed. */
        public final StringValue getString() {
            return stringValue;
        }

        /** Returns the last bytes value parsed. */
        public final BytesValue getBytes() {
            return bytesValue;
        }

        private void parseValue(byte typeByte, int afterValueState) {
            switch (typeByte) {
            case BEGIN:
                type = ValueType.OBJECT;
                state = STATE_BEFORE_OBJECT;
                break;
            case BEGIN_ARRAY:
                type = ValueType.ARRAY;
                state = STATE_BEFORE_ARRAY;
                break;
            case FALSE:
            case TRUE:
                type = ValueType.BOOLEAN;
                booleanValue = typeByte == TRUE;
                state = afterValueState;
                break;
            case DOUBLE:
                type = ValueType.DOUBLE;
                parseDouble();
                state = afterValueState;
                break;
            case INTEGER1:
            case INTEGER2:
            case INTEGER4:
            case INTEGER8:
                type = ValueType.INTEGER;
                integerValue = parseInteger(typeByte);            
                state = afterValueState;
                break;
            case STRING1:
            case STRING2:
            case STRING4:
                type = ValueType.STRING;
                parseString(typeByte, getString());
                state = afterValueState;
                break;
            case BYTES1:
            case BYTES2:
            case BYTES4:
                type = ValueType.BYTES;
                parseBytes(typeByte);
                state = afterValueState;
                break;
            default:
                throw new FormatException("Unexpected type byte: " + typeByte + ".");
            }
        }

        private void parseFieldName(byte typeBeforeName) {
            switch (typeBeforeName) {
            case STRING1:
            case STRING2:
            case STRING4:
                parseString(typeBeforeName, getName());
                break;
            default:
                throw new FormatException("unexpected type: " + typeBeforeName);
            }
        }
        
        private void parseDouble() {
            doubleValue = Util.bytesToDoubleLE(buffer, offset);
            offset += 8;
        }
        
        private void parseBegin() {
            int type = readOne();
            if (type != BEGIN) {
                throw new FormatException("Expected BEGIN, got " + type + ".");
            }
            state = STATE_BEFORE_FIELD;
        }
        
        private void parseString(byte typeByte, StringValue s) {
            long longLen = parseInteger(typeByte);
            if (longLen < 0) {
                throw new FormatException("Bad string length, " + longLen + ".");
            }
            if (longLen > 10*1000000L) {
                throw new FormatException("String length too big, " + longLen + ".");
            }
            
            int len = (int) longLen;
            if (len < 0) throw new FormatException("Bad len, " + len + ".");
            s.set(buffer, offset, len);
            this.offset += len;
        }
        
        private void parseBytes(byte typeByte) {
            long longLen = parseInteger(typeByte);
            if (longLen < 0) {
                throw new FormatException("Bad length of bytes, " + longLen + ".");
            }
            if (longLen > 50*1000000L) {
                throw new FormatException("Bad length of bytes, " + longLen + ".");
            }
            
            int len = (int) longLen;
            
            if (len < 0) throw new FormatException("Bad len, " + len + ".");
            bytesValue.set(buffer, offset, len);
            this.offset += len;
        }
        
        private long parseInteger(byte typeByte) {
            int intType = typeByte & INT_LENGTH_MASK;
            long integer;
            
            switch (intType) {
            case ONE_BYTE:
                integer = buffer[offset];
                offset++;
                break;
                
            case TWO_BYTES:
                integer = Util.bytesToShortLE(buffer, offset);
                offset += 2;
                break;
                
            case FOUR_BYTES:
                integer = Util.bytesToIntLE(buffer, offset);
                offset += 4;
                break;
                
            case EIGHT_BYTES:
                integer = Util.bytesToLongLE(buffer, offset);
                offset += 8;
                break;
                
            default:
                throw new Error("never happens, intType: " + intType);
            }
            
            return integer;
        }
        
        private final byte readOne() {
            return buffer[offset++];
        }
    }

    /**
     * Thrown to indicate that the parsed bytes do not adhere to the expected 
     * byte format.
     */
    public static class FormatException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public FormatException(String message) { super(message); }
    }
    
    /**
     * Writes Binson tokens to an OutputStream. The writer is low-level and very simple.
     * There is no validation, an instance of this class can generate invalid Binson bytes.
     * Make sure fields are written in alphabetical order. This is required to produce valid Binson.
     */
    public static class Writer {
        private OutputStream out;
        
        public Writer(OutputStream out) {
            this.out = out;
        }
        
        public Writer begin() throws IOException {
            out.write(BEGIN);
            return this;
        }
        
        public Writer end() throws IOException {
            out.write(END);
            return this;
        }
        
        public Writer beginArray() throws IOException {
            out.write(BEGIN_ARRAY);
            return this;
        }
        
        public Writer endArray() throws IOException {
            out.write(END_ARRAY);
            return this;
        }
        
        public Writer bool(boolean value) throws IOException {
            out.write(value == true ? TRUE : FALSE);
            return this;
        }
        
        public Writer integer(long value) throws IOException {
            writeIntegerOrLength(INTEGER1, value);
            return this;
        }
        
        public Writer doubl(double value) throws IOException {
            byte[] bytes = new byte[9];
            bytes[0] = DOUBLE;
            Util.doubleToBytesLE(value, bytes, 1);
            out.write(bytes);
            return this;
        }
        
        public Writer string(String string) throws IOException {
            return string(string.getBytes("UTF-8"));
        }
        
        public Writer string(byte[] utf8Bytes) throws IOException {
            writeIntegerOrLength(STRING1, utf8Bytes.length);
            out.write(utf8Bytes);
            return this;
        }
        
        public Writer bytes(byte[] value) throws IOException {
            writeIntegerOrLength(BYTES1, value.length);
            out.write(value);
            return this;
        }
        
        public Writer name(String name) throws IOException {
            string(name);
            return this;
        }
        
        /** Calls flush() on the OutputStream. */
        public void flush() throws IOException {
            out.flush();
        }
        
        private void writeIntegerOrLength(int baseType, long value) throws IOException {
            int type;
            byte[] buffer;
            
            if (value >= -TWO_TO_7 && value < TWO_TO_7) {
                type = baseType | ONE_BYTE;
                buffer = new byte[1];
                buffer[0] = (byte) value;
            } else if (value >= -TWO_TO_15 && value < TWO_TO_15) {
                type = baseType | TWO_BYTES;
                buffer = new byte[2];
                Util.shortToBytesLE((int) value, buffer, 0);
            } else if (value >= -TWO_TO_31 && value < TWO_TO_31) {
                type = baseType | FOUR_BYTES;
                buffer = new byte[4];
                Util.intToBytesLE((int) value, buffer, 0);
            } else {
                type = baseType | EIGHT_BYTES;
                buffer = new byte[8];
                Util.longToBytesLE(value, buffer, 0);
            }
            
            out.write(type);
            out.write(buffer);
        }
    }
    
    /**
     * A String represented as UTF-8 bytes. Mutable to allow memory reuse.
     */
    public static class StringValue {
        public byte[] buffer;
        public int offset;
        public int size;
        
        public StringValue() {
            set(EMPTY_BYTE_ARRAY, 0, 0);
        }
        
        public void set(byte[] buffer, int offset, int size) {
            this.buffer = buffer;
            this.offset = offset;
            this.size = size;
        }
        
        public StringValue(String s) {
            this.offset = 0;
            
            try {
                this.buffer = s.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new Error(e);
            }
            
            this.size = buffer.length;
        }
        
        public boolean equals(Object that) {
            if (that == null || !(that instanceof StringValue)) {
                return false;
            }
            return equals((StringValue) that);
        }
        
        public boolean equals(StringValue that) {
            if (this.size != that.size) {
                return false;
            }
            
            for (int i = 0; i < this.size; i++) {
                if (this.buffer[this.offset + i] != that.buffer[that.offset + i]) {
                    return false;
                }
            }
            
            return true;
        }
        
        public String toString() {
            try {
                return new String(buffer, offset, size, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new Error(e);
            }
        }
    }
    
    public static class BytesValue {
        public byte[] buffer;
        public int offset;
        public int size;
        
        public BytesValue() {
            set(EMPTY_BYTE_ARRAY, 0, 0);
        }
        
        public void set(byte[] buffer, int offset, int size) {
            this.buffer = buffer;
            this.offset = offset;
            this.size = size;
        }
        
        /**
         * Returns a byte array copy of the bytes value.
         * Allocates memory for it.
         */
        public byte[] toByteArray() {
            byte[] result = new byte[size];
            System.arraycopy(buffer, offset, result, 0, size);
            return result;
        }
    }
    
    private static final class Util {
        private static short bytesToShortLE(byte[] arr, int offset) {
            int result = (arr[offset++] & 0x00ff);
            result |= (arr[offset++] & 0x00ff) << 8;
            return (short) result;
        }
        
        private static int bytesToIntLE(byte[] arr, int offset) {
            int i = offset;
            int result = (arr[i++] & 0x00ff);
            result |= (arr[i++] & 0x00ff) << 8;
            result |= (arr[i++] & 0x00ff) << 16;
            result |= (arr[i] & 0x00ff) << 24;
            return result;
        }
        
        private static long bytesToLongLE(byte[] arr, int offset) {
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
        
        private static double bytesToDoubleLE(byte[] arr, int offset) {
            long myLong = bytesToLongLE(arr, offset);
            return Double.longBitsToDouble(myLong);
        }

        private static void shortToBytesLE(int value, byte[] arr, int offset) {
            int i = offset;
            arr[i++] = (byte) value;
            arr[i++] = (byte) (value >>> 8);
        }

        private static final void intToBytesLE(int value, byte[] arr, int offset) {
            arr[offset++] = (byte) value;
            arr[offset++] = (byte) (value >>> 8);
            arr[offset++] = (byte) (value >>> 16);
            arr[offset] = (byte) (value >>> 24);
        }

        private static final void longToBytesLE(final long value, final byte[] arr, int offset) {
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
        
        private static final void doubleToBytesLE(double value, byte[] arr, int offset) {
            long bits = Double.doubleToRawLongBits(value);
            longToBytesLE(bits, arr, 1);
        }
    }
}
