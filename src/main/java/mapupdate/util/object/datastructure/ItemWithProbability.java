package mapupdate.util.object.datastructure;

/**
 * A structure designed for sorting an item according to its probability
 *
 * @param <S> candidate state
 * @author uqpchao
 */
@SuppressWarnings("serial")
public class ItemWithProbability<S, D> implements Comparable<ItemWithProbability<S, D>> {

    private final S item;     // item
    private final double probability;
    private final double secondaryValue;
    private final D thirdValue;

    /**
     * Creates a new item with probability.
     *
     * @param item        The item
     * @param probability The corresponding probability
     */
    public ItemWithProbability(S item, double probability, double secondValue, D thirdValue) {
        this.item = item;
        this.probability = probability;
        this.secondaryValue = secondValue;
        this.thirdValue = thirdValue;
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

    private double getSecondaryValue() {
        return this.secondaryValue;
    }

    @Override
    // reverse the comparison for max heap
    public int compareTo(ItemWithProbability o) {
        int result = Double.compare(o.getProbability(), this.probability);
        if (result != 0)
            return result;
        else {
            int secondResult = Double.compare(this.secondaryValue, o.secondaryValue);
            if (secondResult != 0)
                return secondResult;
            else {
                int thirdResult = Integer.compare(this.thirdValue.hashCode(), o.thirdValue.hashCode());
                if (thirdResult != 0)
                    return thirdResult;
                else {
                    System.out.println("WARNING! The third result is also the same!");
                    return Integer.compare(o.item.hashCode(), this.item.hashCode());
                }
            }
        }
    }
}