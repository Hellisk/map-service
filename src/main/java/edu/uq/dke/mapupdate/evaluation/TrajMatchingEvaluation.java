package edu.uq.dke.mapupdate.evaluation;

import edu.uq.dke.mapupdate.datatype.MatchingResult;
import traminer.util.Pair;
import traminer.util.map.matching.PointNodePair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by uqpchao on 10/07/2017.
 */
public class TrajMatchingEvaluation {
    public void precisionRecallCalc(List<MatchingResult> matchedResult, List<Pair<Integer, List<String>>> groundTruthResult) {
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

        for (MatchingResult r : matchedResult) {
            HashSet<String> matchRoadIDList = new HashSet<>();
            // insert all unique road way ID into the list
            for (PointNodePair p : r.getMatchingResult()) {
                if (p.getMatchingPoint() != null && !matchRoadIDList.contains(p.getMatchingPoint().getRoadID())) {
                    matchRoadIDList.add(p.getMatchingPoint().getRoadID());
                } else
                    matchRoadIDList.add("");

            }
            totalMatchingResultCount += matchRoadIDList.size();
            int hitCount = 0;
            // check the coverage of the roads found in our match
            HashSet<String> groundTruthIDList = globalCompareList.get(Integer.parseInt(r.getTrajID()));
            for (String s : groundTruthIDList) {
                if (matchRoadIDList.contains(s)) {
                    hitCount++;
                }
            }
            totalHitCount += hitCount;
        }

        double precision = (double) totalHitCount / (double) (totalMatchingResultCount);
        double recall = (double) totalHitCount / (double) (gtResultCount);

        System.out.println("Map-matching result evaluated, the precision is: " + precision * 100 + "%, the recall is:" + recall * 100 + "%");
    }
}