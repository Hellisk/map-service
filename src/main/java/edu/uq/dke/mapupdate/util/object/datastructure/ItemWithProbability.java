package edu.uq.dke.mapupdate.util.object.datastructure;

import java.util.Comparator;

/**
 * A structure designed for sorting an item according to its probability
 *
 * @param <S> candidate state
 * @author uqpchao
 */
@SuppressWarnings("serial")
public class ItemWithProbability<S> implements Comparable<ItemWithProbability<S>> {

    private final S item;     // item
    private final double probability;

    /**
     * Creates a new item with probability.
     *
     * @param item        The item
     * @param probability The corresponding probability
     */
    public ItemWithProbability(S item, double probability) {
        this.item = item;
        this.probability = probability;
    }

    /**
     * @return The item
     */
    public S getItem() {
        return this.item;
    }

    /**
     * @return The second object of the pair.
     */
    public double getProbability() {
        return this.probability;
    }

    @Override
    // reverse the comparison for max heap
    public int compareTo(ItemWithProbability o) {
        return Double.compare(o.getProbability(), this.probability);
    }
}