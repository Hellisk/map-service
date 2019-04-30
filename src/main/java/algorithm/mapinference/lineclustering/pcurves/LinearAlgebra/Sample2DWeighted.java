package algorithm.mapinference.lineclustering.pcurves.LinearAlgebra;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

final public class Sample2DWeighted extends Sample2D {
	public Sample2DWeighted() {
		super();
	}
	
	@Override
	final protected boolean AddPoint(StringTokenizer t) {
		try {
			Vektor2D point = new Vektor2DWeighted(t);
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
}
