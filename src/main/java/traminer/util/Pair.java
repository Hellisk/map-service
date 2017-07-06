package traminer.util;

import java.io.Serializable;

/**
 * A wrapper to store a pair of objects (auxiliary object).
 *
 * @author uqdalves
 *
 * @param <T> Type of the first object of the pair.
 * @param <S> Type of the second object of the pair.
 */
@SuppressWarnings("serial")
public class Pair<T, S> implements Serializable {
    /**
     * The two objects to pair
     */
    private final T _1;
    private final S _2;

    /**
     * Creates a new pair of objects.
     *
     * @param _1 The first object of the pair.
     * @param _2 The second object of the pair.
     */
    public Pair(T _1, S _2) {
        this._1 = _1;
        this._2 = _2;
    }

    /**
     * @return The first object of the pair.
     */
    public T _1() {
        return _1;
    }

    /**
     * @return The second object of the pair.
     */
    public S _2() {
        return _2;
    }
}
