package traminer.util;

import traminer.util.math.Decimal;

import java.io.Serializable;

/**
 * Service to compress/decompress a list of elements using Delta encoding.
 *
 * @test Unit test passed.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public final class DeltaEncoder implements Serializable {
    // default error message
    private static final String ERR_MSG = "List of values for "
            + "delta encode/decode must not be null.";

    /**
     * Compress an array of double numbers using delta encoding.
     *
     * @param values The array of numbers to compress.
     * @return A copy of the given array after delta-encoding.
     * @throws NullPointerException If the values list is null.
     */
    public static double[] deltaEncode(double[] values) {
        if (values == null) {
            throw new NullPointerException(ERR_MSG);
        }
        if (values.length <= 1) {
            return values.clone();
        }

        double result[] = new double[values.length];
        Decimal previous = new Decimal(values[0]);
        Decimal current, delta;
        for (int i = 1; i < values.length; i++) {
            current = new Decimal(values[i]);
            delta = current.sub(previous);
            result[i] = delta.value();
            previous = current;
        }
        result[0] = values[0];

        return result;
    }

    /**
     * Compress an array of long numbers using delta encoding.
     *
     * @param values The array of numbers to compress.
     * @return A copy of the given array after delta-encoding.
     * @throws NullPointerException If the values list is null.
     */
    public static long[] deltaEncode(long[] values) {
        if (values == null) {
            throw new NullPointerException(ERR_MSG);
        }
        if (values.length <= 1) {
            return values.clone();
        }

        long result[] = new long[values.length];
        long previous = values[0];
        long current, delta;
        for (int i = 1; i < values.length; i++) {
            current = values[i];
            delta = current - previous;
            result[i] = delta;
            previous = current;
        }
        result[0] = values[0];

        return result;
    }

    /**
     * Compress an array of String numbers using delta encoding.
     *
     * @param values The array of String numbers to compress.
     * @return A copy of the given array after delta-encoding.
     * @throws NullPointerException  If the values list is null.
     * @throws NumberFormatException If any value in the values list
     *                               cannot be parsed to a number.
     */
    public static String[] deltaEncode(String[] values) throws NumberFormatException {
        if (values == null) {
            throw new NullPointerException(ERR_MSG);
        }
        if (values.length <= 1) {
            return values.clone();
        }

        String result[] = new String[values.length];
        Decimal previous = new Decimal(values[0]);
        Decimal current, delta;
        for (int i = 1; i < values.length; i++) {
            current = new Decimal(values[i]);
            delta = current.sub(previous);
            result[i] = delta.toString();
            previous = current;
        }
        result[0] = values[0];

        return result;
    }

    /**
     * Compress an array of String numbers using delta encoding.
     *
     * @param values The array of String numbers to compress.
     * @return A copy of the given array after delta-encoding
     * and parsing the String numbers to double values.
     * @throws NullPointerException  If the values list is null.
     * @throws NumberFormatException If any value in the values list
     *                               cannot be parsed to a number.
     */
    public static double[] deltaEncodeAsDouble(String[] values) throws NumberFormatException {
        if (values == null) {
            throw new NullPointerException(ERR_MSG);
        }
        double result[] = new double[values.length];
        if (values.length <= 1) {
            for (int i = 0; i < values.length; i++) {
                result[i] = Double.parseDouble(values[i]);
            }
            return result;
        }

        Decimal previous = new Decimal(values[0]);
        result[0] = previous.value();
        Decimal current, delta;
        for (int i = 1; i < values.length; i++) {
            current = new Decimal(values[i]);
            delta = current.sub(previous);
            result[i] = delta.value();
            previous = current;
        }

        return result;
    }

    /**
     * Compress an array of String numbers using delta encoding.
     *
     * @param values The array of String numbers to compress.
     * @return A copy of the given array after delta-encoding
     * and parsing the String numbers to Decimal values.
     * @throws NullPointerException  If the values list is null.
     * @throws NumberFormatException If any value in the values list
     *                               cannot be parsed to a number.
     */
    public static Decimal[] deltaEncodeAsDecimal(String[] values) throws NumberFormatException {
        if (values == null) {
            throw new NullPointerException(ERR_MSG);
        }
        Decimal result[] = new Decimal[values.length];
        if (values.length <= 1) {
            for (int i = 0; i < values.length; i++) {
                result[i] = new Decimal(values[i]);
            }
            return result;
        }

        Decimal previous = new Decimal(values[0]);
        result[0] = previous;
        Decimal current, delta;
        for (int i = 1; i < values.length; i++) {
            current = new Decimal(values[i]);
            delta = current.sub(previous);
            result[i] = delta;
            previous = current;
        }

        return result;
    }

    /**
     * Decompress an array of double numbers from delta encoding.
     *
     * @param values The array of numbers to decompress.
     * @return A copy of the given array after delta-decoding.
     * @throws NullPointerException If the values list is null.
     */
    public static double[] deltaDecode(double[] values) {
        if (values == null) {
            throw new NullPointerException(ERR_MSG);
        }
        if (values.length <= 1) {
            return values.clone();
        }

        double result[] = new double[values.length];
        Decimal previous = new Decimal(values[0]);
        Decimal current, delta;
        for (int i = 1; i < values.length; i++) {
            delta = new Decimal(values[i]);
            current = previous.sum(delta);
            result[i] = current.value();
            previous = current;
        }
        result[0] = values[0];

        return result;
    }

    /**
     * Decompress an array of long numbers from delta encoding.
     *
     * @param values The array of numbers to decompress.
     * @return A copy of the given array after delta-decoding.
     *
     * @throws NullPointerException If the values list is null.
     */
    public static long[] deltaDecode(long[] values) {
        if (values == null) {
            throw new NullPointerException(ERR_MSG);
        }
        if (values.length <= 1) {
            return values.clone();
        }

        long result[] = new long[values.length];
        long previous = values[0];
        long current, delta;
        for (int i = 1; i < values.length; i++) {
            delta = values[i];
            current = previous + delta;
            result[i] = current;
            previous = current;
        }
        result[0] = values[0];

        return result;
    }

    /**
     * Decompress an array of String numbers from delta encoding.
     *
     * @param values The array of String numbers to decompress.
     * @return A copy of the given array after delta-decoding.
     * @throws NullPointerException  If the values list is null.
     * @throws NumberFormatException If any value in the values list
     *                               cannot be parsed to a number.
     */
    public static String[] deltaDecode(String[] values) throws NumberFormatException {
        if (values == null) {
            throw new NullPointerException(ERR_MSG);
        }
        if (values.length <= 1) {
            return values.clone();
        }

        String result[] = new String[values.length];
        Decimal previous = new Decimal(values[0]);
        Decimal current, delta;
        for (int i = 1; i < values.length; i++) {
            delta = new Decimal(values[i]);
            current = previous.sum(delta);
            result[i] = current.toString();
            previous = current;
        }
        result[0] = values[0];

        return result;
    }

    /**
     * Decompress an array of String numbers from delta encoding.
     *
     * @param values The array of String numbers to decompress.
     * @return A copy of the given array after delta-decoding
     * and parsing the String numbers to double values.
     * @throws NullPointerException  If the values list is null.
     * @throws NumberFormatException If any value in the values list
     *                               cannot be parsed to a number.
     */
    public static double[] deltaDecodeAsDouble(String[] values) throws NumberFormatException {
        if (values == null) {
            throw new NullPointerException(ERR_MSG);
        }
        double result[] = new double[values.length];
        if (values.length <= 1) {
            for (int i = 0; i < values.length; i++) {
                result[i] = Double.parseDouble(values[i]);
            }
            return result;
        }

        Decimal previous = new Decimal(values[0]);
        result[0] = previous.value();
        Decimal current, delta;
        for (int i = 1; i < values.length; i++) {
            delta = new Decimal(values[i]);
            current = previous.sum(delta);
            result[i] = current.value();
            previous = current;
        }

        return result;
    }

    /**
     * Decompress an array of String numbers from delta encoding.
     *
     * @param values The array of String numbers to decompress.
     * @return A copy of the given array after delta-decoding
     * and parsing the String numbers to Decimal values.
     * @throws NullPointerException  If the values list is null.
     * @throws NumberFormatException If any value in the values list
     *                               cannot be parsed to a number.
     */
    public static Decimal[] deltaDecodeAsDecimal(String[] values) throws NumberFormatException {
        if (values == null) {
            throw new NullPointerException(ERR_MSG);
        }
        Decimal result[] = new Decimal[values.length];
        if (values.length <= 1) {
            for (int i = 0; i < values.length; i++) {
                result[i] = new Decimal(values[i]);
            }
            return result;
        }

        Decimal previous = new Decimal(values[0]);
        result[0] = previous;
        Decimal current, delta;
        for (int i = 1; i < values.length; i++) {
            delta = new Decimal(values[i]);
            current = previous.sum(delta);
            result[i] = current;
            previous = current;
        }

        return result;
    }
}
