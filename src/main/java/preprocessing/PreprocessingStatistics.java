package preprocessing;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Rect;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.Pair;
import util.object.structure.PointMatch;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculate the statistics of the input dataset.
 *
 * @author uqpchao
 * Created 14/05/2019
 */
public class PreprocessingStatistics {
	
	private static final Logger LOG = Logger.getLogger(PreprocessingStatistics.class);
	
	/**
	 * Calculate the statistics of trajectory dataset and the underlying map.
	 *
	 * @param inputTrajList          Input trajectory list.
	 * @param inputMap               Underlying map.
	 * @param gtPointMatchResultList Ground-truth point match result list.
	 */
	static void datasetStatsCalc(List<Trajectory> inputTrajList, RoadNetworkGraph inputMap, List<Pair<Integer, List<PointMatch>>> gtPointMatchResultList) {
		DistanceFunction df = inputMap.getDistanceFunction();
		// trajectory stats
		int trajPointCount = 0;
		long minTimeDiff = Long.MAX_VALUE;
		long maxTimeDiff = Long.MIN_VALUE;
		long totalTimeDiff = 0;
		boolean isValidTraj;
		Map<Long, Integer> samplingRateCount = new LinkedHashMap<>();
		long prevTime = 0;
		long currTimeDiff;
		Map<Integer, Trajectory> id2TrajectoryMapping = new HashMap<>();
		for (Trajectory traj : inputTrajList) {
			isValidTraj = true;
			for (int i = 0; i < traj.size(); i++) {
				TrajectoryPoint point = traj.get(i);
				if (!isValidTraj) {
					trajPointCount += i;
					break;
				}
				if (i != 0) {
					if (point.time() < prevTime) {
						LOG.error("The current trajectory point earlier than the previous one.");
						isValidTraj = false;
					} else {
						currTimeDiff = point.time() - prevTime;
						minTimeDiff = Math.min(minTimeDiff, currTimeDiff);
						maxTimeDiff = Math.max(maxTimeDiff, currTimeDiff);
						totalTimeDiff += currTimeDiff;
						if (!samplingRateCount.containsKey(currTimeDiff)) {
							samplingRateCount.put(currTimeDiff, 1);
						} else {
							samplingRateCount.replace(currTimeDiff, samplingRateCount.get(currTimeDiff) + 1);
						}
					}
				}
				
				prevTime = point.time();
			}
			if (isValidTraj) {
				trajPointCount += traj.size();
				id2TrajectoryMapping.put(Integer.parseInt(traj.getID()), traj);
			}
		}
		double maxGPSError = 0;
		double totalGPSError = 0;
		int pointCount = 0;
		for (Pair<Integer, List<PointMatch>> gtPointMatchResult : gtPointMatchResultList) {
			if (id2TrajectoryMapping.containsKey(gtPointMatchResult._1())) {
				Trajectory currTraj = id2TrajectoryMapping.get(gtPointMatchResult._1());
				List<PointMatch> pointMatches = gtPointMatchResult._2();
				for (int i = 0; i < pointMatches.size(); i++) {
					double currGPSError = df.distance(pointMatches.get(i).getMatchPoint(), currTraj.get(i));
					totalGPSError += currGPSError;
					maxGPSError = Math.max(maxGPSError, currGPSError);
					pointCount++;
				}
			}
		}
		double avgGPSError = totalGPSError / trajPointCount;
		double stdGPSError = 0;
		for (Pair<Integer, List<PointMatch>> gtPointMatchResult : gtPointMatchResultList) {
			if (id2TrajectoryMapping.containsKey(gtPointMatchResult._1())) {
				Trajectory currTraj = id2TrajectoryMapping.get(gtPointMatchResult._1());
				List<PointMatch> pointMatches = gtPointMatchResult._2();
				for (int i = 0; i < pointMatches.size(); i++) {
					double currGPSDiffPow = Math.pow((df.distance(pointMatches.get(i).getMatchPoint(), currTraj.get(i)) - avgGPSError), 2);
					stdGPSError += currGPSDiffPow;
				}
			}
		}
		stdGPSError = Math.sqrt(stdGPSError / pointCount);
		LOG.info("Total number of trajectories: " + inputTrajList.size() + ", trajectory point count: " + trajPointCount);
		LOG.info("Average sampling rate is : " + totalTimeDiff / (double) (trajPointCount - inputTrajList.size()) + ". Min time interval: "
				+ minTimeDiff + " sec, max time interval: " + maxTimeDiff + " sec");
		LOG.info("Average GPS measurement error is :" + avgGPSError + " m, maximum error: " + maxGPSError + " m, standard deviation: " + stdGPSError);
		
		// road network stats
		int vertexCount = inputMap.getNodes().size();
		int miniNodeCount = 0;
		int roadWayCount = inputMap.getWays().size();
		double totalRoadWayLength = 0;
		Rect mapBoundary = inputMap.getBoundary();
		double mapSize = df.area(mapBoundary) / 1000000;    // area of the map(km^2)
		
		// length of the boundaries, left and right should be always the same, while top and bottom can be different when in WGS84
		double mapScaleXTop = df.pointToPointDistance(mapBoundary.maxX(), mapBoundary.maxY(), mapBoundary.minX(), mapBoundary.maxY());
		double mapScaleXBottom = df.pointToPointDistance(mapBoundary.maxX(), mapBoundary.minY(), mapBoundary.minX(), mapBoundary.minY());
		double mapScaleYLeft = df.pointToPointDistance(mapBoundary.minX(), mapBoundary.maxY(), mapBoundary.minX(), mapBoundary.minY());
		double mapScaleYRight = df.pointToPointDistance(mapBoundary.maxX(), mapBoundary.maxY(), mapBoundary.maxX(), mapBoundary.minY());
		LOG.info("Boundary is: " + mapBoundary.minX() + ", " + mapBoundary.maxX() + ", " + mapBoundary.minY() + ", " + mapBoundary.maxY());
		LOG.info("Map size: " + mapSize + " km^2, the boundary lengths are: (top) " + mapScaleXTop + " (bottom) " + mapScaleXBottom +
				" (left) " + mapScaleYLeft + " (right) " + mapScaleYRight);
		
		double noVisitRoadLength = 0;
		double lowVisitRoadLength = 0;
		int noVisitRoadCount = 0;
		int lowVisitRoadCount = 0;
		for (RoadWay w : inputMap.getWays()) {
			miniNodeCount += w.getNodes().size() - 2;
			totalRoadWayLength += w.getLength();
			if (w.getVisitCount() == 0) {
				noVisitRoadCount++;
				noVisitRoadLength += w.getLength();
			} else if (w.getVisitCount() <= 5) {
				lowVisitRoadCount++;
				lowVisitRoadLength += w.getLength();
			}
		}
		double trajDensity = trajPointCount / mapSize;    // pts/km^2
		double mapDensity = totalRoadWayLength / mapSize;
		LOG.info("Map size is: " + mapSize + " km^2");
		LOG.info("Road Node count: " + vertexCount + " + " + miniNodeCount + ", road way count: " + roadWayCount);
		LOG.info("Road unvisited count: " + noVisitRoadCount / (double) inputMap.getWays().size() + ", length: " + noVisitRoadLength / totalRoadWayLength);
		LOG.info("Road low visited(<=5) count: " + lowVisitRoadCount / (double) inputMap.getWays().size() + ", length: " + lowVisitRoadLength / totalRoadWayLength);
		LOG.info("Map density: " + mapDensity / 1000 + " km/km^2, trajectory density: " + trajDensity + " pts/km^2");
	}
}
