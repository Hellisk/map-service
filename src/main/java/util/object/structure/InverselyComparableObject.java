package util.object.structure;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * A structure designed for inversely sorting objects according to their probabilities.
 * <p>
 * The object are firstly compared based on their probability list <tt>probabilities</tt>. The <tt>comparableValue</tt> is only used when
 * the value in <tt>probabilities</tt> are all equal.
 *
 * @param <S> The type of object.
 * @param <D> The type of the probability value which is not <tt>double</tt>.
 * @author Hellisk
 * @since 30/03/2019
 */

public class InverselyComparableObject<S, D> implements Comparable<InverselyComparableObject<S, D>> {
	
	private static final Logger LOG = Logger.getLogger(InverselyComparableObject.class);
	private final S object;     // object
	private final List<Double> probabilities;
	private final D comparableValue;
	
	/**
	 * Creates a new object with probabilities.
	 *
	 * @param object          The object
	 * @param probabilities   The corresponding probabilities
	 * @param comparableValue The value that can be used for comparison if previous probabilities are all equal.
	 */
	public InverselyComparableObject(S object, List<Double> probabilities, D comparableValue) {
		this.object = object;
		this.probabilities = probabilities;
		this.comparableValue = comparableValue;
	}
	
	/**
	 * Creates a new object with one probability.
	 *
	 * @param object          The object
	 * @param probability     The probability
	 * @param comparableValue The value that can be used for comparison if previous probabilities are all equal.
	 */
	public InverselyComparableObject(S object, double probability, D comparableValue) {
		this.object = object;
		this.probabilities = new ArrayList<>();
		this.probabilities.add(probability);
		this.comparableValue = comparableValue;
	}
	
	/**
	 * Creates a new object with two probabilities.
	 *
	 * @param object          The object
	 * @param probability1    The first probability
	 * @param probability2    The second probability
	 * @param comparableValue The value that can be used for comparison if previous probabilities are all equal.
	 */
	public InverselyComparableObject(S object, double probability1, double probability2, D comparableValue) {
		this.object = object;
		this.probabilities = new ArrayList<>();
		this.probabilities.add(probability1);
		this.probabilities.add(probability2);
		this.comparableValue = comparableValue;
	}
	
	/**
	 * Get the comparing object.
	 *
	 * @return The object.
	 */
	public S getObject() {
		return this.object;
	}
	
	/**
	 * Get the major probability, which is the first value in the probability list <tt>probabilities</tt>
	 *
	 * @return The first probability of the list <tt>probabilities</tt>.
	 */
	public double getFirstProbability() {
		return this.probabilities.get(0);
	}
	
	/**
	 * Get the specific probability from the list given the index <tt>i</tt>.
	 *
	 * @param i Probability index.
	 * @return The corresponding probability.
	 * @throws IndexOutOfBoundsException If the requested index does not have corresponding probability.
	 */
	private double getProbability(int i) {
		if (i >= this.probabilities.size() || i < 0)
			throw new IndexOutOfBoundsException("The requested " + i + "-th probability does not exist.");
		return this.probabilities.get(i);
	}
	
	/**
	 * The instances are compared based on their probabilities. The probabilities in the list are compared inversely so as to use in the
	 * max-heap, The <tt>comparableValue</tt> will be used for comparison if all other probabilities are equal.
	 *
	 * @param o The object to be compared.
	 * @return Inverse comparison result.
	 */
	@Override
	public int compareTo(InverselyComparableObject o) {
		int result = Double.compare(o.getFirstProbability(), this.probabilities.get(0));    // inverse comparison
		if (result != 0)
			return result;
		else {
			if (this.probabilities.size() != o.probabilities.size())
				LOG.debug("The size of the probability list is different: " + this.probabilities.size() + "," + o.probabilities.size());
			int i = 1;
			while (result == 0 && this.probabilities.size() > i) {
				if (o.probabilities.size() > i) // both lists contain the current index
					result = Double.compare(this.probabilities.get(i), o.getProbability(i));
				else result = this.probabilities.size() - o.probabilities.size();
				i++;
			}
			if (result != 0)
				return result;
			else if (o.probabilities.size() > i) {  // the compared probabilities are the same, but o has more probability
				return o.probabilities.size() - this.probabilities.size();
			} else {    // use comparableValue for the final comparison
				result = Integer.compare(this.comparableValue.hashCode(), o.comparableValue.hashCode());
				if (result != 0)
					return result;
				else {
					LOG.debug("Two InverselyComparableObjects are identical: " + this.toString() + "_" + o.toString());
					return Integer.compare(this.object.hashCode(), o.object.hashCode());
				}
			}
		}
	}
}