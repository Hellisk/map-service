package algorithm.mapinference.lineclustering.pcurves.LinearAlgebra;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class Sample2D extends SampleLoadable {
	public Sample2D() {
		super();
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// abstract functions of SampleLoadable BEGIN
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	final public Sample DefaultClone() {
		return new Sample2D();
	}
	
	@Override
	protected boolean AddPoint(StringTokenizer t) {
		try {
			Vektor2D point = new Vektor2D(t);
			AddPoint(point);
			return true;
		}
		// If wrong format, we just don't load it, and return false
		catch (NoSuchElementException e1) {
			return false;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// abstract functions of SampleLoadable END
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
