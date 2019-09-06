package evaluation.matchingevaluation;

import org.apache.log4j.Logger;
import util.io.GlobalMapLoader;
import util.io.GlobalTrajectoryLoader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Trajectory;
import util.object.structure.Pair;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * The evaluation session used for measuring the accuracy of route matching result. Each route match result is represented by a list of
 * String presenting the ID of the matched roads.
 *
 * @author Hellisk
 * @since 10/07/2017
 */
public class RouteMatchingEvaluation {
	
	private static final Logger LOG = Logger.getLogger(RouteMatchingEvaluation.class);
	
	/**
	 * Evaluate the precision, recall, F-score and acc (similar to Jaccard Similarity) of the route match results.
	 *
	 * @param matchedResultList The route match result list.
	 * @param gtResultList      The ground-truth match result list.
	 * @param id2RoadLength     Road id to its length on map.
	 * @param removedEdges      The removed edges. Only used to evaluate the map update result. =empty for normal map-matching evaluation.
	 * @return precision, recall, F-score
	 */
	public static String precisionRecallFScoreAccEvaluation(List<Pair<Integer, List<String>>> matchedResultList, List<Pair<Integer,
			List<String>>> gtResultList, Map<String, Double> id2RoadLength, List<RoadWay> removedEdges) {
		DecimalFormat df = new DecimalFormat("0.000");
		// insert all ground truth road match into id2GTResultMapping
		Map<Integer, HashSet<String>> id2GTResultMapping = new HashMap<>();
		for (Pair<Integer, List<String>> gtResult : gtResultList) {
			HashSet<String> gtRoadIDList = new LinkedHashSet<>(gtResult._2());
			id2GTResultMapping.put(gtResult._1(), gtRoadIDList);
		}
		
		// prepare the mapping of road id to road way length
		if (removedEdges != null && !removedEdges.isEmpty()) {
			for (RoadWay w : removedEdges) {
				if (!id2RoadLength.containsKey(w.getID()))
					id2RoadLength.put(w.getID(), w.getLength());
			}
		}
		// start the count

//		double totalCorrectlyMatchedCount = 0;      // total length of perfectly matched road ways
//		double totalMatchedCount = 0;    // total length of the road ways that are matched incorrectly
//		double totalGroundTruthCount = 0;    // total length of the ground-truth road ways
		double totalCorrectlyMatchedLength = 0;      // total length of perfectly matched road ways
		double totalMatchedLength = 0;    // total length of the road ways that are matched incorrectly
		double totalGroundTruthLength = 0;    // total length of the ground-truth road ways
		double totalUniqueLength = 0;    // total length of the roads that appear at least once in output or ground-truth
		for (Pair<Integer, List<String>> r : matchedResultList) {
			double currMatchedLength = 0;
			double currGroundTruthLength = 0;
			double currCorrectlyMatchedLength = 0;
			double currUniqueLength = 0;
			Set<String> matchRoadIDSet = new LinkedHashSet<>(r._2());
			// summarize all matched road length
			for (String s : matchRoadIDSet) {
				if (id2RoadLength.containsKey(s)) {
					totalMatchedLength += id2RoadLength.get(s);
					currMatchedLength += id2RoadLength.get(s);
					totalUniqueLength += id2RoadLength.get(s);
					currUniqueLength += id2RoadLength.get(s);
				} else
					LOG.warn("Road " + s + " is missing from the map.");
			}
			// check the coverage of the roads found in our match
			HashSet<String> groundTruthIDList = id2GTResultMapping.get(r._1());
			if (groundTruthIDList == null)
				throw new NullPointerException("ERROR! Ground-truth of " + r._1() + " is not found.");
			for (String s : groundTruthIDList) {
				if (id2RoadLength.containsKey(s)) {
					double currLength = id2RoadLength.get(s);
					totalGroundTruthLength += currLength;
					currGroundTruthLength += currLength;
					if (matchRoadIDSet.contains(s)) {
						totalCorrectlyMatchedLength += currLength;
						currCorrectlyMatchedLength += currLength;
					} else {
						totalUniqueLength += currLength;
						currUniqueLength += currLength;
					}
				} else
					LOG.warn("Road " + s + " is missing from the map.");
			}
			if (matchedResultList.size() != 1) {
				double currLengthPrecision = currCorrectlyMatchedLength / currMatchedLength;
				double currLengthRecall = currCorrectlyMatchedLength / currGroundTruthLength;
				double currLengthFScore = 2 * (currLengthPrecision * currLengthRecall / (currLengthPrecision + currLengthRecall));
				double currLengthAcc = currMatchedLength / currUniqueLength;
				String currLengthPrecisionString = convertPercentage(currLengthPrecision, df);
				String currLengthRecallString = convertPercentage(currLengthRecall, df);
				String currLengthFScoreString = convertPercentage(currLengthFScore, df);
				String currLengthAccString = convertPercentage(currLengthAcc, df);
				LOG.debug("Current map-matching result evaluated, length precision: " + currLengthPrecisionString + "%, length recall: "
						+ currLengthRecallString + "%, length F-score: " + currLengthFScoreString + "%, length acc: " + currLengthAccString
						+ "%.");
			}
		}
		
		double lengthPrecision = totalCorrectlyMatchedLength / totalMatchedLength;
		double lengthRecall = totalCorrectlyMatchedLength / totalGroundTruthLength;
		double lengthFScore = 2 * (lengthPrecision * lengthRecall / (lengthPrecision + lengthRecall));
		double lengthAcc = totalCorrectlyMatchedLength / totalUniqueLength;
		String lengthPrecisionString = convertPercentage(lengthPrecision, df);
		String lengthRecallString = convertPercentage(lengthRecall, df);
		String lengthFScoreString = convertPercentage(lengthFScore, df);
		String lengthAccString = convertPercentage(lengthAcc, df);
		LOG.info("Map-matching result evaluated, length precision: " + lengthPrecisionString + "%, length recall:" + lengthRecallString +
				"%, F-score: " + lengthFScoreString + "%, length acc: " + lengthAccString + "%.");
		return lengthPrecisionString + "," + lengthRecallString + "," + lengthFScoreString + "," + lengthAccString;
	}
	
	/**
	 * The precision/recall/f-measure/accuracy evaluation of the matching result in Global dataset
	 *
	 * @param id2RouteMatchResult Output result mapping.
	 * @param rawDataFolder       Data folder for map and ground-truth.
	 */
	public static String globalPrecisionRecallEvaluation(Map<Integer, List<String>> id2RouteMatchResult, String rawDataFolder) throws IOException {
		GlobalTrajectoryLoader trajReader = new GlobalTrajectoryLoader(rawDataFolder);
		GlobalMapLoader mapReader = new GlobalMapLoader(rawDataFolder);
		// insert all ground truth road match into globalCompareList
		DecimalFormat df = new DecimalFormat(".000");
		
		// start the comparison
		double totalCorrectlyMatchedLength = 0;      // total length of perfectly matched road ways
		double totalMatchedLength = 0;    // total length of the road ways that are matched incorrectly
		double totalGroundTruthLength = 0;    // total length of the ground-truth road ways
		
		for (int i = 0; i < id2RouteMatchResult.size(); i++) {
			// read the corresponding map to extract actual length of each road
			RoadNetworkGraph map = mapReader.readRawMap(i);
			Map<String, Double> id2RoadLength = new HashMap<>();
			for (RoadWay w : map.getWays())
				id2RoadLength.put(w.getID(), w.getLength());
			
			Set<String> outputRouteResult = new HashSet<>(id2RouteMatchResult.get(i));
			double currMatchedLength = 0;
			// summarize all matched road length
			for (String s : outputRouteResult) {
				if (id2RoadLength.containsKey(s)) {
					currMatchedLength += id2RoadLength.get(s);
				} else
					LOG.debug("Road " + s + " is missing in the map. Inconsistency between map and ground-truth matching result.");
			}
			
			double correctlyMatchedLength = 0;
			// check the coverage of the roads found in our match
			Set<String> gtResultSet = new HashSet<>(trajReader.readGTRouteMatchResult(i));
			double currGroundTruthLength = 0;
			for (String s : gtResultSet) {
				if (id2RoadLength.containsKey(s)) {
					double currLength = id2RoadLength.get(s);
					currGroundTruthLength += currLength;
					if (outputRouteResult.contains(s)) {
						correctlyMatchedLength += currLength;
					}
				} else
					LOG.debug("Road " + s + " is missing in the map. Inconsistency between map and ground-truth matching result.");
			}
			if (correctlyMatchedLength / currMatchedLength > 1)
				System.out.println("TEST");
			LOG.info("Trajectory " + i + ": Precision=" + correctlyMatchedLength / currMatchedLength + ", " +
					"recall=" + correctlyMatchedLength / currGroundTruthLength);
			totalMatchedLength += currMatchedLength;
			totalCorrectlyMatchedLength += correctlyMatchedLength;
			totalGroundTruthLength += currGroundTruthLength;
		}
		
		double precision = totalCorrectlyMatchedLength / totalMatchedLength;
		double recall = totalCorrectlyMatchedLength / totalGroundTruthLength;
		double fScore = 2 * (precision * recall / (precision + recall));
		String precisionString = convertPercentage(precision, df);
		String recallString = convertPercentage(recall, df);
		String fScoreString = convertPercentage(fScore, df);
		LOG.info("Map-matching result evaluated, precision: " + precisionString + "%, recall:" + recallString + "%, F-score: " + fScoreString + "%.");
		return precisionString + "," + recallString + "," + fScoreString;
	}
	
	/**
	 * Route Match Fraction (RMF) proposed in:
	 * <p>
	 * Newson, Paul, and John Krumm. "Hidden Markov map matching through noise and sparseness." Proceedings of the 17th ACM SIGSPATIAL
	 * international conference on advances in geographic information systems. ACM, 2009.
	 *
	 * @param matchedResultList Output route match result list.
	 * @param gtResultList      Ground-truth route match result list.
	 * @param id2RoadLength     Road id to its length on map.
	 * @return Evaluation result.
	 */
	public static String rmfEvaluation(List<Pair<Integer, List<String>>> matchedResultList, List<Pair<Integer, List<String>>> gtResultList,
									   Map<String, Double> id2RoadLength) {
		// insert all ground truth road match into trajID2GTResultMapping
		Map<Integer, HashSet<String>> trajID2GTResultMapping = new HashMap<>();
		for (Pair<Integer, List<String>> gtResult : gtResultList) {
			HashSet<String> gtRoadIDList = new LinkedHashSet<>(gtResult._2());
			trajID2GTResultMapping.put(gtResult._1(), gtRoadIDList);
		}
		
		double totalFalsePositiveLength = 0;    // total length of the incorrect output roads
		double totalFalseNegativeLength = 0;    // total length of the missing ground-truth roads
		double totalGroundTruthLength = 0;    // total length of the ground-truth road ways
		for (Pair<Integer, List<String>> r : matchedResultList) {
			Set<String> matchRoadIDSet = new LinkedHashSet<>(r._2());
			// summarize all matched road length
			for (String s : matchRoadIDSet) {
				if (id2RoadLength.containsKey(s)) {
					totalFalsePositiveLength += id2RoadLength.get(s);    // the total length of all positive roads
				} else
					LOG.warn("Road " + s + " is missing from the map.");
			}
			// check the coverage of the roads found in our match
			HashSet<String> groundTruthIDSet = trajID2GTResultMapping.get(r._1());
			if (groundTruthIDSet == null)
				throw new NullPointerException("ERROR! Ground-truth of " + r._1() + " is not found.");
			for (String s : groundTruthIDSet) {
				if (id2RoadLength.containsKey(s)) {
					double currLength = id2RoadLength.get(s);
					totalGroundTruthLength += currLength;
					if (matchRoadIDSet.contains(s)) {
						totalFalsePositiveLength -= currLength;    // the true positive roads are subtracted
					} else {
						totalFalseNegativeLength += currLength;
					}
				} else
					LOG.warn("Road " + s + " is missing from the map.");
			}
		}
		double rmf = (totalFalsePositiveLength + totalFalseNegativeLength) / totalGroundTruthLength;
		LOG.info("Map-matching result evaluated, Route Mismatch Fraction (RMF): " + rmf + ".");
		return rmf + "";
	}
	
	/**
	 * Measures that evaluate the matching result without the use of ground-truth, which basically comparing the length of trajectory
	 * with the length of matching result. Reference:
	 * <p>
	 * Schweizer, J., Bernardi, S., Rupi, F.: Map-matching algorithm applied to bicycle global positioning system traces in bologna. IET
	 * Intelligent Transport Systems 10(4), 244{250 (2016)
	 *
	 * @param matchedResultList List of output route match results.
	 * @param trajectoryList    List of original trajectories.
	 * @param id2RoadLength     Road id to its length on map.
	 * @return The measure result.
	 */
	public static String nonGTEvaluation(List<Pair<Integer, List<String>>> matchedResultList,
										 List<Trajectory> trajectoryList, Map<String, Double> id2RoadLength) {
		
		// prepare the mapping of road id to road way length
		
		double totalMatchLength = 0;    // total length of the incorrect output roads
		double totalTrajLength = 0;    // total length of the missing ground-truth roads
		for (Pair<Integer, List<String>> r : matchedResultList) {
			Set<String> matchRoadIDSet = new LinkedHashSet<>(r._2());
			// summarize all matched road length
			for (String s : matchRoadIDSet) {
				if (id2RoadLength.containsKey(s)) {
					totalMatchLength += id2RoadLength.get(s);    // the total length of all positive roads
				} else
					LOG.warn("Road " + s + " is missing from the map.");
			}
			// summarize trajectory length
			for (Trajectory currTraj : trajectoryList) {
				totalTrajLength += currTraj.length();
			}
		}
		double il = totalMatchLength / totalTrajLength;
		LOG.info("Map-matching result evaluated, non-ground-truth measure: " + il + ".");
		return il + "";
	}
	
	/**
	 * Convert the current decimal into percentage, =0 if the current decimal is not a number (already divided by zero)
	 *
	 * @param decimal Input decimal.
	 * @param df      Decimal format.
	 * @return The number * 100.
	 */
	private static String convertPercentage(double decimal, DecimalFormat df) {
		return df.format(Double.isNaN(decimal) ? 0 : decimal * 100);
	}
}