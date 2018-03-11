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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_1 == null) ? 0 : _1.hashCode());
        result = prime * result + ((_2 == null) ? 0 : _2.hashCode());
        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Pair other = (Pair) obj;
        if (_1 == null) {
            if (other._1 != null)
                return false;
        } else if (!_1.equals(other._1))
            return false;
        if (_2 == null) {
            return other._2 == null;
        } else return _2.equals(other._2);
    }
}
