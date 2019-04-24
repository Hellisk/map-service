package evaluation;

import org.apache.log4j.Logger;
import util.io.GlobalMapLoader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadWay;
import util.object.structure.Pair;
import util.object.structure.TrajectoryMatchResult;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * The evaluation session
 * TODO refactor this part
 *
 * @author Hellisk
 * @since 10/07/2017
 */
public class ResultEvaluation {
	
	private static final Logger LOG = Logger.getLogger(ResultEvaluation.class);
	
	private List<String> mapMatchingLengthResult = new ArrayList<>();
	private List<String> mapMatchingCountResult = new ArrayList<>();
	private List<String> mapUpdateLengthResult = new ArrayList<>();
	private List<String> mapUpdateCountResult = new ArrayList<>();
	
	public List<String> getMapMatchingLengthResult() {
		return mapMatchingLengthResult;
	}
	
	public List<String> getMapUpdateLengthResult() {
		return mapUpdateLengthResult;
	}
	
	public List<String> getMapMatchingCountResult() {
		return mapMatchingCountResult;
	}
	
	public List<String> getMapUpdateCountResult() {
		return mapUpdateCountResult;
	}
	
	/**
	 * Evaluate the precision, recall and F-score of the Beijing map-matching
	 *
	 * @param matchedResult     The matching results of map-matching algorithm
	 * @param groundTruthResult The ground-truth matching results
	 * @param currentMap        The map used in current map-matching
	 * @param removedEdges      The removed edges, the combination of removed edges and currentMap will generate a complete road way dictionary
	 */
	public void beijingMapMatchingEval(List<TrajectoryMatchResult> matchedResult, List<Pair<Integer, List<String>>>
			groundTruthResult, RoadNetworkGraph currentMap, List<RoadWay> removedEdges) {
		
		DecimalFormat df = new DecimalFormat("0.000");
		// insert all ground truth road match into gtResultList
		Map<Integer, HashSet<String>> gtResultList = new HashMap<>();
		for (Pair<Integer, List<String>> gtResult : groundTruthResult) {
			HashSet<String> gtRoadIDList = new HashSet<>(gtResult._2());
			gtResultList.put(gtResult._1(), gtRoadIDList);
		}
		
		// prepare the mapping of road id to road way length
		Map<String, Double> id2RoadLength = new HashMap<>();
		for (RoadWay w : currentMap.getWays())
			id2RoadLength.put(w.getID(), w.getLength());
		for (RoadWay w : removedEdges) {
			if (!id2RoadLength.containsKey(w.getID()))
				id2RoadLength.put(w.getID(), w.getLength());
		}
		// start the count
		
		double totalCorrectlyMatchedCount = 0;      // total length of perfectly matched road ways
		double totalMatchedCount = 0;    // total length of the road ways that are matched incorrectly
		double totalGroundTruthCount = 0;    // total length of the ground-truth road ways
		double totalCorrectlyMatchedLength = 0;      // total length of perfectly matched road ways
		double totalMatchedLength = 0;    // total length of the road ways that are matched incorrectly
		double totalGroundTruthLength = 0;    // total length of the ground-truth road ways
		
		for (TrajectoryMatchResult r : matchedResult) {
			Set<String> matchRoadIDSet = new LinkedHashSet<>(r.getCompleteMatchRouteAtRank(0).getRoadIDList());
			// summarize all matched road length
			for (String s : matchRoadIDSet) {
				totalMatchedLength += id2RoadLength.get(s);
			}
			totalMatchedCount += matchRoadIDSet.size();
			// check the coverage of the roads found in our match
			HashSet<String> groundTruthIDList = gtResultList.get(Integer.parseInt(r.getTrajID()));
			if (groundTruthIDList == null)
				throw new NullPointerException("ERROR! Ground-truth of " + r.getTrajID() + " is not found.");
			for (String s : groundTruthIDList) {
				double currLength = id2RoadLength.get(s);
				totalGroundTruthLength += currLength;
				totalGroundTruthCount++;
				if (matchRoadIDSet.contains(s)) {
					totalCorrectlyMatchedLength += currLength;
					totalCorrectlyMatchedCount++;
//                } else {
//                    System.out.println("Incorrect matching result from " + r.getTrajID() + ": " + s);
//                    for (String m : matchRoadIDSet) {
//                        if (!groundTruthIDList.contains(m))
//                            System.out.println(m);
//                    }
				}
			}
		}
		
		double lengthPrecision = totalCorrectlyMatchedLength / totalMatchedLength;
		double lengthRecall = totalCorrectlyMatchedLength / totalGroundTruthLength;
		double lengthFScore = 2 * (lengthPrecision * lengthRecall / (lengthPrecision + lengthRecall));
		double countPrecision = totalCorrectlyMatchedCount / totalMatchedCount;
		double countRecall = totalCorrectlyMatchedCount / totalGroundTruthCount;
		double countFScore = 2 * (countPrecision * countRecall / (countPrecision + countRecall));
		String lengthPrecisionString = convertPercentage(lengthPrecision, df);
		String lengthRecallString = convertPercentage(lengthRecall, df);
		String lengthFMeasureString = convertPercentage(lengthFScore, df);
		String countPrecisionString = convertPercentage(countPrecision, df);
		String countRecallString = convertPercentage(countRecall, df);
		String countFMeasureString = convertPercentage(countFScore, df);
		mapMatchingLengthResult.add(lengthPrecisionString + "," + lengthRecallString + "," + lengthFMeasureString);
		mapMatchingCountResult.add(countPrecisionString + "," + countRecallString + "," + countFMeasureString);
		LOG.info("Map-matching result evaluated, length precision: " + lengthPrecisionString + "%, length recall:" + lengthRecallString +
				"%, F-score: " + lengthFMeasureString + "%.");
		LOG.info("Map-matching result evaluated, count precision: " + countPrecisionString + "%, count recall:" + countRecallString +
				"%, F-score: " + countFMeasureString + "%.");
	}
	
	/**
	 * The precision/recall/f-measure evaluation of the matching result in Global dataset
	 *
	 * @param matchedResult     Matching results generated by algorithm
	 * @param groundTruthResult The ground-truth matching result
	 */
	public void globalPrecisionRecallCalc(List<TrajectoryMatchResult> matchedResult, List<Pair<Integer, List<String>>> groundTruthResult,
										  String mapFolder) throws IOException {
		// insert all ground truth road match into globalCompareList
		DecimalFormat df = new DecimalFormat(".00000");
		Map<Integer, HashSet<String>> globalCompareList = new HashMap<>();
		for (Pair<Integer, List<String>> gtResult : groundTruthResult) {
			HashSet<String> gtRoadIDList = new LinkedHashSet<>(gtResult._2());
			globalCompareList.put(gtResult._1(), gtRoadIDList);
		}
		
		// start the comparison
		double totalCorrectlyMatchedLength = 0;      // total length of perfectly matched road ways
		double totalMatchedLength = 0;    // total length of the road ways that are matched incorrectly
		double totalGroundTruthLength = 0;    // total length of the ground-truth road ways
		GlobalMapLoader mapReader = new GlobalMapLoader(mapFolder);
		
		for (TrajectoryMatchResult r : matchedResult) {
			
			// read the corresponding map to extract actual length of each road
			RoadNetworkGraph currMap = mapReader.readRawMap(Integer.parseInt(r.getTrajID()));
			Map<String, Double> id2RoadLength = new HashMap<>();
			for (RoadWay w : currMap.getWays())
				id2RoadLength.put(w.getID(), w.getLength());
			
			Set<String> matchRoadIDSet = new LinkedHashSet<>(r.getCompleteMatchRouteAtRank(0).getRoadIDList());
			double currMatchedLength = 0;
			// summarize all matched road length
			for (String s : matchRoadIDSet) {
				currMatchedLength += id2RoadLength.get(s);
			}
			double correctlyMatchedLength = 0;
			// check the coverage of the roads found in our match
			HashSet<String> groundTruthIDList = globalCompareList.get(Integer.parseInt(r.getTrajID()));
			double currGroundTruthLength = 0;
			for (String s : groundTruthIDList) {
				double currLength = id2RoadLength.get(s);
				currGroundTruthLength += currLength;
				if (matchRoadIDSet.contains(s)) {
					correctlyMatchedLength += currLength;
				}
			}
			LOG.info("Trajectory " + r.getTrajID() + ": Precision=" + correctlyMatchedLength / currMatchedLength + ", " +
					"recall=" + correctlyMatchedLength / currGroundTruthLength);
			totalMatchedLength += currMatchedLength;
			totalCorrectlyMatchedLength += correctlyMatchedLength;
			totalGroundTruthLength += currGroundTruthLength;
		}
		
		double precision = totalCorrectlyMatchedLength / totalMatchedLength;
		double recall = totalCorrectlyMatchedLength / totalGroundTruthLength;
		double fScore = 2 * (precision * recall / (precision + recall));
		LOG.info("Map-matching result evaluated, precision: " + df.format(precision * 100) + "%, recall:" + df.format(recall * 100) +
				"%, F-score: " + df.format(fScore * 100) + "%.");
	}
	
	/**
	 * The precision/recall/f-measure of map update result in Beijing dataset.
	 *
	 * @param inferenceMap   Final output of the mapinference map.
	 * @param removedWayList List of originally removed edges.
	 * @param inputMap       The original input map.
	 */
	public void beijingMapUpdateEval(RoadNetworkGraph inferenceMap, List<RoadWay> removedWayList, RoadNetworkGraph inputMap) {
		// insert all ground truth road match into gtResultList
		DecimalFormat df = new DecimalFormat("0.000");
		HashSet<String> originalRoadSet = new HashSet<>();
		HashMap<String, Double> id2RemovedLength = new HashMap<>();
		HashSet<String> inferenceRoadSet = new HashSet<>();
		// start the count
		double totalFoundRoadLength = 0;      // total length of the removed roads found in the new map(inferred road length)
		double totalFoundRoadOriginalLength = 0;      // total length of the removed roads found in the new map(original road length)
		double totalNewRoadLength = 0;    // total length of the road ways that are matched incorrectly
		double totalGroundTruthLength = 0;    // total length of the ground-truth road ways
		double totalFoundRoadCount = 0;     // total number of removed road found
		double totalNewRoadCount = 0;     // total number of removed road found
		double totalGroundTruthCount = 0;    // total length of the ground-truth road ways
		
		// insert the raw map and removed road information into the dictionary
		for (RoadWay gtResult : inputMap.getWays())
			originalRoadSet.add(gtResult.getID());
		for (RoadWay removedEdge : removedWayList) {
			if (!id2RemovedLength.containsKey(removedEdge.getID())) {
				id2RemovedLength.put(removedEdge.getID(), removedEdge.getLength());
				totalGroundTruthLength += removedEdge.getLength();
				totalGroundTruthCount++;
			} else
				LOG.error("ERROR! The road " + removedEdge.getID() + " has been removed more than once.");
		}
		
		for (RoadWay w : inferenceMap.getWays()) {
			if (!inferenceRoadSet.contains(w.getID())) {
				inferenceRoadSet.add(w.getID());
			} else LOG.error("ERROR! Same road ID occurs more than once!");
			if (id2RemovedLength.containsKey(w.getID())) {
				totalFoundRoadLength += w.getLength();
				totalFoundRoadOriginalLength += id2RemovedLength.get(w.getID());
				totalFoundRoadCount++;
			} else if (!originalRoadSet.contains(w.getID())) {
				totalNewRoadLength += w.getLength();
				totalNewRoadCount++;
			}
		}
		
		double lengthPrecision = totalFoundRoadLength / (totalNewRoadLength + totalFoundRoadLength);
		double lengthRecall = totalFoundRoadOriginalLength / totalGroundTruthLength;
		double lengthFScore = 2 * (lengthPrecision * lengthRecall / (lengthPrecision + lengthRecall));
		double countPrecision = totalFoundRoadCount / (totalNewRoadCount + totalFoundRoadCount);
		double countRecall = totalFoundRoadCount / totalGroundTruthCount;
		double countFScore = 2 * (countPrecision * countRecall / (countPrecision + countRecall));
		double roadDiff = Math.abs(totalFoundRoadLength - totalFoundRoadOriginalLength) / totalFoundRoadOriginalLength;
		
		String lengthPrecisionString = convertPercentage(lengthPrecision, df);
		String lengthRecallString = convertPercentage(lengthRecall, df);
		String lengthFMeasureString = convertPercentage(lengthFScore, df);
		String countPrecisionString = convertPercentage(countPrecision, df);
		String countRecallString = convertPercentage(countRecall, df);
		String countFMeasureString = convertPercentage(countFScore, df);
		mapUpdateCountResult.add(countPrecisionString + "," + countRecallString + "," + countFMeasureString);
		mapUpdateLengthResult.add(lengthPrecisionString + "," + lengthRecallString + "," + lengthFMeasureString);
		LOG.info("Map update result evaluation complete, length precision: " + lengthPrecisionString + "%, length recall:" + lengthRecallString +
				"%, F-score: " + lengthFMeasureString + "%.");
		LOG.info("Map update result evaluation complete, count precision: " + countPrecisionString + "%, count recall:" + countRecallString +
				"%, F-score: " + countFMeasureString + "%.");
		LOG.info("Total number of roads found: " + totalFoundRoadCount + ", missing roads: " + (removedWayList.size() - totalFoundRoadCount) + ", wrong roads: " + totalNewRoadCount + ".");
		
		String print = "";
		if (totalFoundRoadLength > totalFoundRoadOriginalLength)
			print += "Overall, the inferred roads are longer than original road by ";
		else
			print += "Overall, the inferred roads are shorter than original road by ";
		print += convertPercentage(roadDiff, df) + "%";
		LOG.info(print);
	}
	
	private String convertPercentage(double decimal, DecimalFormat df) {
		return df.format(Double.isNaN(decimal) ? 0 : decimal * 100);
	}
}