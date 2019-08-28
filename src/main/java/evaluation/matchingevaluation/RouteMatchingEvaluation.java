package evaluation.matchingevaluation;

import org.apache.log4j.Logger;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Trajectory;
import util.object.structure.Pair;

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
	 * @param currentMap        The underlying map.
	 * @param removedEdges      The removed edges. Only used to evaluate the map update result. =empty for normal map-matching evaluation.
	 * @return precision, recall, F-score
	 */
	public static String precisionRecallFScoreAccEvaluation(List<Pair<Integer, List<String>>> matchedResultList, List<Pair<Integer,
			List<String>>> gtResultList, RoadNetworkGraph currentMap, List<RoadWay> removedEdges) {
		DecimalFormat df = new DecimalFormat("0.000");
		// insert all ground truth road match into id2GTResultMapping
		Map<Integer, HashSet<String>> id2GTResultMapping = new HashMap<>();
		for (Pair<Integer, List<String>> gtResult : gtResultList) {
			HashSet<String> gtRoadIDList = new LinkedHashSet<>(gtResult._2());
			id2GTResultMapping.put(gtResult._1(), gtRoadIDList);
		}
		
		// prepare the mapping of road id to road way length
		Map<String, Double> id2RoadLength = new HashMap<>();
		for (RoadWay w : currentMap.getWays())
			id2RoadLength.put(w.getID(), w.getLength());
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
		double lengthAcc = totalMatchedLength / totalUniqueLength;
		String lengthPrecisionString = convertPercentage(lengthPrecision, df);
		String lengthRecallString = convertPercentage(lengthRecall, df);
		String lengthFScoreString = convertPercentage(lengthFScore, df);
		String lengthAccString = convertPercentage(lengthAcc, df);
		LOG.info("Map-matching result evaluated, length precision: " + lengthPrecisionString + "%, length recall:" + lengthRecallString +
				"%, F-score: " + lengthFScoreString + "%, length acc: " + lengthAccString + "%.");
		return lengthPrecisionString + "," + lengthRecallString + "," + lengthFScoreString + "," + lengthAccString;
	}
	
	/**
	 * The precision/recall/f-measure evaluation of the matching result in Global dataset
	 *
	 * @param matchedResult     Matching results generated by algorithm
	 * @param groundTruthResult The ground-truth matching result
	 */
	public static String globalPrecisionRecallEvaluation(List<Pair<Integer, List<String>>> matchedResult, List<Pair<Integer,
			List<String>>> groundTruthResult, List<RoadNetworkGraph> mapList) {
		// insert all ground truth road match into globalCompareList
		DecimalFormat df = new DecimalFormat(".000");
		Map<Integer, HashSet<String>> globalCompareList = new HashMap<>();
		for (Pair<Integer, List<String>> gtResult : groundTruthResult) {
			HashSet<String> gtRoadIDList = new LinkedHashSet<>(gtResult._2());
			globalCompareList.put(gtResult._1(), gtRoadIDList);
		}
		
		// start the comparison
		double totalCorrectlyMatchedLength = 0;      // total length of perfectly matched road ways
		double totalMatchedLength = 0;    // total length of the road ways that are matched incorrectly
		double totalGroundTruthLength = 0;    // total length of the ground-truth road ways
		
		for (Pair<Integer, List<String>> r : matchedResult) {
			
			// read the corresponding map to extract actual length of each road
			RoadNetworkGraph currMap = mapList.get(r._1());
			Map<String, Double> id2RoadLength = new HashMap<>();
			for (RoadWay w : currMap.getWays())
				id2RoadLength.put(w.getID(), w.getLength());
			
			Set<String> matchRoadIDSet = new LinkedHashSet<>(r._2());
			double currMatchedLength = 0;
			// summarize all matched road length
			for (String s : matchRoadIDSet) {
				if (id2RoadLength.containsKey(s)) {
					currMatchedLength += id2RoadLength.get(s);
				} else
					LOG.warn("Road " + s + " is missing in the map. Inconsistency between map and ground-truth matching result.");
			}
			double correctlyMatchedLength = 0;
			// check the coverage of the roads found in our match
			HashSet<String> groundTruthIDList = globalCompareList.get(r._1());
			double currGroundTruthLength = 0;
			for (String s : groundTruthIDList) {
				if (id2RoadLength.containsKey(s)) {
					double currLength = id2RoadLength.get(s);
					currGroundTruthLength += currLength;
					if (matchRoadIDSet.contains(s)) {
						correctlyMatchedLength += currLength;
					}
				} else
					LOG.warn("Road " + s + " is missing in the map. Inconsistency between map and ground-truth matching result.");
			}
			LOG.info("Trajectory " + r._1() + ": Precision=" + correctlyMatchedLength / currMatchedLength + ", " +
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
	 * @param currentMap        Underlying map.
	 * @return Evaluation result.
	 */
	public static String rmfEvaluation(List<Pair<Integer, List<String>>> matchedResultList, List<Pair<Integer, List<String>>> gtResultList,
									   RoadNetworkGraph currentMap) {
		// insert all ground truth road match into trajID2GTResultMapping
		Map<Integer, HashSet<String>> trajID2GTResultMapping = new HashMap<>();
		for (Pair<Integer, List<String>> gtResult : gtResultList) {
			HashSet<String> gtRoadIDList = new LinkedHashSet<>(gtResult._2());
			trajID2GTResultMapping.put(gtResult._1(), gtRoadIDList);
		}
		
		// prepare the mapping of road id to road way length
		Map<String, Double> id2RoadLength = new HashMap<>();
		for (RoadWay w : currentMap.getWays())
			id2RoadLength.put(w.getID(), w.getLength());
		
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
	 * @param currentMap        The underlying map.
	 * @return The measure result.
	 */
	public static String nonGTEvaluation(List<Pair<Integer, List<String>>> matchedResultList,
										 List<Trajectory> trajectoryList, RoadNetworkGraph currentMap) {
		DecimalFormat df = new DecimalFormat("0.000");
		
		// prepare the mapping of road id to road way length
		Map<String, Double> id2RoadLength = new HashMap<>();
		for (RoadWay w : currentMap.getWays())
			id2RoadLength.put(w.getID(), w.getLength());
		
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