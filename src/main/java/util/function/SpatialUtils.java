package util.function;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import util.object.spatialobject.Point;
import util.object.spatialobject.Rect;
import util.object.spatialobject.SpatialObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of miscellaneous utilities functions.
 *
 * @author uqdalves
 */
public class SpatialUtils implements Serializable {
	/**
	 * Earth radius in meters - AVERAGE.
	 */
	public static final int EARTH_RADIUS = 6371000;
	/**
	 * Earth radius in meters - EQUATOR.
	 */
	public static final int EARTH_RADIUS_EQUATOR = 6378137;
	/**
	 * Earth radius in meters - TROPICS.
	 */
	public static final int EARTH_RADIUS_TROPICS = 6374761;
	/**
	 * Earth radius in meters - POLES.
	 */
	public static final int EARTH_RADIUS_POLES = 6356752;
	
	/**
	 * Displays a list of spatial points in a GUI window.
	 *
	 * @param list List of points to display.
	 */
	public static void display(List<Point> list) {
		if (list.isEmpty()) return;
		
		Graph graph = new SingleGraph("MultiPoint");
		graph.display(false);
		// create one node per point
		Point p;
		for (int i = 1; i < list.size(); i++) {
			p = list.get(i);
			graph.addNode("N" + i).setAttribute("xy", p.x(), p.y());
		}
	}
	
	/**
	 * Find the bounding box of a given set of point.
	 *
	 * @param pointList The point set.
	 * @param df        The distance function.
	 * @return The rectangle representing the bounding box.
	 */
	public static Rect getBoundingBox(List<Point> pointList, DistanceFunction df) {
		double minX = pointList.get(0).x();
		double maxX = pointList.get(0).x();
		double minY = pointList.get(0).y();
		double maxY = pointList.get(0).y();
		for (int i = 1; i < pointList.size(); i++) {
			Point p = pointList.get(i);
			if (p.x() < minX)
				minX = p.x();
			if (p.x() > maxX)
				maxX = p.x();
			if (p.y() < minY)
				minY = p.y();
			if (p.y() > maxY)
				maxY = p.y();
		}
		if (minX == maxX && minY == maxY)
			throw new IllegalArgumentException("The bounding box for point list: " + pointList.toString() + " is a point: " + minX + "," + maxY);
		return new Rect(minX, minY, maxX, maxY, df);
	}
	
	/**
	 * Display a list of spatial objects in a GUI window.
	 *
	 * @param spatialObjList List of spatial objects to display.
	 */
	public static void displayMany(List<SpatialObject> spatialObjList) {
		for (SpatialObject obj : spatialObjList) {
			obj.display();
		}
	}
	
	/**
	 * Check whether the points sequence a--b--c is a counter-clockwise turn.
	 *
	 * @param a First point of the sequence.
	 * @param b Second point of the sequence.
	 * @param c Third point of the sequence.
	 * @return +1 if counter-clockwise, -1 if clockwise, 0 if collinear.
	 */
	public static int isCCW(Point a, Point b, Point c) {
		double area2 = (b.x() - a.x()) * (c.y() - a.y()) -
				(c.x() - a.x()) * (b.y() - a.y());
		if (area2 < 0) return -1;
		else if (area2 > 0) return +1;
		else return 0;
	}
	
	/**
	 * Check whether the points a--b--c are collinear.
	 *
	 * @param a
	 * @param b
	 * @param c
	 * @return True if a,b,c are collinear.
	 */
	public static boolean isCollinear(Point a, Point b, Point c) {
		return isCCW(a, b, c) == 0;
	}
	
	/**
	 * @param values A list of numbers to get the mean.
	 * @return The mean of the values in this list.
	 */
	public static double getMean(List<Double> values) {
		Decimal sum = new Decimal(0);
		for (double value : values) {
			sum = sum.sum(value);
		}
		Decimal mean = sum.divide(values.size());
		
		return mean.value();
	}
	
	/**
	 * @param values A list of numbers to get the mean.
	 * @return The mean of the values in this list.
	 */
	public static double getMean(double... values) {
		Decimal sum = new Decimal(0);
		for (double value : values) {
			sum = sum.sum(value);
		}
		Decimal mean = sum.divide(values.length);
		
		return mean.value();
	}
	
	/**
	 * @param list A list of numbers to get the standard deviation.
	 * @param mean The mean of the values in the list.
	 * @return The standard deviation of values in this list.
	 */
	public static double getStd(List<Double> list, double mean) {
		int size = list.size();
		Decimal diff;
		Decimal meanDecimal = new Decimal(mean);
		Decimal sumSqr = new Decimal(0);
		for (double value : list) {
			diff = meanDecimal.sub(value);
			sumSqr = sumSqr.sum(diff.pow2());
		}
		Decimal std = sumSqr.divide(size).sqrt();
		
		return std.value();
	}
	
	/**
	 * @param values A list of numbers to get the standard deviation.
	 * @return The standard deviation of values in this list.
	 */
	public static double getStd(List<Double> values) {
		int size = values.size();
		Decimal sum = new Decimal(0);
		Decimal sqr = new Decimal(0);
		for (double value : values) {
			sum = sum.sum(value);
			sqr = sqr.sum(value * value);
		}
		Decimal stdSqr = sqr.sub(sum.pow2().divide(size));
		Decimal std = stdSqr.divide(size - 1).sqrt();
		
		return std.value();
	}
	
	/**
	 * @param values A list of numbers to get the standard deviation.
	 * @return The standard deviation of values in this list.
	 */
	public static double getStd(double... values) {
		int size = values.length;
		Decimal sum = new Decimal(0);
		Decimal sqr = new Decimal(0);
		for (double value : values) {
			sum = sum.sum(value);
			sqr = sqr.sum(value * value);
		}
		Decimal stdSqr = sqr.sub(sum.pow2().divide(size));
		Decimal std = stdSqr.divide(size - 1).sqrt();
		
		return std.value();
	}
	
	/**
	 * Returns a random number between zero and the given number.
	 *
	 * @param number threshold.
	 * @return A double value with a positive sign, greater than or
	 * equal to 0.0 and less than the given number.
	 */
	public static double random(double number) {
		return number * Math.random();
	}
	
	/**
	 * Select a given number of random sample elements
	 * from the given list.
	 *
	 * @param list The list to sample.
	 * @param n    Number of sample elements.
	 * @return A list of size (n) sample elements.
	 */
	public static <T> List<T> getSample(List<T> list, final int n) {
		if (list == null) {
			throw new NullPointerException("List of elements for sampling "
					+ "cannot be null.");
		}
		if (n <= 0 || n >= list.size()) {
			throw new IllegalArgumentException("Number of samples must be positve "
					+ "and smaller than the list size.");
		}
		// make sure the original list is unchanged
		List<T> result = new ArrayList<T>(list);
		Collections.shuffle(result);
		return result.subList(0, n);
	}
	
	/**
	 * Dot product between two generic vectors V * U.
	 * Vector U and V should be of same dimension.
	 *
	 * @param v Vector V.
	 * @param u Vector U.
	 * @return Dot product V*U.
	 */
	public static double dotProduct(double[] v, double[] u) {
		if (v == null || u == null) {
			throw new NullPointerException("Vectors for dot product calculation cannot be null.");
		}
		if (u.length != v.length) {
			throw new IllegalArgumentException("Vectors should be of same size for dot product calculation.");
		}
		
		if (v.length == 0) {
			return 0;
		}
		double dotProduct = 0;
		for (int i = 0; i < v.length; i++) {
			dotProduct += v[i] * u[i];
		}
		
		return dotProduct;
	}
	
	/**
	 * The Norm (length) of this N-Dimensional vector.
	 *
	 * @param v N-dimensional vector.
	 * @return The norm of V.
	 */
	public static double norm(double[] v) {
		if (v == null) {
			throw new NullPointerException("Vector cannot be null");
		}
		
		if (v.length == 0) {
			return 0;
		}
		double norm = 0;
		for (int i = 0; i < v.length; i++) {
			norm += v[i] * v[i];
		}
		
		return Math.sqrt(norm);
	}
}
