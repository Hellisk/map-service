package traminer.util;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Service to compress/de-compress a list of elements using Delta encoding.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public final class DeltaEncoder implements Serializable {
    private static final String ERR_MSG =
            "List for delta encode/decode must not be empty nor null.";

    /**
     * Compress an array of double using delta encoding.
     *
     * @throws IllegalArgumentException if the values list is either empty or null.
     */
    public static double[] deltaEncode(double[] values) {
        if (values == null || values.length <= 0) {
            throw new IllegalArgumentException(ERR_MSG);
        }
        if (values.length == 1)
            return values;

        double last = values[0];
        double current, delta;
        for (int i = 1; i < values.length; i++) {
            current = values[i];
            delta = current - last;
            values[i] = delta;
            last = current;
        }

        return values;
    }

    /**
     * Compress an array of long using delta encoding.
     *
     * @throws IllegalArgumentException if the values list is either empty or null.
     */
    public static long[] deltaEncode(long[] values) {
        if (values == null || values.length <= 0) {
            throw new IllegalArgumentException(ERR_MSG);
        }
        if (values.length == 1)
            return values;

        long last = values[0];
        long current, delta;
        for (int i = 1; i < values.length; i++) {
            current = values[i];
            delta = current - last;
            values[i] = delta;
            last = current;
        }
        return values;
    }

    /**
     * Compress an array of numbers represented as a String using delta
     * encoding.
     * <p>
     * Note: Values in the string array must be able to be parsed to numbers.
     *
     * @return Return the delta compressed array of values.
     * @throws IllegalArgumentException if the values list is either empty or null.
     */
    public static String[] deltaEncode(String[] values) {
        if (values == null || values.length <= 0) {
            throw new IllegalArgumentException(ERR_MSG);
        }
        if (values.length == 1)
            return values;

        BigDecimal last = new BigDecimal(values[0]);
        BigDecimal current, delta;
        for (int i = 1; i < values.length; i++) {
            current = new BigDecimal(values[i]);
            delta = current.subtract(last);
            values[i] = delta.toString();
            last = current;
        }
        return values;
    }

    /**
     * Compress an array of byte values using delta encoding.
     *
     * @return Return the delta compressed array of bytes.
     * @throws IllegalArgumentException if the values list is either empty or null.
     */
    public static byte[] deltaEncode(byte[] values) {
        if (values == null || values.length <= 0) {
            throw new IllegalArgumentException(ERR_MSG);
        }
        if (values.length == 1)
            return values;

        byte last = values[0];
        byte current, delta;
        for (int i = 1; i < values.length; i++) {
            current = values[i];
            delta = (byte) (current - last);
            values[i] = delta;
            last = current;
        }
        return values;
    }

    /**
     * Compress an array of numbers represented as a String using delta
     * encoding.
     * <p>
     * Note: Values in the string array must be able to be parsed to double.
     *
     * @return The delta compressed array of values as an array of double.
     * @throws IllegalArgumentException if the values list is either empty or null.
     */
    public static double[] deltaEncodeAsDouble(String[] values) {
        if (values == null || values.length <= 0) {
            throw new IllegalArgumentException(ERR_MSG);
        }
        BigDecimal last = new BigDecimal(values[0]);
        double[] result = new double[values.length];
        result[0] = last.doubleValue();
        if (values.length == 1)
            return result;

        BigDecimal current, delta;
        for (int i = 1; i < values.length; i++) {
            current = new BigDecimal(values[i]);
            delta = current.subtract(last);
            result[i] = delta.doubleValue();
            last = current;
        }
        return result;
    }

    /**
     * De-compress an array of doubles using delta decoding.
     *
     * @throws IllegalArgumentException if the values list is either empty or null.
     */
    public static double[] deltaDecode(double[] values) {
        if (values == null || values.length <= 0) {
            throw new IllegalArgumentException(ERR_MSG);
        }
        if (values.length == 1)
            return values;

        double last = 0.0;
        double current, delta;
        for (int i = 0; i < values.length; i++) {
            delta = values[i];
            current = last + delta;
            values[i] = current;
            last = current;
        }
        return values;
    }

    /**
     * De-compress an array of longs using delta decoding.
     *
     * @throws IllegalArgumentException if the values list is either empty or null.
     */
    public static long[] deltaDecode(long[] values) {
        if (values == null || values.length <= 0) {
            throw new IllegalArgumentException(ERR_MSG);
        }
        if (values.length == 1)
            return values;

        long last = 0;
        long current, delta;
        for (int i = 0; i < values.length; i++) {
            delta = values[i];
            current = last + delta;
            values[i] = current;
            last = current;
        }
        return values;
    }

    /**
     * Decompress an array of number represented as a String using delta
     * decoding.
     * <p>
     * Note: Values in the string array must be able to be parsed to double.
     *
     * @return Return the de-compressed array of values.
     * @throws IllegalArgumentException if the values list is either empty or null.
     */
    public static String[] deltaDecode(String[] values) {
        if (values == null || values.length <= 0) {
            throw new IllegalArgumentException(ERR_MSG);
        }
        if (values.length == 1)
            return values;

        BigDecimal last = new BigDecimal(0);
        BigDecimal current, delta;
        for (int i = 0; i < values.length; i++) {
            delta = new BigDecimal(values[i]);
            current = last.add(delta);
            values[i] = current.toString();
            last = current;
        }
        return values;
    }

    /**
     * De-compress an array of byte values using delta dencoding.
     *
     * @return Return the delta de-compressed array of bytes.
     * @throws IllegalArgumentException if the values list is either empty or null.
     */
    public static byte[] deltaDecode(byte[] values) {
        if (values == null || values.length <= 0) {
            throw new IllegalArgumentException(ERR_MSG);
        }
        if (values.length == 1)
            return values;

        byte last = 0;
        byte delta, current;
        for (int i = 0; i < values.length; i++) {
            delta = values[i];
            current = (byte) (last + delta);
            values[i] = current;
            last = current;
        }
        return values;
    }

    /**
     * De-compress an array of number represented as a String using delta
     * decoding.
     * <p>
     * Note: Values in the string array must be able to be parsed to double.
     *
     * @return The de-compressed array of values as an array of double.
     * @throws IllegalArgumentException if the values list is either empty or null.
     */
    public static double[] deltaDecodeAsDouble(String[] values) {
        if (values == null || values.length <= 0) {
            throw new IllegalArgumentException(ERR_MSG);
        }
        double[] result = new double[values.length];
        result[0] = new BigDecimal(values[0]).doubleValue();
        if (values.length == 1)
            return result;

        BigDecimal last = new BigDecimal(0);
        BigDecimal current, delta;
        for (int i = 0; i < values.length; i++) {
            delta = new BigDecimal(values[i]);
            current = last.add(delta);
            result[i] = current.doubleValue();
            last = current;
        }
        return result;
    }
}
