package edu.uq.dke.mapupdate.evaluation;

import edu.uq.dke.mapupdate.datatype.TrajectoryMatchResult;
import traminer.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by uqpchao on 10/07/2017.
 */
public class TrajMatchingEvaluation {
    public void beijingPrecisionRecallCalc(List<TrajectoryMatchResult> matchedResult, List<Pair<Integer, List<String>>> groundTruthResult) {
        // insert all ground truth road match into globalCompareList
        Map<Integer, HashSet<String>> globalCompareList = new HashMap<>();
        int gtResultCount = 0;
        for (Pair<Integer, List<String>> aGroundTruthResult : groundTruthResult) {
            HashSet<String> gtRoadIDList = new HashSet<>(aGroundTruthResult._2());
            globalCompareList.put(aGroundTruthResult._1(), gtRoadIDList);
            gtResultCount += gtRoadIDList.size();
        }

        // start the count
        int totalHitCount = 0;      // number of perfectly matched road ways
        int totalMatchingResultCount = 0;    // number of road ways that are matched incorrectly

        for (TrajectoryMatchResult r : matchedResult) {
            List<String> matchRoadIDList = r.getMatchWayList();
            // insert all unique road way ID into the list
            totalMatchingResultCount += matchRoadIDList.size();
            int hitCount = 0;
            // check the coverage of the roads found in our match
            HashSet<String> groundTruthIDList = globalCompareList.get(Integer.parseInt(r.getTrajID()));
            for (String s : matchRoadIDList) {
                if (groundTruthIDList.contains(s)) {
                    hitCount++;
                }
            }
            totalHitCount += hitCount;
        }

        double precision = (double) totalHitCount / (double) (totalMatchingResultCount);
        double recall = (double) totalHitCount / (double) (gtResultCount);

        System.out.println("Map-matching result evaluated, the precision is: " + precision * 100 + "%, the recall is:" + recall * 100 + "%");
    }

    public void globalPrecisionRecallCalc(List<TrajectoryMatchResult> matchedResult, List<Pair<Integer, List<String>>> groundTruthResult, List<Map<String, String>> groundTruthTrajectoryInfo) {
        // insert all ground truth road match into globalCompareList
        Map<Integer, HashSet<String>> globalCompareList = new HashMap<>();
        int gtResultCount = 0;
        for (Pair<Integer, List<String>> aGroundTruthResult : groundTruthResult) {
            HashSet<String> gtRoadIDList = new HashSet<>(aGroundTruthResult._2());
            globalCompareList.put(aGroundTruthResult._1(), gtRoadIDList);
            gtResultCount += gtRoadIDList.size();
        }

        // start the count
        int totalHitCount = 0;      // number of perfectly matched road ways
        int totalMatchingResultCount = 0;    // number of road ways that are matched incorrectly

        for (TrajectoryMatchResult r : matchedResult) {
            List<String> matchRoadIDList = r.getMatchWayList();
            // insert all unique road way ID into the list
            totalMatchingResultCount += matchRoadIDList.size();
            int hitCount = 0;
            // check the coverage of the roads found in our match
            HashSet<String> groundTruthIDList = globalCompareList.get(Integer.parseInt(r.getTrajID()));
            for (String s : matchRoadIDList) {
                if (groundTruthIDList.contains(s)) {
                    hitCount++;
                }
            }
            System.out.println("Trajectory " + r.getTrajID() + ": Precision=" + (double) hitCount / (double) matchRoadIDList.size() + ", recall=" + (double) hitCount / (double) globalCompareList.get(Integer.parseInt(r.getTrajID())).size());
            totalHitCount += hitCount;
        }

        double precision = (double) totalHitCount / (double) totalMatchingResultCount;
        double recall = (double) totalHitCount / (double) gtResultCount;

        System.out.println("Map-matching result evaluated, the precision is: " + precision * 100 + "%, the recall is:" + recall * 100 + "%");
    }
}