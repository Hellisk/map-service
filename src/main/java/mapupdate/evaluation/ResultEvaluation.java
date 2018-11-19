package mapupdate.evaluation;

import mapupdate.util.io.CSVRawMapReader;
import mapupdate.util.object.datastructure.Pair;
import mapupdate.util.object.datastructure.TrajectoryMatchingResult;
import mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import mapupdate.util.object.roadnetwork.RoadWay;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

import static mapupdate.Main.LOGGER;
import static mapupdate.Main.ROOT_PATH;

/**
 * Created by uqpchao on 10/07/2017.
 */
public class ResultEvaluation {

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
    public void beijingMapMatchingEval(List<TrajectoryMatchingResult> matchedResult, List<Pair<Integer, List<String>>>
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
            id2RoadLength.put(w.getID(), w.getRoadLength());
        for (RoadWay w : removedEdges) {
            if (!id2RoadLength.containsKey(w.getID()))
                id2RoadLength.put(w.getID(), w.getRoadLength());
        }
        // start the count

        double totalCorrectlyMatchedCount = 0;      // total length of perfectly matched road ways
        double totalMatchedCount = 0;    // total length of the road ways that are matched incorrectly
        double totalGroundTruthCount = 0;    // total length of the ground-truth road ways
        double totalCorrectlyMatchedLength = 0;      // total length of perfectly matched road ways
        double totalMatchedLength = 0;    // total length of the road ways that are matched incorrectly
        double totalGroundTruthLength = 0;    // total length of the ground-truth road ways

        for (TrajectoryMatchingResult r : matchedResult) {
            Set<String> matchRoadIDSet = new LinkedHashSet<>(r.getBestMatchWayList());
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
                }
            }
        }

        double lengthPrecision = totalCorrectlyMatchedLength / totalMatchedLength;
        double lengthRecall = totalCorrectlyMatchedLength / totalGroundTruthLength;
        double lengthFScore = 2 * (lengthPrecision * lengthRecall / (lengthPrecision + lengthRecall));
        double countPrecision = totalCorrectlyMatchedCount / totalMatchedCount;
        double countRecall = totalCorrectlyMatchedCount / totalGroundTruthCount;
        double countFScore = 2 * (countPrecision * countRecall / (countPrecision + countRecall));
        String lengthPrecisionString = df.format(lengthPrecision * 100);
        String lengthRecallString = df.format(lengthRecall * 100);
        String lengthFMeasureString = df.format(lengthFScore * 100);
        String countPrecisionString = df.format(countPrecision * 100);
        String countRecallString = df.format(countRecall * 100);
        String countFMeasureString = df.format(countFScore * 100);
        mapMatchingLengthResult.add(lengthPrecisionString + "," + lengthRecallString + "," + lengthFMeasureString);
        mapMatchingCountResult.add(countPrecisionString + "," + countRecallString + "," + countFMeasureString);
        LOGGER.info("Map-matching result evaluated, length precision: " + lengthPrecisionString + "%, length recall:" + lengthRecallString +
                "%, F-score: " + lengthFMeasureString + "%.");
        LOGGER.info("Map-matching result evaluated, count precision: " + countPrecisionString + "%, count recall:" + countRecallString +
                "%, F-score: " + countFMeasureString + "%.");
    }

    /**
     * The precision/recall/f-measure evaluation of the matching result in Global dataset
     *
     * @param matchedResult     Matching results generated by algorithm
     * @param groundTruthResult The ground-truth matching result
     */
    public void globalPrecisionRecallCalc(List<TrajectoryMatchingResult> matchedResult, List<Pair<Integer, List<String>>> groundTruthResult) throws IOException {
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
        CSVRawMapReader mapReader = new CSVRawMapReader(ROOT_PATH + "input/");

        for (TrajectoryMatchingResult r : matchedResult) {

            // read the corresponding map to extract actual length of each road
            RoadNetworkGraph currMap = mapReader.readRawMap(Integer.parseInt(r.getTrajID()));
            Map<String, Double> id2RoadLength = new HashMap<>();
            for (RoadWay w : currMap.getWays())
                id2RoadLength.put(w.getID(), w.getRoadLength());

            Set<String> matchRoadIDSet = new LinkedHashSet<>(r.getBestMatchWayList());
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
            LOGGER.info("Trajectory " + r.getTrajID() + ": Precision=" + correctlyMatchedLength / currMatchedLength + ", " +
                    "recall=" + correctlyMatchedLength / currGroundTruthLength);
            totalMatchedLength += currMatchedLength;
            totalCorrectlyMatchedLength += correctlyMatchedLength;
            totalGroundTruthLength += currGroundTruthLength;
        }

        double precision = totalCorrectlyMatchedLength / totalMatchedLength;
        double recall = totalCorrectlyMatchedLength / totalGroundTruthLength;
        double fScore = 2 * (precision * recall / (precision + recall));
        LOGGER.info("Map-matching result evaluated, precision: " + df.format(precision * 100) + "%, recall:" + df.format(recall * 100) +
                "%, F-score: " + df.format(fScore * 100) + "%.");
    }

    /**
     * The precision/recall/f-measure of map update result in Beijing dataset.
     *
     * @param inferenceMap   Final output of the inference map.
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
                id2RemovedLength.put(removedEdge.getID(), removedEdge.getRoadLength());
                totalGroundTruthLength += removedEdge.getRoadLength();
                totalGroundTruthCount++;
            } else
                LOGGER.severe("ERROR! The road " + removedEdge.getID() + " has been removed more than once.");
        }

        for (RoadWay w : inferenceMap.getWays()) {
            if (!inferenceRoadSet.contains(w.getID())) {
                inferenceRoadSet.add(w.getID());
            } else LOGGER.severe("ERROR! Same road ID occurs more than once!");
            if (id2RemovedLength.containsKey(w.getID())) {
                totalFoundRoadLength += w.getRoadLength();
                totalFoundRoadOriginalLength += id2RemovedLength.get(w.getID());
                totalFoundRoadCount++;
                totalFoundRoadCount++;
            } else if (!originalRoadSet.contains(w.getID())) {
                totalNewRoadLength += w.getRoadLength();
                totalNewRoadCount++;
            }
        }

        double lengthPrecision = totalFoundRoadLength / (totalNewRoadLength + totalFoundRoadLength);
        double lengthRecall = totalFoundRoadOriginalLength / totalGroundTruthLength;
        double lengthFScore = 2 * (lengthPrecision * lengthRecall / (lengthPrecision + lengthRecall));
        double countPrecision = totalFoundRoadCount / (totalNewRoadCount + totalFoundRoadCount);
        double countRecall = totalFoundRoadCount / totalGroundTruthCount;
        double countFScre = 2 * (countPrecision * countRecall / (countPrecision + countRecall));
        double roadDiff = Math.abs(totalFoundRoadLength - totalFoundRoadOriginalLength) / totalFoundRoadOriginalLength;

        String lengthPrecisionString = df.format(lengthPrecision * 100);
        String lengthRecallString = df.format(lengthRecall * 100);
        String lengthFMeasureString = df.format(lengthFScore * 100);
        String countPrecisionString = df.format(countPrecision * 100);
        String countRecallString = df.format(countRecall * 100);
        String countFMeasureString = df.format(countFScre * 100);
        mapUpdateCountResult.add(countPrecisionString + "," + countRecallString + "," + countFMeasureString);
        mapUpdateLengthResult.add(lengthPrecisionString + "," + lengthRecallString + "," + lengthFMeasureString);
        LOGGER.info("Map update result evaluation complete, length precision: " + lengthPrecisionString + "%, length recall:" + lengthRecallString +
                "%, F-score: " + lengthFMeasureString + "%.");
        LOGGER.info("Map update result evaluation complete, count precision: " + countPrecisionString + "%, count recall:" + countRecallString +
                "%, F-score: " + countFMeasureString + "%.");
        LOGGER.info("Total number of roads found: " + totalFoundRoadCount + ", missing roads: " + (removedWayList.size() - totalFoundRoadCount) + ", wrong roads: " + totalNewRoadCount + ".");

        String print = "";
        if (totalFoundRoadLength > totalFoundRoadOriginalLength)
            print += "Overall, the inferred roads are longer than original road by ";
        else
            print += "Overall, the inferred roads are shorter than original road by ";
        print += df.format(roadDiff * 100) + "%";
        LOGGER.info(print);
    }
}