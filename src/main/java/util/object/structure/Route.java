package util.object.structure;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.spatialobject.Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A route is a sequence of road ways representing the actual running route of on the road, which should be continuous ideally. The
 * breakpoint information are stored if the route happens to break. Multiple routes can be concatenated into one if they are continuous.
 *
 * @author Hellisk
 * @since 2/04/2019
 */
public class Route implements Serializable {
	
	private static final Logger LOG = Logger.getLogger(Route.class);
	
	private List<String> roadIDList;
	private Point startPoint;    // the start point must lie on the first road of the roadIDList
	private Point endPoint;        // the end point must lie on the last road of the roadIDList
//	private final List<Pair<String, String>> breakPointList;    // list of unconnected road pairs represented by road ids.
	
	/**
	 * Check whether the start point/end point locates at the first/last road way before creating the route.
	 *
	 * @param startPoint The start point of the route, should be on the first road way in roadIDList
	 * @param endPoint   The end point of the route, should be on the last road way in roadIDList
	 * @param roadIDList The road way list which constitute the route.
	 */
	public Route(Point startPoint, Point endPoint, List<String> roadIDList) {
		if (startPoint.equals2D(endPoint)) {
			if (roadIDList.isEmpty())
				throw new IllegalArgumentException("The current route has the same start and end points.");
		}
		this.startPoint = startPoint;
		this.endPoint = endPoint;
		this.roadIDList = roadIDList;
	}
	
	public static Route parseRoute(String s, DistanceFunction df) {
		String[] routeInfo = s.split(",");
		if (routeInfo.length != 2)
			throw new IllegalArgumentException("The input text cannot be parsed to a route: " + s);
		
		// parse start and end points
		String[] pointInfo = routeInfo[0].split(" ");
		if (pointInfo.length != 4)
			throw new IllegalArgumentException("The input text cannot be parsed to start and end points: " + routeInfo[0]);
		Point startPoint = new Point(Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1]), df);
		Point endPoint = new Point(Double.parseDouble(pointInfo[2]), Double.parseDouble(pointInfo[3]), df);
		
		// parse route ID list
		String[] idInfo = routeInfo[1].split(" ");
		List<String> routeIDList = new ArrayList<>(Arrays.asList(idInfo));
		
		return new Route(startPoint, endPoint, routeIDList);
	}
	
	/**
	 * Add a route to the end of the current one. Report break if two routes are not connected.
	 *
	 * @param route The route to be appended.
	 *              //	 * @return True if two route are connected, otherwise false.
	 */
	public void addRoute(Route route) {
		if (this.endPoint.equals2D(route.getStartPoint())) {
			if (this.getEndRoadID().equals(route.getStartRoadID())) {    // continuous road, connect them
				this.roadIDList.remove(this.roadIDList.size() - 1);
				this.roadIDList.addAll(route.getRoadIDList());
			} else {    // same point, but the road id are not the same
				LOG.debug("The end road is not the same as the start road of next route even with the same point. Probably intersection " +
						"point: " + this.getEndRoadID() + "," + route.getStartRoadID());
				this.roadIDList.addAll(route.getRoadIDList());
			}
			this.setEndPoint(route.getEndPoint());
//			return true;
		} else {
			this.roadIDList.addAll(route.getRoadIDList());
			this.setEndPoint(route.getEndPoint());
//			return false;
		}
	}
	
	public List<String> getRoadIDList() {
		return roadIDList;
	}
	
	public String getStartRoadID() {
		return roadIDList.get(0);
	}
	
	public String getEndRoadID() {
		return roadIDList.get(roadIDList.size() - 1);
	}
	
	public Point getStartPoint() {
		return startPoint;
	}
	
	private void setStartPoint(Point startPoint) {
		this.startPoint = startPoint;
	}
	
	public Point getEndPoint() {
		return endPoint;
	}
	
	private void setEndPoint(Point endPoint) {
		this.endPoint = endPoint;
	}
	
	@Override
	protected Route clone() {
		List<String> roadIDList = new ArrayList<>(this.roadIDList);
		return new Route(this.startPoint.clone(), this.endPoint.clone(), roadIDList);
	}
	
	/**
	 * Format: startPointX startPointY endPointX endPointY, routeID1 routeID2 routeID3 ...
	 *
	 * @return The String representation of Route object.
	 */
	@Override
	public String toString() {
		StringBuilder roadIDString = new StringBuilder();
		for (String s : roadIDList) {
			roadIDString.append(s).append(" ");
		}
		roadIDString.substring(0, roadIDString.length() - 1);    // remove the last space
		return startPoint.toString() + " " + endPoint.toString() + "," + (roadIDString.length() == 0 ? "null" : roadIDString);
	}
}
