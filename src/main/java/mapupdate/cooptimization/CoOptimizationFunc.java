package mapupdate.cooptimization;

import mapupdate.util.function.GreatCircleDistanceFunction;
import mapupdate.util.object.datastructure.TrajectoryMatchingResult;
import mapupdate.util.object.datastructure.Triplet;
import mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import mapupdate.util.object.roadnetwork.RoadWay;
import mapupdate.util.object.spatialobject.Point;
import mapupdate.util.object.spatialobject.Trajectory;

import java.text.DecimalFormat;
import java.util.*;

import static mapupdate.Main.LOGGER;
import static mapupdate.Main.SCORE_THRESHOLD;

public class CoOptimizationFunc {

    private Map<String, RoadWay> id2NewRoadWay = new LinkedHashMap<>();   // the mapping between the id and the new road way
    private Map<String, RoadWay> id2RoadWay = new LinkedHashMap<>();   // the mapping between the id and all road ways
    private Map<String, Double> wayID2InfluenceScore = new LinkedHashMap<>();   // the influence score of every road way
    private Map<String, List<Triplet<String, Integer, Double>>> newRoad2AffectedTrajIDandAmount = new LinkedHashMap<>();   // for each new
    // road, list all the affected road id, length and affect amount

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
                if (!id2NewRoadWay.containsKey(w.getID())) {
                    id2NewRoadWay.put(w.getID(), w);
                    w.setInfluenceScore(0);
                } else LOGGER.severe("ERROR! The new road has been added to the new road way mapping: " + w.getID());
            }

            if (!id2RoadWay.containsKey(w.getID()))
                id2RoadWay.put(w.getID(), w);
            else LOGGER.severe("ERROR! The road has been added to the all road way mapping: " + w.getID());
        }

        for (TrajectoryMatchingResult matchingResult : currMatchingResultList) {
            TrajectoryMatchingResult prevMatchingResult = id2PrevMatchingResult.get(matchingResult.getTrajID());
            if (probabilitySum(prevMatchingResult) != probabilitySum(matchingResult)) {     // the matching result changes due to new
                // road insertion, start the certainty calculation

                boolean isNewRoadWayInvolved = false;
                for (int i = 0; i < matchingResult.getNumOfPositiveRank(); i++) {
                    if (prevMatchingResult.getProbability(i) > matchingResult.getProbability(i)) {
                        LOGGER.warning("WARNING! The previous matching probability is larger than the current one:" + matchingResult.getTrajID());
                        StringBuilder print = new StringBuilder();
                        print.append("Previous result: ");
                        List<String> prevMatchWayList = prevMatchingResult.getMatchWayList(i);
                        for (int j = 0; j < prevMatchWayList.size() - 1; j++) {
                            print.append("\"").append(prevMatchWayList.get(j)).append("\",");
                        }
                        print.append("\"").append(prevMatchWayList.get(prevMatchWayList.size() - 1)).append("\"");
                        LOGGER.info(print.toString());

                        print = new StringBuilder();
                        print.append("Current result: ");
                        List<String> currMatchWayList = matchingResult.getMatchWayList(i);
                        for (int j = 0; j < currMatchWayList.size() - 1; j++) {
                            print.append("\"").append(currMatchWayList.get(j)).append("\",");
                        }
                        print.append("\"").append(currMatchWayList.get(currMatchWayList.size() - 1)).append("\"");
                        LOGGER.info(print.toString());
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
                    LOGGER.severe("ERROR! The matching probability changes without matching to new roads.");
                else {
                    double certaintyDiff = Math.abs(certaintyCalc(matchingResult) - certaintyCalc(prevMatchingResult));
                    if (certaintyDiff <= 0) {
                        LOGGER.severe("ERROR! The certainty difference should be larger than zero.");
                        continue;
                    }
                    roadInfluenceAssignment(prevMatchingResult, matchingResult, certaintyDiff);  // distribute the influence
                    // score to the new road ways contributing the match change

                    HashSet<String> newRoadSet = new HashSet<>();
                    for (int i = 0; i < matchingResult.getNumOfPositiveRank(); i++) {
                        for (String s : matchingResult.getMatchWayList(i)) {
                            if (id2NewRoadWay.containsKey(s) && !newRoadSet.contains(s)) {
                                newRoadSet.add(s);
                                if (newRoad2AffectedTrajIDandAmount.containsKey(s))
                                    newRoad2AffectedTrajIDandAmount.get(s).add(new Triplet<>(matchingResult.getTrajID(),
                                            matchingResult.getTrajSize(), probabilitySum(matchingResult) - probabilitySum(prevMatchingResult)));
                                else {
                                    List<Triplet<String, Integer, Double>> affectedTrajList = new ArrayList<>();
                                    affectedTrajList.add(new Triplet<>(matchingResult.getTrajID(), matchingResult.getTrajSize(),
                                            probabilitySum(matchingResult) - probabilitySum(prevMatchingResult)));
                                    newRoad2AffectedTrajIDandAmount.put(s, affectedTrajList);
                                }
                            }
                        }
//                        if (i == 0 && newRoadSet.size() > 1) {
//                            StringBuilder print = new StringBuilder();
//                            print.append("Trajectory ").append(matchingResult.getTrajID()).append(" is matched to ").append(newRoadSet.size()).append(" new ").append("roads: ");
//                            for (String s : newRoadSet) {
//                                print.append(s).append(" ");
//                            }
//                            LOGGER.info(print.toString());
//                        }
                    }
                }
            }
        }

        for (RoadWay w : roadMap.getWays()) {
            if (w.isNewRoad()) {
                if (wayID2InfluenceScore.containsKey(w.getID()))
                    w.setInfluenceScore(wayID2InfluenceScore.get(w.getID()));
                else
                    w.setInfluenceScore(0);
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
            List<Trajectory>> matchingResultTriplet, List<RoadWay> gtRemovedRoadWayList, double lastCost) {
        DecimalFormat df = new DecimalFormat("0.000");
        Set<RoadWay> removedRoadWaySet = new HashSet<>();
        Set<String> removedRoadIDSet = new HashSet<>();
        List<Trajectory> rematchTrajectoryList = new ArrayList<>();
        HashSet<String> removedGTIDSet = new HashSet<>();
        for (RoadWay w : gtRemovedRoadWayList) {
            removedGTIDSet.add(w.getID());
        }
        // Normalize the influence score and confidence score
        double maxInfluenceScore = Double.NEGATIVE_INFINITY;
        double maxConfidenceScore = Double.NEGATIVE_INFINITY;
        List<Double> influenceScoreList = new ArrayList<>();
        List<Double> confidenceScoreList = new ArrayList<>();
        HashMap<Double, List<RoadWay>> influenceScore2RoadList = new HashMap<>();
        HashMap<Double, List<RoadWay>> confidenceScore2RoadList = new HashMap<>();
        HashMap<String, Integer> newRoad2InfluenceRank = new HashMap<>();
        HashMap<String, Integer> newRoad2ConfidenceRank = new HashMap<>();
        for (RoadWay w : matchingResultTriplet._2().getWays()) {
            if (w.isNewRoad()) {
                if (Double.isNaN(w.getInfluenceScore()))
                    w.setInfluenceScore(0);
                maxInfluenceScore = w.getInfluenceScore() > maxInfluenceScore ? w.getInfluenceScore() : maxInfluenceScore;
                influenceScoreList.add(w.getInfluenceScore());
                if (influenceScore2RoadList.containsKey(w.getInfluenceScore())) {
                    influenceScore2RoadList.get(w.getInfluenceScore()).add(w);
                } else {
                    List<RoadWay> idList = new ArrayList<>();
                    idList.add(w);
                    influenceScore2RoadList.put(w.getInfluenceScore(), idList);
                }
                maxConfidenceScore = w.getConfidenceScore() > maxConfidenceScore ? w.getConfidenceScore() : maxConfidenceScore;
                confidenceScoreList.add(w.getConfidenceScore());
                if (confidenceScore2RoadList.containsKey(w.getConfidenceScore())) {
                    confidenceScore2RoadList.get(w.getConfidenceScore()).add(w);
                } else {
                    List<RoadWay> idList = new ArrayList<>();
                    idList.add(w);
                    confidenceScore2RoadList.put(w.getConfidenceScore(), idList);
                }
            }
        }

        double highCandidate = 0;
        double highILowC = 0;
        double highIHighC = 0;
        double lowILowC = 0;
        double lowIHighC = 0;
        int highCandidateHit = 0;
        int highILowCHit = 0;
        int highIHighCHit = 0;
        int lowILowCHit = 0;
        int lowIHighCHit = 0;
        HashSet<String> highCandidateSet = new HashSet<>();
        HashSet<String> highILowCSet = new HashSet<>();
        HashSet<String> highIHighCSet = new HashSet<>();
        HashSet<String> lowILowCSet = new HashSet<>();
        HashSet<String> lowIHighCSet = new HashSet<>();
        Collections.sort(influenceScoreList);
        Collections.reverse(influenceScoreList);
        Collections.sort(confidenceScoreList);
        Collections.reverse(confidenceScoreList);

        // influence score display
        if (influenceScoreList.size() != confidenceScoreList.size())
            LOGGER.severe("ERROR! The count of influence score and confident score is not the same.");

        // display the influence and confidence score list
        LOGGER.info("The current list of influence/confidence score:");
        LOGGER.info("Format(influence/confidence): score, road_id, affected_traj_count is_correct_road");
        for (int i = 0; i < influenceScoreList.size(); i++) {
            double influenceScore = influenceScoreList.get(i);
            double confidenceScore = confidenceScoreList.get(i);
            if (influenceScore2RoadList.containsKey(influenceScore) && confidenceScore2RoadList.containsKey(confidenceScore)) {
                RoadWay influenceRoad = influenceScore2RoadList.get(influenceScore).get(0);
                RoadWay confidenceRoad = confidenceScore2RoadList.get(confidenceScore).get(0);
                int influenceCount = newRoad2AffectedTrajIDandAmount.containsKey(influenceRoad.getID()) ?
                        newRoad2AffectedTrajIDandAmount.get(influenceRoad.getID()).size() : 0;
                LOGGER.info(influenceScore + "," + influenceRoad.getID() + "," + influenceCount + "," + removedGTIDSet.contains(influenceRoad.getID())
                        + "\t\t" + confidenceScore + "," + confidenceRoad.getID() + "," + removedGTIDSet.contains(confidenceRoad.getID()));
                newRoad2InfluenceRank.put(influenceRoad.getID(), i);
                newRoad2ConfidenceRank.put(confidenceRoad.getID(), i);
                influenceScore2RoadList.get(influenceScore).remove(0);
                confidenceScore2RoadList.get(confidenceScore).remove(0);
                if (influenceScore2RoadList.get(influenceScore).size() == 0)
                    influenceScore2RoadList.remove(influenceScore);
                if (confidenceScore2RoadList.get(confidenceScore).size() == 0)
                    confidenceScore2RoadList.remove(confidenceScore);
            } else LOGGER.severe("ERROR! The corresponding influence/confidence score is not found.");
        }

        int influencePosition = (int) (Math.floor(influenceScoreList.size() / (double) 100 * (SCORE_THRESHOLD / 2)));
        int confidencePosition = (int) (Math.floor(confidenceScoreList.size() / (double) 100 * (SCORE_THRESHOLD / 2)));
        double influenceThreshold = influenceScoreList.get(influencePosition == influenceScoreList.size() ? influenceScoreList.size() - 1 :
                influencePosition);
        double confidenceThreshold = confidenceScoreList.get(confidencePosition == confidenceScoreList.size() ?
                confidenceScoreList.size() - 1 : confidencePosition);
        for (RoadWay w : matchingResultTriplet._2().getWays()) {
            if (w.isNewRoad()) {
                if (w.getInfluenceScore() > influenceThreshold || w.getConfidenceScore() > confidenceThreshold) {
                    highCandidate += w.getInfluenceScore() * w.getConfidenceScore();
                    highCandidateSet.add(w.getID());
                    if (removedGTIDSet.contains(w.getID()))
                        highCandidateHit++;
                }
            }
        }
        // reset the position to the next threshold
        influencePosition =
                influencePosition + (int) (Math.floor((influenceScoreList.size() - influencePosition) / (double) 100 * (SCORE_THRESHOLD / 2)));
        confidencePosition =
                confidencePosition + (int) (Math.floor((confidenceScoreList.size() - confidencePosition) / (double) 100 * (SCORE_THRESHOLD / 2)));
        influenceThreshold = influenceScoreList.get(influencePosition == influenceScoreList.size() ? influenceScoreList.size() - 1 :
                influencePosition);
        confidenceThreshold = confidenceScoreList.get(confidencePosition == confidenceScoreList.size() ? confidenceScoreList.size() - 1
                : confidencePosition);
//        LOGGER.info(influenceThreshold + ", " + confidenceThreshold);
        int newRoadCount = 0;
        for (RoadWay w : matchingResultTriplet._2().getWays()) {
            if (w.isNewRoad()) {
                String print = "";
                print += "Road: " + w.getID();
                print += ", affectedTrajCount=" + (newRoad2AffectedTrajIDandAmount.containsKey(w.getID()) ?
                        newRoad2AffectedTrajIDandAmount.get(w.getID()).size() : 0);
                print += ", infScore=" + w.getInfluenceScore() + ", infRank=" + newRoad2InfluenceRank.get(w.getID())
                        + ", conScore=" + w.getConfidenceScore() + ", conRank=" + newRoad2ConfidenceRank.get(w.getID()) + ", originalRoad=" + removedGTIDSet.contains(w.getID());
                newRoadCount++;
                if (highCandidateSet.contains(w.getID())) {                     // already in highCandidate, skip
                    print += ",TOP";
                } else if (w.getInfluenceScore() > influenceThreshold && w.getConfidenceScore() > confidenceThreshold) {
                    highIHighC += w.getInfluenceScore() * w.getConfidenceScore();
                    highIHighCSet.add(w.getID());
                    print += ",HIHC";
                    if (removedGTIDSet.contains(w.getID()))
                        highIHighCHit++;
                } else if (w.getInfluenceScore() > influenceThreshold && w.getConfidenceScore() <= confidenceThreshold) {
                    highILowC += w.getInfluenceScore() * w.getConfidenceScore();
                    highILowCSet.add(w.getID());
                    print += ",HILC";
                    removedRoadWaySet.add(w);
                    removedRoadIDSet.add(w.getID());
                    if (removedGTIDSet.contains(w.getID()))
                        highILowCHit++;
                } else if (w.getInfluenceScore() <= influenceThreshold && w.getConfidenceScore() > confidenceThreshold) {
                    lowIHighC += w.getInfluenceScore() * w.getConfidenceScore();
                    lowIHighCSet.add(w.getID());
                    print += ",LIHC";
                    removedRoadWaySet.add(w);
                    removedRoadIDSet.add(w.getID());
                    if (removedGTIDSet.contains(w.getID()))
                        lowIHighCHit++;
                } else if (w.getInfluenceScore() <= influenceThreshold && w.getConfidenceScore() <= confidenceThreshold) {
                    lowILowC += w.getInfluenceScore() * w.getConfidenceScore();
                    lowILowCSet.add(w.getID());
                    print += ",LILC";
                    removedRoadWaySet.add(w);
                    removedRoadIDSet.add(w.getID());
                    if (removedGTIDSet.contains(w.getID()))
                        lowILowCHit++;
                }
                LOGGER.info(print);
//                if (newRoad2AffectedTrajIDandAmount.containsKey(w.getID())) {
//                    LOGGER.info("Affected Trajectories: ");
//                    for (Triplet<String, Integer, Double> affectedRoad : newRoad2AffectedTrajIDandAmount.get(w.getID())) {
//                        LOGGER.info(affectedRoad._1() + "," + affectedRoad._2() + "," + affectedRoad._3());
//                    }
//                }
            }
        }
        int savedRoadCount = highCandidateSet.size() + highIHighCSet.size() + highILowCSet.size() + lowIHighCSet.size() + lowILowCSet.size();
        if (savedRoadCount != newRoadCount)
            LOGGER.severe("ERROR! some roads are missing: " + savedRoadCount + "," + newRoadCount);
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
        double totalBenefit = (highCandidate + highIHighC) / SCORE_THRESHOLD - lastCost / (100 - SCORE_THRESHOLD) > 0 ?
                (highILowC + lowIHighC + lowILowC) : -1;
        LOGGER.info("Map refinement finished, total road removed: " + removedRoadWaySet.size() + ", trajectory affected: " +
                rematchTrajectoryList.size() + ", max Influence score: " + df.format(maxInfluenceScore) + ", max confidence score: " + df.format(maxConfidenceScore));
        LOGGER.info("High value candidate score:" + df.format(highCandidate) + ", count: " + highCandidateSet.size() + ", " +
                "accuracy: " + (highCandidateSet.size() != 0 ? df.format(highCandidateHit / (double) highCandidateSet.size() * 100) : 0) + "%.");
        LOGGER.info("High Influence High Confidence score:" + df.format(highIHighC) + ", count: " + highIHighCSet.size() + ", " +
                "accuracy: " + (highIHighCSet.size() != 0 ? df.format(highIHighCHit / (double) highIHighCSet.size() * 100) : 0) + "%.");
        LOGGER.info("High Influence Low Confidence score:" + df.format(highILowC) + ", count: " + highILowCSet.size() + ", " +
                "accuracy: " + (highILowCSet.size() != 0 ? df.format(highILowCHit / (double) highILowCSet.size() * 100) : 0) + "%.");
        LOGGER.info("Low Influence High Confidence score: " + df.format(lowIHighC) + ", count: " + lowIHighCSet.size() + ", " +
                "accuracy: " + (lowIHighCSet.size() != 0 ? df.format(lowIHighCHit / (double) lowIHighCSet.size() * 100) : 0) + "%.");
        LOGGER.info("Low Influence Low Confidence score: " + df.format(lowILowC) + ", count: " + lowILowCSet.size() + ", accuracy" +
                ": " + (lowILowCSet.size() != 0 ? df.format(lowILowCHit / (double) lowILowCSet.size() * 100) : 0) + "%.");
        LOGGER.info("Remaining items score: " + df.format(highIHighC + highCandidate) + ", remove items score: " + df.format(lowILowC + lowIHighC + highILowC));

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
        double firstNormalizedProbability = Math.pow(matchingResult.getProbability(0), 1.0 / matchingResult.getTrajSize());
        for (int i = 1; i < matchingResult.getNumOfPositiveRank(); i++) {
            double normalizedProbability = Math.pow(matchingResult.getProbability(i), 1.0 / matchingResult.getTrajSize());
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