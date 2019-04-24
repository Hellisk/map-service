package util.object.structure;

import util.function.DistanceFunction;

/**
 * A item that can be represented by its length.
 *
 * @author Hellisk
 * @since 30/03/2019
 */
public class LengthObject implements Comparable<LengthObject> {
	
	private final DistanceFunction distFunc;
	private double length;
	private String parentID;
	
	LengthObject(DistanceFunction df) {
		this.length = 0;
		this.parentID = "";
		this.distFunc = df;
	}
	
	LengthObject(double length, String parentID, DistanceFunction df) {
		this.length = length;
		this.parentID = parentID;
		this.distFunc = df;
	}
	
	public double getLength() {
		return length;
	}
	
	public void setLength(double length) {
		this.length = length;
	}
	
	public String getID() {
		return this.parentID;
	}
	
	public void setID(String parentID) {
		this.parentID = parentID;
	}
	
	public DistanceFunction getDistanceFunction() {
		return distFunc;
	}
	
	@Override
	public int compareTo(LengthObject o) {
		if (o == null)
			throw new NullPointerException();
		return Double.compare(this.length, o.length);
	}
}
