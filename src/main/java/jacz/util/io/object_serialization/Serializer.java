package jacz.util.io.object_serialization;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class contains utility methods for transforming different types of objects into byte arrays, and vice-versa
 */
public class Serializer {

    /**
     * Adds several byte arrays into a single byte array (admits null values)
     *
     * @return a byte array result of the concatenation of the argument byte arrays
     */
    public static byte[] addArrays(byte[]... arrays) {
        int totalLength = 0;
        int partialLength = 0;
        for (byte[] oneArray : arrays) {
            if (oneArray != null) {
                totalLength += oneArray.length;
            }
        }
        byte[] result = new byte[totalLength];
        for (byte[] oneArray : arrays) {
            if (oneArray != null) {
                System.arraycopy(oneArray, 0, result, partialLength, oneArray.length);
                partialLength += oneArray.length;
            }
        }
        return result;
    }

    /**
     * Serializes a string object into a byte array
     *
     * @param o Object to serialize
     * @return byte array containing the object
     */
    public static byte[] serializeObject(Serializable o) {
        byte[] objectData = serializeObjectWithoutLengthHeader(o);
        return addArrays(Serializer.serialize(objectData.length), objectData);
    }


    private static byte[] serializeObjectWithoutLengthHeader(Serializable o) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(o);
            so.flush();
            so.close();
            bo.close();
            return bo.toByteArray();
        } catch (IOException e) {
            // this exception cannot happen, since the output stream is in memory, not in the file system
            e.printStackTrace();
            return new byte[0];
        }
    }


    /**
     * Serializes an object into a String
     *
     * @param o Object to serialize
     * @return String containing the object
     */
    public static String serializeObjectToString(Serializable o) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(bo);
        so.writeObject(o);
        so.flush();
        // This encoding induces a bijection between byte[] and String (unlike UTF-8)
        return bo.toString("ISO-8859-1");
    }

    /**
     * Serializes a list of objects into a String without using the native Java serialization API. The process builds a string as follows:
     * First, the number of elements and a separator are written. Second, for each element, their length and a separator are written, followed by
     * the element itself
     * <p/>
     * This is not an efficient serialization, but is readable and avoids unexpected characters to appear
     *
     * @param list Object list to serialize
     * @return String containing the object
     */
    public static String serializeListToReadableString(List<?> list, String pre, String post) {
        if (pre == null || pre.isEmpty()) {
            throw new IllegalArgumentException("pre cannot be null or empty");
        }
        if (post == null) {
            post = "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(list.size()).append(pre);
        for (Object o : list) {
            if (o != null) {
                stringBuilder.append(o.toString().length()).append(pre).append(o.toString()).append(post);
            } else {
                stringBuilder.append(-1).append(pre).append(post);
            }
        }
        return stringBuilder.toString();
    }

    public static byte[] serializeListToByteArray(List<?> list, char separator) {
        return serialize(serializeListToReadableString(list, Character.toString(separator), null));
    }

    /**
     * Serializes a string object into a byte array. The byte array contains 4 bytes for indicating the length of the string, and the necessary
     * bytes for storing the characters of the string
     *
     * @param str String to serialize
     * @return byte array containing the string object
     */
    public static byte[] serialize(String str) {
        if (str == null) {
            // null values are serialized with a 4-byte array containing a -1
            return Serializer.serialize(-1);
        } else {
            byte[] strBytes = str.getBytes();
            return addArrays(serialize(strBytes.length), strBytes);
        }
    }

    /**
     * Serializes a Boolean object in a 1 byte array
     *
     * @param b the Boolean object to serialize
     * @return a byte array of size 1 with the value of the given Boolean
     */
    public static byte[] serialize(Boolean b) {
        if (b == null) {
            // null values are serialized with 1 byte containing a -1
            return serialize((byte) -1);
        } else if (b) {
            return serialize((byte) 1);
        } else {
            return serialize((byte) 0);
        }
    }

    /**
     * Serializes a boolean value in a 1 byte array
     *
     * @param b the boolean value to serialize
     * @return a byte array of size 1 with the value of the given boolean
     */
    public static byte[] serialize(boolean b) {
        return serialize(Boolean.valueOf(b));
    }

    /**
     * Serializes a Byte object in a 1 or 2 byte array
     *
     * @param b the Byte object to serialize
     * @return a byte array of size 1 or 2 with the value of the given byte
     */
    public static byte[] serialize(Byte b) {
        if (b != null && b != Byte.MIN_VALUE) {
            return serialize(b.byteValue());
        } else if (b != null) {
            // b is MIN_VALUE -> we add a byte containing a true
            return addArrays(serialize(b.byteValue()), serialize(true));
        } else {
            // b is null -> we codify MIN_VALUE and add a byte containing a false
            return addArrays(serialize(Byte.MIN_VALUE), serialize(false));
        }
    }

    /**
     * Serializes a byte value in a 1 byte array
     *
     * @param b the byte value to serialize
     * @return a byte array of size 1 with the value of the given byte
     */
    public static byte[] serialize(byte b) {
        return serializeNumber((long) b, 1);
    }

    /**
     * Serializes a Short object in a 2 or 3 byte array
     *
     * @param s the Byte object to serialize
     * @return a byte array of size 2 or 3 with the value of the given short
     */
    public static byte[] serialize(Short s) {
        if (s != null && s != Short.MIN_VALUE) {
            return serialize(s.shortValue());
        } else if (s != null) {
            // b is MIN_VALUE -> we add a byte containing a true
            return addArrays(serialize(s.shortValue()), serialize(true));
        } else {
            // b is null -> we codify MIN_VALUE and add a byte containing a false
            return addArrays(serialize(Short.MIN_VALUE), serialize(false));
        }
    }

    /**
     * Serializes a short value in a 2 byte array
     *
     * @param s the short value to serialize
     * @return a byte array of size 2 with the value of the given short
     */
    public static byte[] serialize(short s) {
        return serializeNumber((long) s, 2);
    }

    /**
     * Serializes an Integer object in a 4 or 5 byte array
     *
     * @param i the Integer object to serialize
     * @return a byte array of size 4 or 5 with the value of the given integer
     */
    public static byte[] serialize(Integer i) {
        if (i != null && i != Integer.MIN_VALUE) {
            return serialize(i.intValue());
        } else if (i != null) {
            // b is MIN_VALUE -> we add a byte containing a true
            return addArrays(serialize(i.intValue()), serialize(true));
        } else {
            // b is null -> we codify MIN_VALUE and add a byte containing a false
            return addArrays(serialize(Integer.MIN_VALUE), serialize(false));
        }
    }

    /**
     * Serializes an int value in a 4 byte array
     *
     * @param i the int value to serialize
     * @return a byte array of size 4 with the value of the given integer
     */
    public static byte[] serialize(int i) {
        return serializeNumber((long) i, 4);
    }

    /**
     * Serializes a Long object in an 8 or 9 byte array
     *
     * @param l the Long object to serialize
     * @return a byte array of size 8 or 9 with the value of the given long
     */
    public static byte[] serialize(Long l) {
        if (l != null && l != Long.MIN_VALUE) {
            return serialize(l.longValue());
        } else if (l != null) {
            // b is MIN_VALUE -> we add a byte containing a true
            return addArrays(serialize(l.longValue()), serialize(true));
        } else {
            // b is null -> we codify MIN_VALUE and add a byte containing a false
            return addArrays(serialize(Long.MIN_VALUE), serialize(false));
        }
    }

    /**
     * Serializes a long value in an 8 byte array
     *
     * @param l the long value to serialize
     * @return a byte array of size 8 with the value of the given long
     */
    public static byte[] serialize(long l) {
        return serializeNumber(l, 8);
    }

    /**
     * Serializes a Float object in a 4 or 5 byte array
     *
     * @param f the Float object to serialize
     * @return a byte array of size 4 or 5 with the value of the given long
     */
    public static byte[] serialize(Float f) {
        if (f == null) {
            return serialize((Integer) null);
        } else {
            return serialize(Float.floatToIntBits(f));
        }
    }

    /**
     * Serializes a float value in a 4 byte array
     *
     * @param f the float object to serialize
     * @return a byte array of size 4 with the value of the given long
     */
    public static byte[] serialize(float f) {
        return serialize(Float.floatToIntBits(f));
    }

    /**
     * Serializes a Double object in an 8 or 9 byte array
     *
     * @param d the Double object to serialize
     * @return a byte array of size 8 or 9 with the value of the given long
     */
    public static byte[] serialize(Double d) {
        if (d == null) {
            return serialize((Long) null);
        } else {
            return serialize(Double.doubleToLongBits(d));
        }
    }

    /**
     * Serializes a double value in an 8 byte array
     *
     * @param d the double value to serialize
     * @return a byte array of size 8 with the value of the given long
     */
    public static byte[] serialize(double d) {
        return serialize(Double.doubleToLongBits(d));
    }

    public static <T extends Enum<T>> byte[] serialize(Enum<T> e) {
        return Serializer.serialize(e.ordinal());
    }

    private static byte[] serializeNumber(long number, int byteCount) {
        String hex = Long.toHexString(number);
        hex = keepHexDigits(hex, byteCount * 2);
        byte[] data = new byte[byteCount];
        for (int j = 0; j < byteCount; j++) {
            data[j] = (byte) Short.parseShort(hex.substring(2 * j, 2 * j + 2), 16);
        }
        return data;
    }

    public static byte[] serialize(byte[] bytes) {
        if (bytes != null) {
            return addArrays(serialize(bytes.length), bytes);
        } else {
            return serialize((Integer) null);
        }
    }

    public static byte[] deserializeRest(byte[] data, MutableOffset offset) {
        byte[] rest = new byte[data.length - offset.value()];
        System.arraycopy(data, offset.value(), rest, 0, rest.length);
        offset.add(rest.length);
        return rest;
    }

    public static Object deserializeObject(byte[] data, MutableOffset offset) throws ClassNotFoundException {
        int objectLength = deserializeIntValue(data, offset);
        byte[] objectData = Arrays.copyOfRange(data, offset.value(), offset.value() + objectLength);
        Object o = deserializeObjectWithoutLengthHeader(objectData);
        offset.add(objectLength);
        return o;
    }

    private static Object deserializeObjectWithoutLengthHeader(byte[] data) throws ClassNotFoundException {
        try {
            ByteArrayInputStream bi = new ByteArrayInputStream(data);
            ObjectInputStream si = new ObjectInputStream(bi);
            return si.readObject();
        } catch (IOException e) {
            // this exception cannot happen, since the input stream is in memory, not in the file system. Nevertheless, throw the exception
            e.printStackTrace();
            return null;
        }
    }

    public static Object deserializeObject(String s) throws IOException, ClassNotFoundException {
        // This encoding induces a bijection between byte[] and String (unlike UTF-8)
        byte b[] = s.getBytes("ISO-8859-1");
        ByteArrayInputStream bi = new ByteArrayInputStream(b);
        ObjectInputStream si = new ObjectInputStream(bi);
        return si.readObject();
    }

    public static List<String> deserializeListFromReadableString(String s, String pre, String post) throws ParseException {
        if (pre == null || pre.isEmpty()) {
            throw new IllegalArgumentException("pre cannot be null or empty");
        }
        if (post == null) {
            post = "";
        }
        int offset = 0;
        int index = s.indexOf(pre);
        try {
            int elementCount = Integer.parseInt(s.substring(0, index));
            index += pre.length();
            List<String> list = new ArrayList<>();
            while (list.size() < elementCount) {
                offset = index;
                index = s.indexOf(pre, offset);
                int elementLength = Integer.parseInt(s.substring(offset, index));
                if (elementLength >= 0) {
                    String element = s.substring(index + pre.length(), index + pre.length() + elementLength);
                    list.add(element);
                    index += pre.length() + elementLength + post.length();
                } else {
                    // element is null
                    list.add(null);
                    index += pre.length() + post.length();
                }
            }
            return list;
        } catch (Exception e) {
            throw new ParseException("Error parsing the string", offset);
        }
    }

    public static List<String> deserializeListFromByteArray(byte[] data, MutableOffset offset, char separator) throws ParseException {
        return deserializeListFromReadableString(deserializeString(data, offset), Character.toString(separator), null);
    }

    public static String deserializeString(byte[] data, MutableOffset offset) {
        int strLen = deserializeIntValue(data, offset);
        if (strLen < 0) {
            return null;
        } else {
            byte[] strBytes = new byte[strLen];
            System.arraycopy(data, offset.value(), strBytes, 0, strLen);
            offset.add(strLen);
            return new String(strBytes);
        }
    }

    public static Boolean deserializeBoolean(byte[] data, MutableOffset offset) {
        byte b = deserializeByteValue(data, offset);
        if (b == -1) {
            return null;
        } else {
            return b != 0;
        }
    }

    public static boolean deserializeBooleanValue(byte[] data, MutableOffset offset) {
        return deserializeByteValue(data, offset) != 0;
    }

    public static Byte deserializeByte(byte[] data, MutableOffset offset) {
        byte b = deserializeByteValue(data, offset);
        if (b == Byte.MIN_VALUE) {
            Boolean isMinValue = deserializeBooleanValue(data, offset);
            if (isMinValue) {
                return b;
            } else {
                return null;
            }
        } else {
            return b;
        }
    }

    public static Byte deserializeByteValue(byte[] data, MutableOffset offset) {
        return deserializeNumber(data, 1, offset).byteValue();
    }

    public static Short deserializeShort(byte[] data, MutableOffset offset) {
        short s = deserializeShortValue(data, offset);
        if (s == Short.MIN_VALUE) {
            Boolean isMinValue = deserializeBooleanValue(data, offset);
            if (isMinValue) {
                return s;
            } else {
                return null;
            }
        } else {
            return s;
        }
    }

    public static short deserializeShortValue(byte[] data, MutableOffset offset) {
        return deserializeNumber(data, 2, offset).shortValue();
    }

    public static Integer deserializeInt(byte[] data, MutableOffset offset) {
        int i = deserializeIntValue(data, offset);
        if (i == Integer.MIN_VALUE) {
            Boolean isMinValue = deserializeBooleanValue(data, offset);
            if (isMinValue) {
                return i;
            } else {
                return null;
            }
        } else {
            return i;
        }
    }

    public static int deserializeIntValue(byte[] data, MutableOffset offset) {
        return deserializeNumber(data, 4, offset).intValue();
    }

    public static Long deserializeLong(byte[] data, MutableOffset offset) {
        long l = deserializeLongValue(data, offset);
        if (l == Long.MIN_VALUE) {
            Boolean isMinValue = deserializeBooleanValue(data, offset);
            if (isMinValue) {
                return l;
            } else {
                return null;
            }
        } else {
            return l;
        }
    }

    public static long deserializeLongValue(byte[] data, MutableOffset offset) {
        return deserializeNumber(data, 8, offset);
    }

    public static Float deserializeFloat(byte[] data, MutableOffset offset) {
        Integer floatBits = deserializeInt(data, offset);
        if (floatBits != null) {
            return Float.intBitsToFloat(floatBits);
        } else {
            return null;
        }
    }

    public static float deserializeFloatValue(byte[] data, MutableOffset offset) {
        return Float.intBitsToFloat(deserializeIntValue(data, offset));
    }

    public static Double deserializeDouble(byte[] data, MutableOffset offset) {
        Long doubleBits = deserializeLong(data, offset);
        if (doubleBits != null) {
            return Double.longBitsToDouble(doubleBits);
        } else {
            return null;
        }
    }

    public static double deserializeDoubleValue(byte[] data, MutableOffset offset) {
        return Double.longBitsToDouble(deserializeLongValue(data, offset));
    }

    public static <E extends Enum<E>> E deserializeEnum(Class<E> enumType, byte[] data, MutableOffset offset) {
        int ordinal = Serializer.deserializeIntValue(data, offset);
        for (Enum<E> value : enumType.getEnumConstants()) {
            if (value.ordinal() == ordinal) {
                //noinspection unchecked
                return (E) value;
            }
        }
        return null;
    }

    private static Long deserializeNumber(byte[] data, int byteCount, MutableOffset offset) {
        String hex = "";
        for (int i = 0; i < byteCount; i++) {
            hex = hex + addZerosLeft(byteToHex(data[i + offset.value()]), 2);
        }
        offset.add(byteCount);
        return Long.parseLong(hex, 16);
    }

    public static byte[] deserializeBytes(byte[] data, MutableOffset offset) {
        Integer bytesLen = deserializeInt(data, offset);
        if (bytesLen != null) {
            byte[] bytes = new byte[bytesLen];
            System.arraycopy(data, offset.value(), bytes, 0, bytesLen);
            offset.add(bytesLen);
            return bytes;
        } else {
            return null;
        }
    }

    private static String byteToHex(byte b) {
        String intHex = Integer.toHexString((int) b);
        if (intHex.length() > 2) {
            intHex = intHex.substring(intHex.length() - 2, intHex.length());
        }
        return intHex;
    }

    private static String keepHexDigits(String hex, int digitCount) {
        if (hex.length() > digitCount) {
            return hex.substring(hex.length() - digitCount, hex.length());
        } else {
            return addZerosLeft(hex, digitCount);
        }
    }

    private static String addZerosLeft(String str, int expectedLength) {
        while (str.length() < expectedLength) {
            str = "0" + str;
        }
        return str;
    }
}
