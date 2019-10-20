package util.object.structure;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The object for storing point and route map-matching results of a trajectory, which includes the matching node and edge of each point
 * and the matching route of the entire trajectory. If only the point match or route match is available, use list of PointMatch or String
 * instead.
 *
 * @author Hellisk
 * @since 30/03/2019
 */
public class SimpleTrajectoryMatchResult {
	
	private static final Logger LOG = Logger.getLogger(SimpleTrajectoryMatchResult.class);
	private String trajID;    // the original trajectory
	private List<PointMatch> pointMatchResult;    // either point match or route match can be empty, but not both.
	private List<String> routeMatchResult;
	
	public SimpleTrajectoryMatchResult(String trajID, List<PointMatch> pointMatchResult, List<String> routeMatchResult) {
		this.trajID = trajID;
		if (pointMatchResult == null || pointMatchResult.isEmpty())
			this.pointMatchResult = new ArrayList<>();
		else {
			for (PointMatch pointMatch : pointMatchResult) {    // remove the trailing road series number.
				pointMatch.setRoadID(pointMatch.getRoadID().contains("|") ? pointMatch.getRoadID().substring(0,
						pointMatch.getRoadID().lastIndexOf("|")) : pointMatch.getRoadID());
			}
			this.pointMatchResult = pointMatchResult;
		}
		
		if (routeMatchResult == null || routeMatchResult.isEmpty())
			this.routeMatchResult = new ArrayList<>();
		else
			this.routeMatchResult = routeMatchResult;
		
		if (this.routeMatchResult.isEmpty() && this.pointMatchResult.isEmpty())
			LOG.warn("Both the point and route match result is empty, trajectory ID: " + trajID);
	}
	
	public static SimpleTrajectoryMatchResult parseSimpleTrajMatchResult(String s, String trajID, DistanceFunction df) {
		String[] lines = s.split("\n");
		if (lines.length == 0)    // matching result contains no route and point match
			return new SimpleTrajectoryMatchResult(trajID, new ArrayList<>(), new ArrayList<>());
		List<PointMatch> pointMatchList = new ArrayList<>();
		// start parsing the first line, which contains point matches
		String[] firstLine = lines[0].split(",");
		if (firstLine.length != 1 || !firstLine[0].equals("")) {
			for (String value : firstLine) {    // initialise all match sequences
				pointMatchList.add(PointMatch.parsePointMatch(value, df));
			}
		}
		List<String> routeMatchList;
		if (lines.length != 2) {
			if (s.endsWith("\n")) {
				// no route match result, return empty list
				routeMatchList = new ArrayList<>();
			} else
				throw new IllegalArgumentException("The input trajectory string has line count other than 2: " + lines.length);
		} else {
			String[] secondLine = lines[1].split(",");
			if (secondLine.length != 1 || !secondLine[0].equals("")) {
				routeMatchList = new ArrayList<>(Arrays.asList(secondLine));
			} else {
				routeMatchList = new ArrayList<>();
			}
		}
		return new SimpleTrajectoryMatchResult(trajID, pointMatchList, routeMatchList);
	}
	
	public String getTrajID() {
		return this.trajID;
	}
	
	/**
	 * Get the point matching result list.
	 *
	 * @return The point matching result list.
	 */
	public List<PointMatch> getPointMatchResultList() {
		return pointMatchResult;
	}
	
	/**
	 * Return the route matching result list.
	 *
	 * @return The point matching result.
	 */
	public List<String> getRouteMatchResultList() {
		return routeMatchResult;
	}
	
	/**
	 * Return the point matching result of a trajectory point.
	 *
	 * @param pointIndex The index of the specific point.
	 * @return The point matching result.
	 */
	public PointMatch getPointMatch(int pointIndex) {
		return pointMatchResult.get(pointIndex);
	}
	
	/**
	 * Set the point matching results for the trajectory. The point list must of the same size as the trajectory.
	 *
	 * @param pointMatchList The point matching results.
	 */
	public void setPointMatchResult(List<PointMatch> pointMatchList) {
		for (PointMatch pointMatch : pointMatchList) {    // remove the trailing road series number.
			pointMatch.setRoadID(pointMatch.getRoadID().contains("\\|") ? pointMatch.getRoadID().substring(0,
					pointMatch.getRoadID().lastIndexOf("\\|")) : pointMatch.getRoadID());
		}
		this.pointMatchResult = pointMatchList;
	}
	
	/**
	 * Set the route matching result for the trajectory.
	 *
	 * @param routeMatchList The route match result list.
	 */
	public void setRouteMatchResult(List<String> routeMatchList) {
		this.routeMatchResult = routeMatchList;
	}
	
	public boolean containsPointMatch() {
		return !this.pointMatchResult.isEmpty();
	}
	
	public boolean containsRouteMatch() {
		return !this.routeMatchResult.isEmpty();
	}
	
	/**
	 * Format: 1. pointMatch1,pointMatch2,...
	 * 2. routeMatch1,routeMatch,...
	 * The trajectory id should be stored as the name of the file
	 *
	 * @return The result of the map-matching.
	 */
	@Override
	public String toString() {
		StringBuilder line = new StringBuilder();
		
		// the first line is the point match result
		if (this.pointMatchResult.size() != 0) {
			for (int i = 0; i < pointMatchResult.size(); i++) {
				line.append(this.getPointMatch(i).toString()).append(",");
			}
			line.deleteCharAt(line.length() - 1);
		}
		line.append("\n");
		
		// the second line is the route match result
		if (this.routeMatchResult.size() != 0) {
			for (int i = 0; i < routeMatchResult.size(); i++) {
				line.append(this.getRouteMatchResultList().get(i)).append(",");
			}
			line.deleteCharAt(line.length() - 1);
		}
		return line.toString();
	}
}