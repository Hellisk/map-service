package evaluation.matchingevaluation;

import org.apache.log4j.Logger;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadWay;
import util.object.structure.Pair;

import java.text.DecimalFormat;
import java.util.*;

/**
 * The evaluation session
 * TODO refactor this part
 *
 * @author Hellisk
 * @since 10/07/2017
 */
public class RouteMatchingEvaluation {
	
	private static final Logger LOG = Logger.getLogger(RouteMatchingEvaluation.class);
	
	/**
	 * Evaluate the precision, recall and F-score of the Beijing map-matching
	 *
	 * @param matchedResultList The matching results of map-matching algorithm
	 * @param gtResultList      The ground-truth matching results
	 * @param currentMap        The underlying map used in map-matching
	 * @param removedEdges      The removed edges. They may appear in the ground-truth results if it is used to evaluate the map update
	 *                          result. Leave empty if evaluation normal map-matching.
	 */
	public static String precisionRecallEvaluation(List<Pair<Integer, List<String>>> matchedResultList, List<Pair<Integer,
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
		
		for (Pair<Integer, List<String>> r : matchedResultList) {
			double currMatchedLength = 0;
			double currGroundTruthLength = 0;
			double currCorrectlyMatchedLength = 0;
			Set<String> matchRoadIDSet = new LinkedHashSet<>(r._2());
			// summarize all matched road length
			for (String s : matchRoadIDSet) {
				if (id2RoadLength.containsKey(s)) {
					totalMatchedLength += id2RoadLength.get(s);
					currMatchedLength += id2RoadLength.get(s);
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
					}
				} else
					LOG.warn("Road " + s + " is missing from the map.");
			}
			if (matchedResultList.size() != 1) {
				double currLengthPrecision = currCorrectlyMatchedLength / currMatchedLength;
				double currLengthRecall = currCorrectlyMatchedLength / currGroundTruthLength;
				double currLengthFScore = 2 * (currLengthPrecision * currLengthRecall / (currLengthPrecision + currLengthRecall));
				String currLengthPrecisionString = convertPercentage(currLengthPrecision, df);
				String currLengthRecallString = convertPercentage(currLengthRecall, df);
				String currLengthFScoreString = convertPercentage(currLengthFScore, df);
				LOG.debug("Map-matching result evaluated, length precision: " + currLengthPrecisionString + "%, length recall:"
						+ currLengthRecallString + "%, F-score: " + currLengthFScoreString + "%.");
			}
		}
		
		double lengthPrecision = totalCorrectlyMatchedLength / totalMatchedLength;
		double lengthRecall = totalCorrectlyMatchedLength / totalGroundTruthLength;
		double lengthFScore = 2 * (lengthPrecision * lengthRecall / (lengthPrecision + lengthRecall));
		String lengthPrecisionString = convertPercentage(lengthPrecision, df);
		String lengthRecallString = convertPercentage(lengthRecall, df);
		String lengthFScoreString = convertPercentage(lengthFScore, df);
		LOG.info("Map-matching result evaluated, length precision: " + lengthPrecisionString + "%, length recall:" + lengthRecallString +
				"%, F-score: " + lengthFScoreString + "%.");
		return lengthPrecisionString + "," + lengthRecallString + "," + lengthFScoreString;
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