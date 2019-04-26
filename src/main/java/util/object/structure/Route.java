package util.object.structure;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.spatialobject.Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A route is a sequence of road ways representing the actual running route of on the road, which should be continuous ideally. Multiple
 * routes can be concatenated into one if they are continuous.
 * </p>
 * The <tt>Route</tt> object for a break point should contains the same start and end points(=(0,0) when it is unable to be matched to a
 * road) but empty roadIDList. Those objects are to be skipped when concatenating a complete route.
 *
 * @author Hellisk
 * @since 2/04/2019
 */
public class Route implements Serializable {
	
	private static final Logger LOG = Logger.getLogger(Route.class);
	
	private List<String> roadIDList;
	private Point startPoint;    // the start point must lie on the first road of the roadIDList
	private Point endPoint;        // the end point must lie on the last road of the roadIDList
	
	/**
	 * Check whether the start point/end point locates at the first/last road way before creating the route.
	 *
	 * @param startPoint The start point of the route, should be on the first road way in roadIDList
	 * @param endPoint   The end point of the route, should be on the last road way in roadIDList
	 * @param roadIDList The road way list which constitute the route.
	 */
	public Route(Point startPoint, Point endPoint, List<String> roadIDList) {
		this.startPoint = startPoint;
		this.endPoint = endPoint;
		this.roadIDList = roadIDList;
	}
	
	static Route parseRoute(String s, DistanceFunction df) {
		String[] routeInfo = s.split(" ");
		if (routeInfo.length < 5)
			throw new IllegalArgumentException("The input text cannot be parsed to a route: " + s);
		
		// parse start and end points
		Point startPoint = new Point(Double.parseDouble(routeInfo[0]), Double.parseDouble(routeInfo[1]), df);
		Point endPoint = new Point(Double.parseDouble(routeInfo[2]), Double.parseDouble(routeInfo[3]), df);
		
		// parse route ID list
		List<String> routeIDList = new ArrayList<>(Arrays.asList(routeInfo).subList(4, routeInfo.length));
		return new Route(startPoint, endPoint, routeIDList);
	}
	
	/**
	 * Add a route to the end of the current one. Used for concatenating a sequence of routes.
	 *
	 * @param route The route to be appended.
	 */
	void addRoute(Route route) {
		if (route.startPoint.equals2D(route.endPoint)) {
			return;
		}
		if (this.endPoint.equals2D(route.getStartPoint())) {
			if (this.getEndRoadID().equals(route.getStartRoadID())) {    // continuous road, connect them
				this.roadIDList.remove(this.roadIDList.size() - 1);
				this.roadIDList.addAll(route.getRoadIDList());
			} else {    // same point, but the road id are not the same
				if (route.getRoadIDList().size() < 2) {
					this.roadIDList.addAll(route.getRoadIDList());
				} else {
					// find the first road id that matches the end road id
					int index = 1;
					String currRoadID = route.getRoadIDList().get(index);
					while (!currRoadID.equals(this.getEndRoadID()) && index < route.getRoadIDList().size() - 1) {
						index++;
						currRoadID = route.getRoadIDList().get(index);
					}
					if (index < route.getRoadIDList().size() - 1) {    // found the connecting road id
						this.roadIDList.addAll(route.getRoadIDList().subList(index + 1, route.getRoadIDList().size()));
					} else {        // add all of the roads anyway
						this.roadIDList.addAll(route.getRoadIDList());
					}
				}
			}
			this.setEndPoint(route.getEndPoint());
		} else {
			this.roadIDList.addAll(route.getRoadIDList());
			this.setEndPoint(route.getEndPoint());
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
		if (roadIDList.size() != 0) {
			for (int i = 0; i < roadIDList.size() - 1; i++) {
				String s = roadIDList.get(i);
				roadIDString.append(s).append(" ");
			}
			roadIDString.append(roadIDList.get(roadIDList.size() - 1));
		}
		return startPoint.toString() + " " + endPoint.toString() + " " + (roadIDString.length() == 0 ? "null" : roadIDString);
	}
}
