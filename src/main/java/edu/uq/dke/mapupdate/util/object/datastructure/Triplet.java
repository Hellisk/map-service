package edu.uq.dke.mapupdate.util.object.datastructure;

import java.io.Serializable;

/**
 * A wrapper to store a triplet of objects (auxiliary object).
 *
 * @param <T> Type of the first object of the triplet.
 * @param <S> Type of the second object of the triplet.
 * @param <R> Type of the third object of the triplet.
 * @author uqpchao
 */
@SuppressWarnings("serial")
public class Triplet<T, S, R> implements Serializable {
    /**
     * The three objects to combine
     */
    private final T _1;
    private final S _2;
    private final R _3;

    /**
     * Creates a new triplet of objects.
     *
     * @param _1 The first object of the triplet.
     * @param _2 The second object of the triplet.
     * @param _3 The third object of the triplet.
     */
    public Triplet(T _1, S _2, R _3) {
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
    }

    /**
     * @return The first object of the triplet.
     */
    public T _1() {
        return _1;
    }

    /**
     * @return The second object of the triplet.
     */
    public S _2() {
        return _2;
    }

    /**
     * @return The third object of the triplet.
     */
    public R _3() {
        return _3;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_1 == null) ? 0 : _1.hashCode());
        result = prime * result + ((_2 == null) ? 0 : _2.hashCode());
        result = prime * result + ((_3 == null) ? 0 : _3.hashCode());
        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Triplet other = (Triplet) obj;
        if (_1 == null) {
            if (other._1 != null)
                return false;
        } else if (!_1.equals(other._1))
            return false;
        if (_2 == null) {
            return other._2 == null;
        } else if (!_2.equals(other._2))
            return false;
        if (_3 == null) {
            return other._3 == null;
        } else return _3.equals(other._3);
    }
}