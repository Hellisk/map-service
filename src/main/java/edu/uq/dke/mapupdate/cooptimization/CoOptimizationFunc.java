package edu.uq.dke.mapupdate.cooptimization;

import edu.uq.dke.mapupdate.util.function.GreatCircleDistanceFunction;
import edu.uq.dke.mapupdate.util.object.datastructure.TrajectoryMatchingResult;
import edu.uq.dke.mapupdate.util.object.datastructure.Triplet;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;
import edu.uq.dke.mapupdate.util.object.spatialobject.Point;
import edu.uq.dke.mapupdate.util.object.spatialobject.Trajectory;

import java.util.*;

import static edu.uq.dke.mapupdate.Main.SCORE_THRESHOLD;

public class CoOptimizationFunc {

    private Map<String, RoadWay> id2NewRoadWay = new LinkedHashMap<>();   // the mapping between the id and the new road way
    private Map<String, RoadWay> id2RoadWay = new LinkedHashMap<>();   // the mapping between the id and all road ways
    private Map<String, Double> wayID2InfluenceScore = new LinkedHashMap<>();   // the influence score of every road way

    /**
     * Calculate the influence score for each newly added road ways.
     *
     * @param currMatchingResultList The current matching result set
     * @param id2PrevMatchingResult  The previous matching result set
     * @param roadMap                The current road map
     * @return The road map whose new edges have influence score assigned
     */
    public RoadNetworkGraph influenceScoreGen(List<TrajectoryMatchingResult> currMatchingResultList, Map<String, TrajectoryMatchingResult> id2PrevMatchingResult, RoadNetworkGraph roadMap) {
        // save all new roads into the mapping for road lookup
        for (RoadWay w : roadMap.getWays()) {
            if (w.isNewRoad()) {
                if (!id2NewRoadWay.containsKey(w.getID()))
                    id2NewRoadWay.put(w.getID(), w);
                else System.out.println("ERROR! The new road has been added to the new road way mapping: " + w.getID());
            }

            if (!id2RoadWay.containsKey(w.getID()))
                id2RoadWay.put(w.getID(), w);
            else System.out.println("ERROR! The road has been added to the all road way mapping: " + w.getID());
        }

        for (TrajectoryMatchingResult matchingResult : currMatchingResultList) {
            TrajectoryMatchingResult prevMatchingResult = id2PrevMatchingResult.get(matchingResult.getTrajID());
            if (probabilitySum(prevMatchingResult) != probabilitySum(matchingResult)) {     // the matching result changes due to new
                // road insertion, start the certainty calculation

                boolean isNewRoadWayInvolved = false;
                for (int i = 0; i < matchingResult.getNumOfPositiveRank(); i++) {
                    if (prevMatchingResult.getProbability(i) > matchingResult.getProbability(i)) {
                        System.out.println("ERROR! The previous matching probability is larger than the current one.");
                    }
                    for (String id : matchingResult.getMatchWayList(i)) {
                        if (id2NewRoadWay.containsKey(id)) {
                            isNewRoadWayInvolved = true;
                            break;
                        }
                    }
                    if (isNewRoadWayInvolved)
                        break;
                }
                if (!isNewRoadWayInvolved)
                    System.out.println("ERROR! The matching probability changes without matching to new roads.");
                else {
                    double certaintyDiff = certaintyCalc(matchingResult) - certaintyCalc(prevMatchingResult);
                    if (certaintyDiff <= 0) {
                        System.out.println("ERROR! The certainty difference should be larger than zero.");
                        continue;
                    }
                    roadInfluenceAssignment(prevMatchingResult, matchingResult, certaintyDiff);  // distribute the influence
                    // score to the new road ways contributing the match change
                }
            }
        }

        for (RoadWay w : roadMap.getWays()) {
            if (w.isNewRoad() && wayID2InfluenceScore.containsKey(w.getID())) {
                w.setInfluenceScore(wayID2InfluenceScore.get(w.getID()));
            } else if (!w.isNewRoad() && wayID2InfluenceScore.containsKey(w.getID())) {
                System.out.println("ERROR! The old road should not be assigned influence score.");
            }
        }
        return roadMap;
    }

    /**
     * Distribute the total influence score to all new road ways involved
     *
     * @param prevMatchingResult The previous matching result
     * @param currMatchingResult The current matching result
     * @param certaintyDiff      The total influence to be assigned
     */
    private void roadInfluenceAssignment(TrajectoryMatchingResult prevMatchingResult, TrajectoryMatchingResult currMatchingResult, double certaintyDiff) {
        Set<String> prevMatchingWaySet = new HashSet<>();
        for (int i = 0; i < prevMatchingResult.getNumOfPositiveRank(); i++)
            prevMatchingWaySet.addAll(prevMatchingResult.getMatchWayList(i));

        Map<String, Double> wayID2TotalLengthAssigned = new LinkedHashMap<>();  // for each new way involved, the total length of changed
        // road ways that assigned to it.
        for (int i = 0; i < currMatchingResult.getNumOfPositiveRank(); i++) {
            Set<RoadWay> keyChangingRoadWayList = new LinkedHashSet<>();
            // find all new road ways that affecting the current matching result
            for (String s : currMatchingResult.getMatchWayList(i)) {
                if (id2NewRoadWay.containsKey(s)) {
                    keyChangingRoadWayList.add(id2NewRoadWay.get(s));
                }
            }

            for (String s : currMatchingResult.getMatchWayList(i)) {
                if (!prevMatchingWaySet.contains(s)) {    // the current road way never appears in the previous top-k matching results
                    String closestNewWay = findClosestNewWay(id2RoadWay.get(s), keyChangingRoadWayList);    // find the closest new way
                    // to assign
                    if (!wayID2TotalLengthAssigned.containsKey(closestNewWay))
                        wayID2TotalLengthAssigned.put(closestNewWay, id2RoadWay.get(s).getRoadLength());
                    else wayID2TotalLengthAssigned.replace(closestNewWay, wayID2TotalLengthAssigned.get(closestNewWay) + id2RoadWay.get(s)
                            .getRoadLength());
                }
            }
        }

        double totalLength = 0d;
        for (Map.Entry<String, Double> entry : wayID2TotalLengthAssigned.entrySet()) {  // summarize the total length
            totalLength += entry.getValue();
        }

        for (Map.Entry<String, Double> entry : wayID2TotalLengthAssigned.entrySet()) {  // calculate the influence score derived the
            // current trajectory match and store it
            double influenceScore = certaintyDiff * (entry.getValue() / totalLength);
            if (!wayID2InfluenceScore.containsKey(entry.getKey()))
                wayID2InfluenceScore.put(entry.getKey(), influenceScore);
            else wayID2InfluenceScore.replace(entry.getKey(), wayID2InfluenceScore.get(entry.getKey()) + influenceScore);
        }
    }

    public Triplet<RoadNetworkGraph, List<Trajectory>, Double> costFunctionCal(Triplet<List<TrajectoryMatchingResult>, RoadNetworkGraph,
            List<Trajectory>> matchingResultTriplet) {
        Set<RoadWay> removedRoadWaySet = new HashSet<>();
        Set<String> removedRoadIDSet = new HashSet<>();
        List<Trajectory> rematchTrajectoryList = new ArrayList<>();
        // Normalize the influence score and confidence score
        double maxInfluenceScore = Double.NEGATIVE_INFINITY;
        double maxConfidenceScore = Double.NEGATIVE_INFINITY;
        List<Double> influenceScoreList = new ArrayList<>();
        List<Double> confidenceScoreList = new ArrayList<>();
        for (RoadWay w : matchingResultTriplet._2().getWays()) {
            if (w.isNewRoad()) {
                maxInfluenceScore = w.getInfluenceScore() > maxInfluenceScore ? w.getInfluenceScore() : maxInfluenceScore;
                influenceScoreList.add(w.getInfluenceScore());
                maxConfidenceScore = w.getConfidenceScore() > maxConfidenceScore ? w.getConfidenceScore() : maxConfidenceScore;
                confidenceScoreList.add(w.getConfidenceScore());
            }
        }
        Collections.sort(influenceScoreList);
        Collections.sort(confidenceScoreList);
        double medianInfluence = influenceScoreList.size() % 2 == 0 ? influenceScoreList.get(influenceScoreList.size() / 2 - 1)
                : influenceScoreList.get((influenceScoreList.size() - 1) / 2);
        double medianConfidence = confidenceScoreList.size() % 2 == 0 ? confidenceScoreList.get(confidenceScoreList.size() / 2 - 1)
                : confidenceScoreList.get((confidenceScoreList.size() - 1) / 2);
        double highILowC = 0;
        double highIHighC = 0;
        double lowILowC = 0;
        double lowIHighC = 0;
        List<String> highILowCList = new ArrayList<>();
        List<String> highIHighCList = new ArrayList<>();
        List<String> lowILowCList = new ArrayList<>();
        List<String> lowIHighCList = new ArrayList<>();
        for (RoadWay w : matchingResultTriplet._2().getWays()) {
            if (w.isNewRoad()) {
                System.out.println("Road: " + w.getID() + ", " + w.getInfluenceScore() + ", " + w.getConfidenceScore());
                if (w.getInfluenceScore() > medianInfluence && w.getConfidenceScore() > medianConfidence) {
                    highIHighC += w.getInfluenceScore() * w.getConfidenceScore();
                    highIHighCList.add(w.getID());
                } else if (w.getInfluenceScore() > medianInfluence && w.getConfidenceScore() < medianConfidence) {
                    highILowC += w.getInfluenceScore() * w.getConfidenceScore();
                    highILowCList.add(w.getID());
                } else if (w.getInfluenceScore() < medianInfluence && w.getConfidenceScore() < medianConfidence) {
                    lowILowC += w.getInfluenceScore() * w.getConfidenceScore();
                    lowILowCList.add(w.getID());
                    removedRoadWaySet.add(w);
                    removedRoadIDSet.add(w.getID());
                } else if (w.getInfluenceScore() < medianInfluence && w.getConfidenceScore() > medianConfidence) {
                    lowIHighC += w.getInfluenceScore() * w.getConfidenceScore();
                    lowIHighCList.add(w.getID());
                    removedRoadWaySet.add(w);
                    removedRoadIDSet.add(w.getID());
                }
            }
        }

        for (TrajectoryMatchingResult matchingResult : matchingResultTriplet._1()) {
            boolean isRematchRequired = false;
            for (int i = 0; i < matchingResult.getNumOfPositiveRank(); i++) {
                for (String s : matchingResult.getMatchWayList(i)) {
                    if (removedRoadIDSet.contains(s)) {
                        isRematchRequired = true;
                        rematchTrajectoryList.add(matchingResult.getRawTrajectory());
                        break;
                    }
                }
                if (isRematchRequired)
                    break;
            }
        }

        // refine the map according to cost function
        RoadNetworkGraph finalMap = matchingResultTriplet._2();
        finalMap.getWays().removeAll(removedRoadWaySet);
        finalMap.isolatedNodeRemoval();
        double totalBenefit = highIHighC + highILowC - lowILowC - lowIHighC;
        System.out.println("Map refinement finished, total road removed: " + removedRoadWaySet.size() + ", trajectory affected: " +
                rematchTrajectoryList.size());
        System.out.println("High Influence Low Confident edges: " + Arrays.toString(highILowCList.toArray()).concat(","));
        System.out.println("High Influence High Confident edges: " + Arrays.toString(highIHighCList.toArray()).concat(","));
        System.out.println("Low Influence Low Confident edges: " + Arrays.toString(lowILowCList.toArray()).concat(","));
        System.out.println("Low Influence High Confident edges: " + Arrays.toString(lowIHighCList.toArray()).concat(","));

        return new Triplet<>(finalMap, rematchTrajectoryList, totalBenefit);
    }

    private String findClosestNewWay(RoadWay currRoadWay, Set<RoadWay> candidateRoadWaySet) {
        GreatCircleDistanceFunction distanceFunction = new GreatCircleDistanceFunction();
        Point currRoadWayCenter = currRoadWay.getVirtualCenter();
        String closestWayID = "";
        double minDistance = Double.POSITIVE_INFINITY;
        for (RoadWay w : candidateRoadWaySet) {
            double distance = distanceFunction.distance(currRoadWayCenter, w.getVirtualCenter());
            if (distance < minDistance) {
                minDistance = distance;
                closestWayID = w.getID();
            }
        }
        return closestWayID;
    }

    /**
     * Calculate the certainty of the matching result list.
     *
     * @param matchingResult The top-k matching result of the given trajectory
     * @return The certainty value
     */
    private double certaintyCalc(TrajectoryMatchingResult matchingResult) {
        double probabilitySum = 0;
        double firstNormalizedProbability = Math.pow(matchingResult.getProbability(0), 1.0 / matchingResult.getTrajLength());
        for (int i = 1; i < matchingResult.getNumOfPositiveRank(); i++) {
            double normalizedProbability = Math.pow(matchingResult.getProbability(i), 1.0 / matchingResult.getTrajLength());
            probabilitySum += -normalizedProbability * Math.log(normalizedProbability);
        }
        return firstNormalizedProbability * probabilitySum;
    }

    /**
     * Sum up the probabilities of all top k matching results.
     *
     * @param matchingResult The top-k matching result of the given trajectory
     * @return The summary of probabilities
     */
    private double probabilitySum(TrajectoryMatchingResult matchingResult) {
        double probSum = 0;
        for (int i = 0; i < matchingResult.getNumOfPositiveRank(); i++) {
            probSum += matchingResult.getProbability(i);
        }
        return probSum;
    }

}