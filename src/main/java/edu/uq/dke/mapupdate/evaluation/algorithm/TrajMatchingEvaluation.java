package edu.uq.dke.mapupdate.evaluation.algorithm;

import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;

import java.util.*;

/**
 * Created by uqpchao on 10/07/2017.
 */
public class TrajMatchingEvaluation {
    public void trajectoryMatchingEvaluation(List<RoadWay> matchedResult, List<RoadWay> groundTruthResult) {
        // insert all ground truth road match into globalCompareList
        Map<String, List<String>> globalCompareList = new HashMap<>();
        for (RoadWay w : groundTruthResult) {
            String id = w.getId();
            List<String> matchIDList = new ArrayList<>();
            for (RoadNode n : w.getNodes()) {
                if (!matchIDList.contains(n.getId())) {
                    matchIDList.add(n.getId());
                }
            }
            globalCompareList.put(id, matchIDList);
        }
        int totalHitCount = 0;      // number of perfectly matched road ways
        int totalMissCount = 0;     // number of road ways that are not found in the match result
        int wrongMatchCount = 0;    // number of road ways that are matched incorrectly

        for (RoadWay w : matchedResult) {
            Set<String> uniqueIDList = new HashSet<>();
            // insert all unique road way ID into the list
            for (RoadNode n : w.getNodes()) {
                if (!uniqueIDList.contains(n.getId())) {
                    uniqueIDList.add(n.getId());
                }
            }

            // check the coverage of the roads found in our match
            int foundCount = 0;
            List<String> groundTruthIDList = globalCompareList.get(w.getId());
            for (String s : groundTruthIDList) {
                if (uniqueIDList.contains(s)) {
                    foundCount++;
                }
            }
            totalHitCount += foundCount;
            totalMissCount += groundTruthIDList.size() - foundCount;
            wrongMatchCount += uniqueIDList.size() - foundCount;
        }

        double precision = (double) totalHitCount / (double) (totalHitCount + wrongMatchCount);
        double recall = (double) totalHitCount / (double) (totalHitCount + totalMissCount);

        System.out.println("The precision is: " + precision * 100 + "%, the recall is:" + recall * 100 + "%");
    }
}
