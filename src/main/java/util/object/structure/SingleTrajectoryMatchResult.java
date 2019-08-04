package util.object.structure;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.spatialobject.Point;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * The object for storing single map-matching results of a trajectory, which includes the matching node and edge of each point, the matching
 * route between two points, the probabilities and a list of breakpoints. The structure is used to store multiple matching results for the
 * same trajectory and sorted by their probabilities. Please refer to <tt>SingleTrajectoryMatchResult</tt> if only the best matching
 * result is stored.
 *
 * @author Hellisk
 * @since 30/03/2019
 */
public class SingleTrajectoryMatchResult {
	
	private static final Logger LOG = Logger.getLogger(Point.class);
	
	private final int requiredNumOfRanks;    // The number of map-matching results required by the system. The actual number of matching
	// results should be no more than it.
	private int numOfRanks;        // Number of map-matching results. Each count represents a complete map-matching result of the
	// trajectory.
	private Trajectory trajectory;    // the original trajectory
	private List<List<PointMatch>> pointMatchResult;        // the point matching result
	private List<List<Route>> routeMatchResult;        // the matching route which starts from the matching point of the last point and
	// ends at the current matching point
	private double[] probabilities;        // The overall probability of the matching result. The length of the probability list is
	// equal to requiredNumOfRanks. The initial probability should be Double.NEGATIVE_INFINITY.
	private List<BitSet> breakPointBSList;    // each position represents if the corresponding trajectory point is a break point
	
	public SingleTrajectoryMatchResult(Trajectory traj, int requiredNumOfRanks) {
		if (requiredNumOfRanks < 1) throw new IllegalArgumentException("Matching result size should be at least 1");
		this.trajectory = traj;
		this.requiredNumOfRanks = requiredNumOfRanks;
		this.numOfRanks = 0;
		this.pointMatchResult = new ArrayList<>(requiredNumOfRanks);
		this.routeMatchResult = new ArrayList<>(requiredNumOfRanks);
		this.breakPointBSList = new ArrayList<>(requiredNumOfRanks);
		this.probabilities = new double[requiredNumOfRanks];
		for (int i = 0; i < requiredNumOfRanks; i++) {
			this.pointMatchResult.add(new ArrayList<>());
			this.routeMatchResult.add(new ArrayList<>());
			this.breakPointBSList.add(new BitSet(trajectory.size()));
			this.probabilities[i] = Double.NEGATIVE_INFINITY;
		}
	}
	
	public SingleTrajectoryMatchResult(Trajectory traj, int requiredNumOfRanks, int numOfRanks, List<List<PointMatch>> pointMatchResult,
									   List<List<Route>> routeMatchResult, double[] probabilities, List<BitSet> breakPointBSList) {
		if (requiredNumOfRanks < 1) throw new IllegalArgumentException("Matching result size should be at least 1");
		if (numOfRanks > requiredNumOfRanks)
			throw new IllegalArgumentException("The number of matching result should not be larger than the requirement: " + numOfRanks
					+ "," + requiredNumOfRanks);
		if (numOfRanks != pointMatchResult.size() || numOfRanks != routeMatchResult.size() || numOfRanks != breakPointBSList.size())
			throw new IllegalArgumentException("Inconsistent size of point/route/breakpoint matching list.");
		if (probabilities.length != requiredNumOfRanks || probabilities[numOfRanks - 1] == Double.NEGATIVE_INFINITY
				|| (numOfRanks < requiredNumOfRanks && probabilities[numOfRanks] != Double.NEGATIVE_INFINITY))
			throw new IllegalArgumentException("Inconsistent size of probability list.");
		this.trajectory = traj;
		this.requiredNumOfRanks = requiredNumOfRanks;
		this.numOfRanks = numOfRanks;
		this.pointMatchResult = pointMatchResult;
		this.routeMatchResult = routeMatchResult;
		this.breakPointBSList = breakPointBSList;
		this.probabilities = probabilities;
	}
	
	/**
	 * Construct a single matching result.
	 *
	 * @param traj         The original trajectory.
	 * @param pointMatches The point matching result.
	 * @param routeMatches The route matching result.
	 * @param probability  The matching probability.
	 * @param breakPointBS The breakpoint list.
	 */
	public SingleTrajectoryMatchResult(Trajectory traj, List<PointMatch> pointMatches, List<Route> routeMatches,
									   double probability, BitSet breakPointBS) {
		if (traj.size() != pointMatches.size() || traj.size() != routeMatches.size() || traj.size() != breakPointBS.size())
			throw new IllegalArgumentException("Inconsistent input size of point/route/breakpoint matching list.");
		this.requiredNumOfRanks = 1;
		this.trajectory = traj;
		this.pointMatchResult = new ArrayList<>(requiredNumOfRanks);
		this.routeMatchResult = new ArrayList<>(requiredNumOfRanks);
		this.breakPointBSList = new ArrayList<>(requiredNumOfRanks);
		this.probabilities = new double[requiredNumOfRanks];
		this.pointMatchResult.add(pointMatches);
		this.routeMatchResult.add(routeMatches);
		this.breakPointBSList.add(breakPointBS);
		this.probabilities[0] = probability;
		this.numOfRanks = 1;
	}
	
	/**
	 * Use the constructor to merge multiple matching results of the same trajectory into one.
	 *
	 * @param matchResult Multiple matching results of the same trajectory.
	 */
	public SingleTrajectoryMatchResult(List<SingleTrajectoryMatchResult> matchResult, int requiredNumOfRanks) {
		if (matchResult == null)
			throw new NullPointerException("The matching result list is null.");
		// check whether all the items are the matching results of the same trajectory
		String trajID = matchResult.get(0).getTrajID();
		this.requiredNumOfRanks = requiredNumOfRanks;
		this.numOfRanks = 0;
		for (SingleTrajectoryMatchResult mr : matchResult) {
			if (!mr.getTrajID().equals(trajID))
				throw new IllegalArgumentException("Some of the matching results are from different trajectory: " + mr.getTrajID() +
						"," + trajID);
			if (mr.pointMatchResult.size() != mr.routeMatchResult.size() || mr.pointMatchResult.size() != mr.breakPointBSList.size())
				throw new IllegalArgumentException("Inconsistent size of point/route/breakpoint matching list.");
			this.numOfRanks += mr.numOfRanks;
		}
		if (this.numOfRanks > requiredNumOfRanks)
			throw new IndexOutOfBoundsException("The matching result in the list exceeds the total number of required results.");
		
		this.trajectory = matchResult.get(0).getTrajectory();
		this.pointMatchResult = new ArrayList<>(requiredNumOfRanks);
		this.routeMatchResult = new ArrayList<>(requiredNumOfRanks);
		this.breakPointBSList = new ArrayList<>(requiredNumOfRanks);
		this.probabilities = new double[requiredNumOfRanks];
		int index = 0;    // the number of the currently stored matching result
		for (SingleTrajectoryMatchResult mr : matchResult) {
			this.pointMatchResult.addAll(mr.pointMatchResult);
			this.routeMatchResult.addAll(mr.routeMatchResult);
			this.breakPointBSList.addAll(mr.breakPointBSList);
			System.arraycopy(mr.probabilities, 0, this.probabilities, index, index + mr.pointMatchResult.size() - index);
			index += mr.pointMatchResult.size();
		}
		for (int i = index; i < requiredNumOfRanks; i++)
			this.probabilities[i] = Double.NEGATIVE_INFINITY;
	}
	
	public static SingleTrajectoryMatchResult parseTrajectoryMatchResult(String s, DistanceFunction df) {
		while (s.lastIndexOf("\n") == s.length() - 1)
			s = s.substring(0, s.length() - 1);        // remove the extra lines
		String[] lines = s.split("\n");
		
		// start parsing the first line, which contains global attributes
		String[] firstLine = lines[0].split("\\|");
		if (firstLine.length != 6)
			throw new IllegalArgumentException("The first line of the input text cannot be parsed into MultipleTrajectoryMatchResult attributes: "
					+ lines[0]);
		int requiredNumOfRanks = Integer.parseInt(firstLine[0]);
		int numOfMatches = Integer.parseInt(firstLine[1]);
		int trajectorySize = Integer.parseInt(firstLine[2]);
		String trajectoryID = firstLine[3];
		if (lines.length - 1 != trajectorySize)
			throw new IllegalArgumentException("The input trajectory size is different from the defined size: " + (lines.length - 1)
					+ "," + trajectorySize);
		double[] probabilities = new double[requiredNumOfRanks];
		List<BitSet> breakPointBSList = new ArrayList<>(requiredNumOfRanks);
		String[] probabilityInfo = firstLine[4].split(",");
		String[] breakPointBSInfo = firstLine[5].split("},\\{");
		if (probabilityInfo.length != numOfMatches)
			throw new IllegalArgumentException("The input number of probabilities is inconsistent with the number of matching results: "
					+ probabilityInfo.length + "," + numOfMatches);
		if (breakPointBSInfo.length != numOfMatches)
			throw new IllegalArgumentException("The input number of breakpoint information is inconsistent with the number of matching " +
					"results: " + breakPointBSInfo.length + "," + numOfMatches);
		for (int i = 0; i < numOfMatches; i++) {
			probabilities[i] = Double.parseDouble(probabilityInfo[i]);
			BitSet breakpointBS = new BitSet(trajectorySize);
			if (breakPointBSInfo[i].length() > 2) {    // the BitSet contains values
				String[] indices = breakPointBSInfo[i].replace("{", "").replace("}", "").split(", ");
				for (String index : indices) {
					breakpointBS.set(Integer.parseInt(index));
				}
			}
			breakPointBSList.add(breakpointBS);
		}
		
		// start parsing the rest of the lines
		List<TrajectoryPoint> trajPointList = new ArrayList<>();
		List<List<PointMatch>> pointMatchList = new ArrayList<>(requiredNumOfRanks);
		List<List<Route>> routeMatchList = new ArrayList<>(requiredNumOfRanks);
		for (int i = 0; i < numOfMatches; i++) {    // initialise all match sequences
			pointMatchList.add(new ArrayList<>());
			routeMatchList.add(new ArrayList<>());
		}
		for (int i = 1; i < lines.length; i++) {
			String[] matchInfo = lines[i].split(",");
			trajPointList.add(TrajectoryPoint.parseTrajectoryPoint(matchInfo[0], df));
			String[] pointMatchInfo = matchInfo[1].split("\\|");
			String[] routeMatchInfo = matchInfo[2].split("\\|");
			for (int j = 0; j < numOfMatches; j++) {
				pointMatchList.get(j).add(PointMatch.parsePointMatch(pointMatchInfo[j], df));
				routeMatchList.get(j).add(Route.parseRoute(routeMatchInfo[j], df));
			}
		}
		Trajectory currTraj = new Trajectory(trajectoryID, trajPointList);
		return new SingleTrajectoryMatchResult(currTraj, requiredNumOfRanks, numOfMatches, pointMatchList, routeMatchList, probabilities,
				breakPointBSList);
	}
	
	public String getTrajID() {
		return trajectory.getID();
	}
	
	public Trajectory getTrajectory() {
		return this.trajectory;
	}
	
	public int getTrajSize() {
		return this.trajectory.size();
	}
	
	public TrajectoryPoint getTrajPoint(int position) {
		return this.trajectory.get(position);
	}
	
	public int getActualMatchCount() {
		if (this.numOfRanks != this.pointMatchResult.size() || this.pointMatchResult.size() != this.routeMatchResult.size()
				|| this.pointMatchResult.size() != this.breakPointBSList.size())
			throw new IllegalArgumentException("Inconsistent size of point/route/breakpoint matching list.");
		return numOfRanks;
	}
	
	public int getRequiredNumOfRanks() {
		return requiredNumOfRanks;
	}
	
	/**
	 * Return the point matching result of a trajectory point.
	 *
	 * @param pointIndex The index of the specific point.
	 * @return The point matching result.
	 */
	public PointMatch getPointMatchResult(int pointIndex) {
		return pointMatchResult.get(0).get(pointIndex);
	}
	
	/**
	 * Return the point matching result of a trajectory point.
	 *
	 * @return All the point matching results.
	 */
	public List<List<PointMatch>> getAllPointMatchResult() {
		return pointMatchResult;
	}
	
	/**
	 * Set all the point matching results for the trajectory. The point list must of the same size as the trajectory.
	 *
	 * @param pointMatchList All the point matching results.
	 */
	public void setAllPointMatchResult(List<List<PointMatch>> pointMatchList) {
		if (pointMatchList.size() > requiredNumOfRanks)
			throw new IllegalArgumentException("The point matching result list is larger than the required matching count.");
		for (List<PointMatch> pm : pointMatchList) {
			if (pm.size() != trajectory.size())
				throw new IllegalArgumentException("The point match list doesn't have the same size as the trajectory.");
		}
		this.pointMatchResult = pointMatchList;
		this.numOfRanks = this.pointMatchResult.size();
	}
	
	/**
	 * Get the specified ranked point matching result.
	 *
	 * @param pointIndex The index of the specific point.
	 * @param rankIndex  The rankIndex specified.
	 * @return The point matching result of a trajectory point in the rankIndex-th matching result.
	 */
	public PointMatch getPointMatchResultAtRank(int pointIndex, int rankIndex) {
		if (rankIndex >= numOfRanks) throw new IndexOutOfBoundsException("The specified matching result rankIndex is out of range.");
		return pointMatchResult.get(rankIndex).get(pointIndex);
	}
	
	/**
	 * Set the point matching result for the trajectory. The point list must of the same size as the trajectory.
	 *
	 * @param pointMatchList The new point matching result.
	 */
	public void setPointMatchResult(List<PointMatch> pointMatchList) {
		if (pointMatchList.size() != trajectory.size())
			throw new IllegalArgumentException("The point match list doesn't have the same size as the trajectory.");
		this.pointMatchResult.set(0, pointMatchList);
	}
	
	/**
	 * Set the point matching result for the trajectory. The point list must of the same size as the trajectory.
	 *
	 * @param pointMatchList The new point matching result.
	 * @param rankIndex      The rankIndex specified.
	 */
	public void setPointMatchResultAtRank(List<PointMatch> pointMatchList, int rankIndex) {
		if (rankIndex >= numOfRanks) throw new IndexOutOfBoundsException("The specified matching result rankIndex is out of range.");
		if (pointMatchList.size() != trajectory.size())
			throw new IllegalArgumentException("The point match list doesn't have the same size as the trajectory: " +
					pointMatchList.size() + "," + trajectory.size());
		this.pointMatchResult.set(0, pointMatchList);
	}
	
	/**
	 * Return the route matching result of a trajectory point.
	 *
	 * @param pointIndex The index of the specific point.
	 * @return The point matching result.
	 */
	public Route getRouteMatchResult(int pointIndex) {
		return routeMatchResult.get(0).get(pointIndex);
	}
	
	/**
	 * Return all the route matching result of a trajectory point.
	 *
	 * @return All the point matching result.
	 */
	public List<List<Route>> getAllRouteMatchResult() {
		return routeMatchResult;
	}
	
	/**
	 * Set all the route matching result for the trajectory. The route list must of the same size as the trajectory.
	 *
	 * @param routeMatchLists The new point matching result.
	 */
	public void setAllRouteMatchResult(List<List<Route>> routeMatchLists) {
		if (routeMatchLists.size() > requiredNumOfRanks) throw new IndexOutOfBoundsException("Input route matching results " +
				"have larger size than the requirement.");
		for (List<Route> rm : routeMatchLists) {
			if (rm.size() != trajectory.size())
				throw new IllegalArgumentException("The point match list doesn't have the same size as the trajectory.");
		}
		this.numOfRanks = routeMatchLists.size();
		this.breakPointBSList.clear();
		for (int i = 0; i < routeMatchLists.size(); i++) {
			this.breakPointBSList.add(new BitSet(trajectory.size()));
			for (int j = 1; j < routeMatchLists.size(); j++) {
				if (!routeMatchLists.get(i).get(j - 1).getEndPoint().equals2D(routeMatchLists.get(i).get(j).getStartPoint())) {
					// route is broken
					this.breakPointBSList.get(i).set(j);
				}
			}
		}
		this.routeMatchResult = routeMatchLists;
	}
	
	/**
	 * Get the specified ranked route matching result.
	 *
	 * @param pointIndex The index of the specific point.
	 * @param rankIndex  The rankIndex specified.
	 * @return The route matching result of a trajectory point in the rankIndex-th matching result.
	 */
	public Route getRouteMatchResultAtRank(int pointIndex, int rankIndex) {
		if (rankIndex >= numOfRanks) throw new IndexOutOfBoundsException("The specified matching result rankIndex is out of range.");
		return routeMatchResult.get(rankIndex).get(pointIndex);
	}
	
	/**
	 * Set the route matching result for the trajectory. The route list must of the same size as the trajectory.
	 *
	 * @param routeMatchList The new point matching result.
	 */
	public void setRouteMatchResult(List<Route> routeMatchList) {
		if (routeMatchList.size() != trajectory.size())
			throw new IllegalArgumentException("The point match list doesn't have the same size as the trajectory: " +
					routeMatchList.size() + "," + trajectory.size());
		this.routeMatchResult.set(0, routeMatchList);
		// reset the breakpoint list
		this.breakPointBSList.get(0).clear();
		for (int i = 1; i < routeMatchList.size(); i++) {
			if (!routeMatchList.get(i - 1).getEndPoint().equals2D(routeMatchList.get(i).getStartPoint())) {    // route is broken
				this.breakPointBSList.get(0).set(i);
			}
		}
	}
	
	/**
	 * Set the point matching result for the trajectory. The route list must of the same size as the trajectory.
	 *
	 * @param routeMatchList The new route matching result.
	 * @param rankIndex      The rankIndex specified.
	 */
	public void setRouteMatchResultAtRank(List<Route> routeMatchList, int rankIndex) {
		if (rankIndex >= numOfRanks) throw new IndexOutOfBoundsException("The specified matching result rankIndex is out of range.");
		if (routeMatchList.size() != trajectory.size())
			throw new IllegalArgumentException("The point match list doesn't have the same size as the trajectory: " +
					routeMatchList.size() + "," + trajectory.size());
		this.routeMatchResult.set(rankIndex, routeMatchList);
		// reset the breakpoint list
		this.breakPointBSList.get(rankIndex).clear();
		for (int i = 1; i < routeMatchList.size() - 1; i++) {
			if (!routeMatchList.get(i - 1).getEndPoint().equals2D(routeMatchList.get(i).getStartPoint())) {    // route is broken
				this.breakPointBSList.get(rankIndex).set(i);
			}
		}
	}
	
	public double getProbability() {
		return probabilities[0];
	}
	
	public void setProbability(double probability) {
		this.probabilities[0] = probability;
	}
	
	public double[] getAllProbability() {
		return probabilities;
	}
	
	public double getProbabilityAtRank(int rank) {
		if (rank >= numOfRanks) throw new IndexOutOfBoundsException("The specified rank index is out of range.");
		return probabilities[rank];
	}
	
	public void setAllProbabilities(double[] probabilities) {
		if (probabilities.length > requiredNumOfRanks)
			throw new IllegalArgumentException("The size of the probability set is larger than the requirement: " + probabilities.length
					+ "," + requiredNumOfRanks);
		else if (probabilities.length < requiredNumOfRanks) {
			LOG.warn("The size of the probability set is smaller than the requirement: " + probabilities.length + "," + requiredNumOfRanks);
			System.arraycopy(probabilities, 0, this.probabilities, 0, probabilities.length);
			for (int i = probabilities.length; i < this.probabilities.length; i++) {
				this.probabilities[i] = Double.NEGATIVE_INFINITY;
			}
		} else {
			if (probabilities[numOfRanks - 1] == Double.NEGATIVE_INFINITY)
				throw new IndexOutOfBoundsException("Inconsistent probabilities set size: " + probabilities.length + "," + numOfRanks);
			this.probabilities = probabilities;
		}
	}
	
	public void setProbabilityAtRank(double probability, int rankIndex) {
		if (rankIndex >= numOfRanks) throw new IndexOutOfBoundsException("The specified rank index is out of range.");
		this.probabilities[rankIndex] = probability;
	}
	
	/**
	 * Return the particular map-matching result.
	 *
	 * @param rankIndex The specific rank index.
	 * @return The complete map-matching result.
	 */
	public SingleTrajectoryMatchResult getMatchResultAtRank(int rankIndex) {
		return new SingleTrajectoryMatchResult(trajectory, pointMatchResult.get(rankIndex), routeMatchResult.get(rankIndex),
				probabilities[rankIndex], breakPointBSList.get(rankIndex));
	}
	
	/**
	 * Return a route which contains the entire matching path of the route match result at a given rank.
	 *
	 * @param rankIndex The specific rank index.
	 * @return A route which is the merge result of all the route matches.
	 */
	public Route getCompleteMatchRouteAtRank(int rankIndex) {
		if (rankIndex >= numOfRanks) throw new IndexOutOfBoundsException("way list get failed: the specified rank is out of range.");
		List<Route> routeList = routeMatchResult.get(rankIndex);
		Route currRoute = routeList.get(0).clone();
		for (int i = 1; i < routeList.size(); i++) {
			currRoute.addRoute(routeList.get(i));
		}
		return currRoute;
	}
	
	/**
	 * Format: 1. requiredNumOfRanks|numOfRank|trajectorySize|trajectoryID|probability1,probability2,...|breakpointBS1,breakpointBS2,...
	 * 2. trajectoryPoint1,pointMatch1-1|pointMatch1-2|pointMatch1-3,routeMatch1-1|routeMatch1-2|routeMatch1-3
	 * 3. trajectoryPoint2,pointMatch2-1|pointMatch2-2|pointMatch2-3,routeMatch2-1|routeMatch2-2|routeMatch2-3
	 * 4. ...
	 *
	 * @return The result of the map-matching.
	 */
	@Override
	public String toString() {
		// validate object data
		if (this.requiredNumOfRanks < this.numOfRanks || this.numOfRanks <= 0)
			throw new IllegalArgumentException("The actual number of matching results is larger than the requirement.");
		if (this.numOfRanks != this.pointMatchResult.size() || this.numOfRanks != this.routeMatchResult.size()
				|| this.numOfRanks != this.breakPointBSList.size())
			throw new IllegalArgumentException("Inconsistent size of point/route/breakpoint matching list.");
		for (int i = 0; i < numOfRanks; i++) {
			if (this.trajectory.size() != pointMatchResult.get(i).size() || this.trajectory.size() != this.routeMatchResult.get(i).size())
				throw new IllegalArgumentException("The size of the matching result is inconsistent with the trajectory size. rank " + i + ":"
						+ this.trajectory.size() + "_" + this.pointMatchResult.get(i).size() + "_" + this.routeMatchResult.get(i).size());
		}
		StringBuilder line = new StringBuilder();
		
		// the first line is the general attributes. Format: requiredNumOfRanks|numOfRank|trajectorySize|trajectoryID|probability1,
		// probability2,...|breakpointBS1,breakpointBS2,...
		line.append(requiredNumOfRanks).append("|").append(numOfRanks).append("|").append(trajectory.size()).append("|")
				.append(trajectory.getID()).append("|");
		for (int i = 0; i < numOfRanks - 1; i++) {
			if (probabilities[i] == Double.NEGATIVE_INFINITY)
				throw new IndexOutOfBoundsException("The number of probabilities is less than the number of matching results: " + i + ","
						+ numOfRanks);
			line.append(probabilities[i]).append(",");
		}
		line.append(probabilities[numOfRanks - 1]);
		if (probabilities.length > numOfRanks && probabilities[numOfRanks] != Double.NEGATIVE_INFINITY)
			throw new IndexOutOfBoundsException("The number of probabilities is more than the number of matching results.");
		line.append("|");
		for (int i = 0; i < numOfRanks - 1; i++) {
			line.append(breakPointBSList.get(i).toString()).append(",");
		}
		line.append(breakPointBSList.get(numOfRanks - 1).toString());
		line.append("\n");
		
		// the actual matching result starts from the second line
		for (int i = 0; i < trajectory.size(); i++) {
			line.append(this.trajectory.get(i).toString()).append(",");
			for (int j = 0; j < numOfRanks - 1; j++) {
				line.append(this.pointMatchResult.get(j).get(i).toString()).append("|");
			}
			line.append(this.pointMatchResult.get(numOfRanks - 1).get(i).toString()).append(",");
			for (int j = 0; j < numOfRanks - 1; j++) {
				line.append(this.routeMatchResult.get(j).get(i).toString()).append("|");
			}
			line.append(this.routeMatchResult.get(numOfRanks - 1).get(i).toString()).append("\n");
		}
		return line.toString();
	}
}