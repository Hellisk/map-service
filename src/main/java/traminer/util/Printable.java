package traminer.util;

import java.io.Serializable;

/**
 * Interface for printing information in the system
 * standard output/console in a painless manner.
 *
 * @author uqdalves
 */
public interface Printable extends Serializable {
    default void print(Object s) {
        System.out.print(s.toString());
    }

    default void println(Object s) {
        System.out.println(s.toString());
    }

    default void println() {
        System.out.println();
    }

    default void printerr(Object s) {
        System.err.println(s.toString());
    }
}
