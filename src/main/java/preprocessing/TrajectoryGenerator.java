package preprocessing;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Segment;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.Pair;

import java.util.*;

/**
 * Generate synthetic trajectories according to the given parameters.
 *
 * @author uqpchao
 * Created 3/06/2019
 */
public class TrajectoryGenerator {
	
	private static final Logger LOG = Logger.getLogger(TrajectoryGenerator.class);
	
	/**
	 * Generate a list of synthetic trajectories that follows the given distribution and sampling rate.
	 *
	 * @param gtRouteList      The input list of routes and trajectory IDs, the input route may not be continuous on the map.
	 * @param timeDiffList     The time span of each route travel.
	 * @param map              The underlying map.
	 * @param sigma            The Gaussian function parameter. Pr(x\in[x-sigma,x+sigma])=0.6526, Pr(x\in[x-2*sigma,x+2*sigma])=0.9544
	 * @param samplingInterval The number of seconds per point.
	 * @return The generated trajectories, which has the same size as the input route list.
	 */
	public static List<Trajectory> rawTrajGenerator(List<Pair<String, List<String>>> gtRouteList, List<Long> timeDiffList,
													RoadNetworkGraph map, double sigma, int samplingInterval) {
		DistanceFunction distFunc = map.getDistanceFunction();
		if (gtRouteList.size() != timeDiffList.size())
			throw new IllegalArgumentException("The size of the input route list and time different list is inconsistent.");
		Map<String, RoadWay> id2WayMap = new HashMap<>();
		for (RoadWay way : map.getWays()) {
			id2WayMap.put(way.getID(), way);
		}
		List<Trajectory> resultTrajList = new ArrayList<>();
		for (int i = 0; i < gtRouteList.size(); i++) {
			List<RoadWay> currRoute = new ArrayList<>();
			double length = 0;
			boolean isContinuous = true;
			for (String s : gtRouteList.get(i)._2()) {
				RoadWay currWay = id2WayMap.get(s);
				if (currRoute.size() != 0) {
					if (!currRoute.get(currRoute.size() - 1).getToNode().equals(currWay.getFromNode())) {
						LOG.warn("The input routes contains disconnected roads.");
						isContinuous = false;
						break;
					}
				}
				currRoute.add(currWay);
				length += currWay.getLength();
			}
			if (!isContinuous)    // the current route is omitted
				continue;
			List<TrajectoryPoint> trajPointList = new ArrayList<>();
			double interval = length / timeDiffList.get(i) * samplingInterval;        // the distance per point
			double remainLength = 0;    // used when the previous road way has left-over distance
			RoadNode startNode = currRoute.get(0).getFromNode();
			
			// add start point
			trajPointList.add(new TrajectoryPoint(startNode.lon(), startNode.lat(), trajPointList.size() + 1, distFunc));
			for (RoadWay roadWay : currRoute) {
				for (Segment edge : roadWay.getEdges()) {
					remainLength += edge.length();
					while (remainLength > interval) {    // insert new node
						remainLength -= interval;
						double ratio = remainLength / edge.length();
						double currLon = edge.x2() - (edge.x2() - edge.x1()) * ratio;
						double currLat = edge.y2() - (edge.y2() - edge.y1()) * ratio;
						trajPointList.add(new TrajectoryPoint(currLon, currLat, trajPointList.size() + 1, distFunc));
					}
				}
			}
			RoadNode endNode = currRoute.get(currRoute.size() - 1).getToNode();
			
			// add start point
			trajPointList.add(new TrajectoryPoint(endNode.lon(), endNode.lat(), trajPointList.size() + 1, distFunc));
			trajPointShift(trajPointList, sigma, distFunc);
			Trajectory currTraj = new Trajectory(gtRouteList.get(i)._1(), trajPointList);
			resultTrajList.add(currTraj);
		}
		return resultTrajList;
	}
	
	/**
	 * Generate a list of synthetic trajectories that follows the given distribution with sampling rate 1pts/sec and road coverage
	 * requirement.
	 *
	 * @param gtRouteList      The input list of routes and trajectory IDs, the input route may not be continuous on the map.
	 * @param timeDiffList     The time span of each route travel.
	 * @param map              The underlying map.
	 * @param sigma            The Gaussian function parameter. Pr(x\in[x-sigma,x+sigma])=0.6526, Pr(x\in[x-2*sigma,x+2*sigma])=0.9544
	 * @param samplingInterval The number of seconds per point.
	 * @param percentage       The percentage of roads to be covered.
	 * @return The generated trajectories, which has the same size as the input route list.
	 */
	public static List<Trajectory> rawTrajWithCoverageGenerator(List<Pair<String, List<String>>> gtRouteList, List<Long> timeDiffList,
																RoadNetworkGraph map, double sigma, int samplingInterval, double percentage) {
		Set<String> coveredWaySet = new HashSet<>();
		boolean isNewRoadOccurred;
		double mapWaySize = map.getWays().size();
		List<Pair<String, List<String>>> tempGTRouteList = new ArrayList<>();
		List<Long> tempTimeDiffList = new ArrayList<>();
		for (int i = 0; i < gtRouteList.size(); i++) {
			isNewRoadOccurred = false;
			for (String s : gtRouteList.get(i)._2()) {
				if (!coveredWaySet.contains(s)) {
					isNewRoadOccurred = true;
					break;
				}
			}
			if (isNewRoadOccurred) {
				coveredWaySet.addAll(gtRouteList.get(i)._2());
				tempGTRouteList.add(gtRouteList.get(i));
				tempTimeDiffList.add(timeDiffList.get(i));
				if (coveredWaySet.size() >= mapWaySize * percentage)
					break;
			}
		}
		if (coveredWaySet.size() < mapWaySize * percentage)
			System.out.println("Cannot achieve required road coverage, the actual coverage is: " + (double) coveredWaySet.size() / mapWaySize * 100 + "%");
		return rawTrajGenerator(tempGTRouteList, tempTimeDiffList, map, sigma, samplingInterval);
	}
	
	/**
	 * Randomly shift every point in the trajectories into a region whose distance to its original position is less than
	 * <tt>errorRadius</tt>. The randomness follows the Gaussian distribution.
	 *
	 * @param trajPointList The input trajectory point list.
	 * @param sigma         The Gaussian parameter.
	 * @param distFunc      The distance function.
	 */
	private static void trajPointShift(List<TrajectoryPoint> trajPointList, double sigma, DistanceFunction distFunc) {
		Random random = new Random();
		for (TrajectoryPoint point : trajPointList) {
			double newLon = point.x() + distFunc.getCoordinateOffsetX(random.nextGaussian() * sigma, point.y());
			double newLat = point.y() + distFunc.getCoordinateOffsetY(random.nextGaussian() * sigma, point.x());
			point.setPoint(newLon, newLat, distFunc);
		}
	}
}
