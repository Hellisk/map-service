package algorithm.cooptimization;

import org.apache.log4j.Logger;
import util.function.GreatCircleDistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;
import util.object.spatialobject.Trajectory;
import util.object.structure.*;

import java.text.DecimalFormat;
import java.util.*;

public class CoOptimizationFunc {
	
	private static final Logger LOG = Logger.getLogger(CoOptimizationFunc.class);
	private Map<String, RoadWay> id2NewRoadWay = new LinkedHashMap<>();   // the mapping between the id and the new road way
	private Map<String, RoadWay> id2RoadWay = new LinkedHashMap<>();   // the mapping between the id and all road ways
	private Map<String, Double> wayID2InfluenceScore = new LinkedHashMap<>();   // the influence score of every road way
	private Map<String, List<Triplet<String, Integer, Double>>> newRoad2AffectedTrajIDAndAmount = new LinkedHashMap<>();   // for each new
	// road, list all the affected trajectory id, length and affect amount
	
	CoOptimizationFunc(RoadNetworkGraph roadGraph, List<RoadWay> newWayList) {
		for (RoadWay w : roadGraph.getWays())
			id2RoadWay.put(w.getID(), w);
		for (RoadWay w : newWayList) {
			w.setInfluenceScore(0);
			id2NewRoadWay.put(w.getID(), w);
		}
	}
	
	CoOptimizationFunc() {
	
	}
	
	/**
	 * Calculate the influence score for each newly added road ways.
	 *
	 * @param currMatchResultList The current matching result set
	 * @param id2PrevMatchResult  The previous matching result set
	 * @param roadMap             The current road map
	 */
	void influenceScoreGen(List<MultipleTrajectoryMatchResult> currMatchResultList,
						   Map<String, MultipleTrajectoryMatchResult> id2PrevMatchResult, RoadNetworkGraph roadMap) {
		// save all new roads into the mapping for road lookup
		for (RoadWay w : roadMap.getWays()) {
			if (w.isNewRoad()) {
				if (!id2NewRoadWay.containsKey(w.getID())) {
					id2NewRoadWay.put(w.getID(), w);
					w.setInfluenceScore(0);
				} else LOG.error("ERROR! The new road has been added to the new road way mapping: " + w.getID());
			}
			
			if (!id2RoadWay.containsKey(w.getID()))
				id2RoadWay.put(w.getID(), w);
			else LOG.error("ERROR! The road has been added to the all road way mapping: " + w.getID());
		}
		
		int changedMatchingCount = 0;
		for (MultipleTrajectoryMatchResult matchResult : currMatchResultList) {
			MultipleTrajectoryMatchResult prevMatchResult = id2PrevMatchResult.get(matchResult.getTrajID());
			if (probabilitySum(prevMatchResult) != probabilitySum(matchResult)) {     // the matching result changes due to new
				// road insertion, start the certainty calculation
				changedMatchingCount++;
				boolean isNewRoadWayInvolved = false;
				for (int i = 0; i < matchResult.getActualMatchCount(); i++) {
//                    if (prevMatchResult.getProbabilities(i) > matchResult.getProbabilities(i)) {
//                        System.out.println("WARNING! The previous matching probability is larger than the current one:" + matchResult.getTrajID());
//                        StringBuilder print = new StringBuilder();
//                        print.append("Previous result: ");
//                        List<String> prevMatchWayList = prevMatchResult.getCompleteMatchRouteAtRank(i);
//                        for (int j = 0; j < prevMatchWayList.size() - 1; j++) {
//                            print.append("\"").append(prevMatchWayList.get(j)).append("\",");
//                        }
//                        print.append("\"").append(prevMatchWayList.get(prevMatchWayList.size() - 1)).append("\"");
//                        System.out.println(print);
//
//                        print = new StringBuilder();
//                        print.append("Current result: ");
//                        List<String> currMatchWayList = matchResult.getCompleteMatchRouteAtRank(i);
//                        for (int j = 0; j < currMatchWayList.size() - 1; j++) {
//                            print.append("\"").append(currMatchWayList.get(j)).append("\",");
//                        }
//                        print.append("\"").append(currMatchWayList.get(currMatchWayList.size() - 1)).append("\"");
//                        System.out.println(print);
//                    }
					for (String id : matchResult.getCompleteMatchRouteAtRank(i).getRoadIDList()) {
						if (id2NewRoadWay.containsKey(id)) {
							isNewRoadWayInvolved = true;
							break;
						}
					}
					if (isNewRoadWayInvolved)
						break;
				}
				if (!isNewRoadWayInvolved) {
//                    LOG.warning("WARNING! The matching probability changes without matching to new roads: " + matchResult.getTrajID());
				} else {
					double certaintyDiff = Math.abs(certaintyCalc(matchResult) - certaintyCalc(prevMatchResult));
					if (certaintyDiff <= 0) {
						LOG.warn("The certainty difference should be larger than zero.");
						continue;
					}
					influenceScoreDistribution(prevMatchResult, matchResult, certaintyDiff);  // distribute the influence
					// score to the new road ways contributing the match change
					
					HashSet<String> newRoadSet = new HashSet<>();
					for (int i = 0; i < matchResult.getActualMatchCount(); i++) {
						for (String s : matchResult.getCompleteMatchRouteAtRank(i).getRoadIDList()) {
							if (id2NewRoadWay.containsKey(s) && !newRoadSet.contains(s)) {
								newRoadSet.add(s);
								if (newRoad2AffectedTrajIDAndAmount.containsKey(s))
									newRoad2AffectedTrajIDAndAmount.get(s).add(new Triplet<>(matchResult.getTrajID(),
											matchResult.getTrajSize(), probabilitySum(matchResult) - probabilitySum(prevMatchResult)));
								else {
									List<Triplet<String, Integer, Double>> affectedTrajList = new ArrayList<>();
									affectedTrajList.add(new Triplet<>(matchResult.getTrajID(), matchResult.getTrajSize(),
											probabilitySum(matchResult) - probabilitySum(prevMatchResult)));
									newRoad2AffectedTrajIDAndAmount.put(s, affectedTrajList);
								}
							}
						}
//                        if (i == 0 && newRoadSet.size() > 1) {
//                            StringBuilder print = new StringBuilder();
//                            print.append("Trajectory ").append(matchResult.getTrajID()).append(" is matched to ").append(newRoadSet.size()).append(" new ").append("roads: ");
//                            for (String s : newRoadSet) {
//                                print.append(s).append(" ");
//                            }
//                            LOG.info(print.toString());
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
		
		LOG.info("Influence score calculation is done. Total number of changed trajectory map-matching: " + changedMatchingCount);
	}
	
	/**
	 * Calculate the influence score for a specific road.
	 *
	 * @param currMatchResultList      The current matching result set.
	 * @param trajID2MatchResultUpdate The previous matching result set.
	 * @param newRoadID                The ID of the road to be calculated.
	 */
	void singleRoadInfluenceScoreGen(List<MatchResultWithUnmatchedTraj> currMatchResultList,
									 HashMap<String, List<Pair<String, MatchResultWithUnmatchedTraj>>> trajID2MatchResultUpdate,
									 String newRoadID) {
		
		for (MatchResultWithUnmatchedTraj matchResult : currMatchResultList) {
			MatchResultWithUnmatchedTraj prevMatchResult = trajID2MatchResultUpdate.get(matchResult.getTrajID()).get(0)._2();
			if (probabilitySum(prevMatchResult.getMatchResult()) != probabilitySum(matchResult.getMatchResult())) {     // the matching result
				// changes due to new road insertion, start the certainty calculation
				boolean isNewRoadWayInvolved = false;
				for (int i = 0; i < matchResult.getMatchResult().getActualMatchCount(); i++) {
//                    if (prevMatchResult.getProbabilities(i) > matchResult.getProbabilities(i)) {
//                        System.out.println("WARNING! The previous matching probability is larger than the current one:" + matchResult.getTrajID());
//                        StringBuilder print = new StringBuilder();
//                        print.append("Previous result: ");
//                        List<String> prevMatchWayList = prevMatchResult.getCompleteMatchRouteAtRank(i);
//                        for (int j = 0; j < prevMatchWayList.size() - 1; j++) {
//                            print.append("\"").append(prevMatchWayList.get(j)).append("\",");
//                        }
//                        print.append("\"").append(prevMatchWayList.get(prevMatchWayList.size() - 1)).append("\"");
//                        System.out.println(print);
//
//                        print = new StringBuilder();
//                        print.append("Current result: ");
//                        List<String> currMatchWayList = matchResult.getCompleteMatchRouteAtRank(i);
//                        for (int j = 0; j < currMatchWayList.size() - 1; j++) {
//                            print.append("\"").append(currMatchWayList.get(j)).append("\",");
//                        }
//                        print.append("\"").append(currMatchWayList.get(currMatchWayList.size() - 1)).append("\"");
//                        System.out.println(print);
//                    }
					for (String id : matchResult.getMatchResult().getCompleteMatchRouteAtRank(i).getRoadIDList()) {
						if (id.equals(newRoadID)) {
							isNewRoadWayInvolved = true;
							break;
						}
					}
					if (isNewRoadWayInvolved)
						break;
				}
				
				if (!isNewRoadWayInvolved) {
					LOG.debug("WARNING! The matching probability changes without matching to new roads.");
				} else {
					double certaintyDiff =
							Math.abs(certaintyCalc(matchResult.getMatchResult()) - certaintyCalc(prevMatchResult.getMatchResult()));
					if (certaintyDiff <= 0) {
						LOG.warn("The certainty difference should be larger than zero.");
						continue;
					}

                    // insert the current matching result to the updateGoh list
					trajID2MatchResultUpdate.get(matchResult.getTrajID()).add(new Pair<>(newRoadID, matchResult));
					
					if (!wayID2InfluenceScore.containsKey(newRoadID))
						wayID2InfluenceScore.put(newRoadID, certaintyDiff);
					else wayID2InfluenceScore.replace(newRoadID, wayID2InfluenceScore.get(newRoadID) + certaintyDiff);
					
					if (newRoad2AffectedTrajIDAndAmount.containsKey(newRoadID))
						newRoad2AffectedTrajIDAndAmount.get(newRoadID).add(new Triplet<>(matchResult.getTrajID(),
								matchResult.getMatchResult().getTrajSize(),
								probabilitySum(matchResult.getMatchResult()) - probabilitySum(prevMatchResult.getMatchResult())));
					else {
						List<Triplet<String, Integer, Double>> affectedTrajList = new ArrayList<>();
						affectedTrajList.add(new Triplet<>(matchResult.getTrajID(),
								matchResult.getMatchResult().getTrajSize(),
								probabilitySum(matchResult.getMatchResult()) - probabilitySum(prevMatchResult.getMatchResult())));
						newRoad2AffectedTrajIDAndAmount.put(newRoadID, affectedTrajList);
					}
				}
			}
		}
		
		if (wayID2InfluenceScore.containsKey(newRoadID))
			id2NewRoadWay.get(newRoadID).setInfluenceScore(wayID2InfluenceScore.get(newRoadID));
		else
			id2NewRoadWay.get(newRoadID).setInfluenceScore(0);
		
	}
	
	/**
	 * Distribute the total influence score to all new road ways involved
	 *
	 * @param prevMatchResult The previous matching result
	 * @param currMatchResult The current matching result
	 * @param certaintyDiff   The total influence to be assigned
	 */
	private void influenceScoreDistribution(MultipleTrajectoryMatchResult prevMatchResult, MultipleTrajectoryMatchResult currMatchResult, double certaintyDiff) {
		Set<String> prevMatchingWaySet = new HashSet<>();
		for (int i = 0; i < prevMatchResult.getActualMatchCount(); i++)
			prevMatchingWaySet.addAll(prevMatchResult.getCompleteMatchRouteAtRank(i).getRoadIDList());
		
		Map<String, Double> wayID2TotalLengthAssigned = new LinkedHashMap<>();  // for each new way involved, the total length of changed
		// road ways that assigned to it.
		for (int i = 0; i < currMatchResult.getActualMatchCount(); i++) {
			Set<RoadWay> keyChangingRoadWayList = new LinkedHashSet<>();
			// find all new road ways that affecting the current matching result
			for (String s : currMatchResult.getCompleteMatchRouteAtRank(i).getRoadIDList()) {
				if (id2NewRoadWay.containsKey(s)) {
					keyChangingRoadWayList.add(id2NewRoadWay.get(s));
				}
			}
			
			for (String s : currMatchResult.getCompleteMatchRouteAtRank(i).getRoadIDList()) {
				if (!prevMatchingWaySet.contains(s)) {    // the current road way never appears in the previous top-k matching results
					String closestNewWay = findClosestNewWay(id2RoadWay.get(s), keyChangingRoadWayList);    // find the closest new way
					// to assign
					if (!wayID2TotalLengthAssigned.containsKey(closestNewWay))
						wayID2TotalLengthAssigned.put(closestNewWay, id2RoadWay.get(s).getLength());
					else wayID2TotalLengthAssigned.replace(closestNewWay, wayID2TotalLengthAssigned.get(closestNewWay) + id2RoadWay.get(s)
							.getLength());
				}
			}
		}
		
		double totalLength = 0;
		for (Map.Entry<String, Double> entry : wayID2TotalLengthAssigned.entrySet()) {  // summarize the total length
			totalLength += entry.getValue();
		}
		
		for (Map.Entry<String, Double> entry : wayID2TotalLengthAssigned.entrySet()) {  // calculate the influence score derived the
			// current trajectory match and store it
			double influenceScore = totalLength == 0 ? 0 : certaintyDiff * (entry.getValue() / totalLength);
			if (!wayID2InfluenceScore.containsKey(entry.getKey()))
				wayID2InfluenceScore.put(entry.getKey(), influenceScore);
			else wayID2InfluenceScore.replace(entry.getKey(), wayID2InfluenceScore.get(entry.getKey()) + influenceScore);
		}
	}
	
	public Triplet<RoadNetworkGraph, List<Trajectory>, Double> percentageBasedCostCalc(Pair<List<MultipleTrajectoryMatchResult>,
			List<Triplet<Trajectory, String, String>>> matchResultTriplet, List<RoadWay> gtRemovedRoadWayList, RoadNetworkGraph currMap,
																					   double scoreThreshold, double lastCost) {
		DecimalFormat df = new DecimalFormat("0.000");
		Set<RoadWay> removedRoadWaySet = new HashSet<>();
		Set<String> removedRoadIDSet = new HashSet<>();
		HashSet<String> removedGTIDSet = new HashSet<>();
		for (RoadWay w : gtRemovedRoadWayList) {
			removedGTIDSet.add(w.getID());
		}
		// Normalize the influence score and confidence score
		List<Double> influenceScoreList = new ArrayList<>();
		List<Double> confidenceScoreList = new ArrayList<>();
		HashMap<Double, List<RoadWay>> influenceScore2RoadList = new HashMap<>();
		HashMap<Double, List<RoadWay>> confidenceScore2RoadList = new HashMap<>();
		HashMap<String, Integer> newRoad2InfluenceRank = new HashMap<>();
		HashMap<String, Integer> newRoad2ConfidenceRank = new HashMap<>();
		
		initScoreLists(influenceScoreList, confidenceScoreList, influenceScore2RoadList, confidenceScore2RoadList, currMap.getWays());
		
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
		
		// influence score display
		if (influenceScoreList.size() != confidenceScoreList.size())
			LOG.error("ERROR! The count of influence score and confident score is not the same.");
		
		// display the influence and confidence score list
		LOG.info("The current list of influence/confidence score:");
		LOG.info("Format(influence/confidence): score, road_id, affected_traj_count is_correct_road");
		for (int i = 0; i < influenceScoreList.size(); i++) {
			double influenceScore = influenceScoreList.get(i);
			double confidenceScore = confidenceScoreList.get(i);
			if (influenceScore2RoadList.containsKey(influenceScore) && confidenceScore2RoadList.containsKey(confidenceScore)) {
				RoadWay influenceRoad = influenceScore2RoadList.get(influenceScore).get(0);
				RoadWay confidenceRoad = confidenceScore2RoadList.get(confidenceScore).get(0);
				int influenceCount = newRoad2AffectedTrajIDAndAmount.containsKey(influenceRoad.getID()) ?
						newRoad2AffectedTrajIDAndAmount.get(influenceRoad.getID()).size() : 0;
				LOG.info(influenceScore + "," + influenceRoad.getID() + "," + influenceCount + "," + removedGTIDSet.contains(influenceRoad.getID())
						+ "\t\t" + confidenceScore + "," + confidenceRoad.getID() + "," + removedGTIDSet.contains(confidenceRoad.getID()));
				newRoad2InfluenceRank.put(influenceRoad.getID(), i);
				newRoad2ConfidenceRank.put(confidenceRoad.getID(), i);
				influenceScore2RoadList.get(influenceScore).remove(0);
				confidenceScore2RoadList.get(confidenceScore).remove(0);
				if (influenceScore2RoadList.get(influenceScore).size() == 0)
					influenceScore2RoadList.remove(influenceScore);
				if (confidenceScore2RoadList.get(confidenceScore).size() == 0)
					confidenceScore2RoadList.remove(confidenceScore);
			} else LOG.error("ERROR! The corresponding influence/confidence score is not found.");
		}
		
		int influencePosition = (int) (Math.floor(influenceScoreList.size() / (double) 100 * (scoreThreshold / 2)));
		int confidencePosition = (int) (Math.floor(confidenceScoreList.size() / (double) 100 * (scoreThreshold / 2)));
		double influenceThreshold = influenceScoreList.get(influencePosition == influenceScoreList.size() ? influenceScoreList.size() - 1 :
				influencePosition);
		double confidenceThreshold = confidenceScoreList.get(confidencePosition == confidenceScoreList.size() ?
				confidenceScoreList.size() - 1 : confidencePosition);
		for (RoadWay w : currMap.getWays()) {
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
				influencePosition + (int) (Math.floor((influenceScoreList.size() - influencePosition) / (double) 100 * (scoreThreshold / 2)));
		confidencePosition =
				confidencePosition + (int) (Math.floor((confidenceScoreList.size() - confidencePosition) / (double) 100 * (scoreThreshold / 2)));
		influenceThreshold = influenceScoreList.get(influencePosition == influenceScoreList.size() ? influenceScoreList.size() - 1 :
				influencePosition);
		confidenceThreshold = confidenceScoreList.get(confidencePosition == confidenceScoreList.size() ? confidenceScoreList.size() - 1
				: confidencePosition);
//        LOG.info(influenceThreshold + ", " + confidenceThreshold);
		int newRoadCount = 0;
		for (RoadWay w : currMap.getWays()) {
			if (w.isNewRoad()) {
				String print = "";
				print += "Road: " + w.getID();
				print += ", affectedTrajCount=" + (newRoad2AffectedTrajIDAndAmount.containsKey(w.getID()) ?
						newRoad2AffectedTrajIDAndAmount.get(w.getID()).size() : 0);
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
				LOG.info(print);
//                if (newRoad2AffectedTrajIDAndAmount.containsKey(w.getID())) {
//                    LOG.info("Affected Trajectories: ");
//                    for (Triplet<String, Integer, Double> affectedRoad : newRoad2AffectedTrajIDAndAmount.get(w.getID())) {
//                        LOG.info(affectedRoad._1() + "," + affectedRoad._2() + "," + affectedRoad._3());
//                    }
//                }
			}
		}
		
		int savedRoadCount = highCandidateSet.size() + highIHighCSet.size() + highILowCSet.size() + lowIHighCSet.size() + lowILowCSet.size();
		if (savedRoadCount != newRoadCount)
			LOG.error("ERROR! some roads are missing: " + savedRoadCount + "," + newRoadCount);
		
		// refine the map according to cost function
		currMap.removeRoadWayList(removedRoadWaySet);
		currMap.isolatedNodeRemoval();
		double totalBenefit =
				highCandidate + highIHighC != 0 && (highCandidate + highIHighC) / scoreThreshold - lastCost / (100 - scoreThreshold) > 0 ?
						(highILowC + lowIHighC + lowILowC) : -1;
		
		// List all trajectories that require rematch
		List<Trajectory> rematchTrajectoryList = new ArrayList<>();
		rematchCheck(matchResultTriplet, removedRoadIDSet, rematchTrajectoryList);
		
		LOG.info("Map refinement finished, total road removed: " + removedRoadWaySet.size() + ", trajectory affected: " +
				rematchTrajectoryList.size());
		LOG.info("High value candidate score:" + df.format(highCandidate) + ", count: " + highCandidateSet.size() + ", " +
				"accuracy: " + (highCandidateSet.size() != 0 ? df.format(highCandidateHit / (double) highCandidateSet.size() * 100) : 0) + "%.");
		LOG.info("High Influence High Confidence score:" + df.format(highIHighC) + ", count: " + highIHighCSet.size() + ", " +
				"accuracy: " + (highIHighCSet.size() != 0 ? df.format(highIHighCHit / (double) highIHighCSet.size() * 100) : 0) + "%.");
		LOG.info("High Influence Low Confidence score:" + df.format(highILowC) + ", count: " + highILowCSet.size() + ", " +
				"accuracy: " + (highILowCSet.size() != 0 ? df.format(highILowCHit / (double) highILowCSet.size() * 100) : 0) + "%.");
		LOG.info("Low Influence High Confidence score: " + df.format(lowIHighC) + ", count: " + lowIHighCSet.size() + ", " +
				"accuracy: " + (lowIHighCSet.size() != 0 ? df.format(lowIHighCHit / (double) lowIHighCSet.size() * 100) : 0) + "%.");
		LOG.info("Low Influence Low Confidence score: " + df.format(lowILowC) + ", count: " + lowILowCSet.size() + ", accuracy" +
				": " + (lowILowCSet.size() != 0 ? df.format(lowILowCHit / (double) lowILowCSet.size() * 100) : 0) + "%.");
		LOG.info("Remaining items score: " + df.format(highIHighC + highCandidate) + ", remove items score: " + df.format(lowILowC + lowIHighC + highILowC));
		
		return new Triplet<>(currMap, rematchTrajectoryList, totalBenefit);
	}
	
	Triplet<RoadNetworkGraph, List<Trajectory>, Double> combinedScoreCostCalc(Pair<List<MultipleTrajectoryMatchResult>,
			List<Triplet<Trajectory, String, String>>> matchResultTriplet, List<RoadWay> gtRemovedRoadWayList, RoadNetworkGraph currMap
			, double scoreThreshold, double lambda, double lastCost) {
		DecimalFormat df = new DecimalFormat("0.000");
		Set<RoadWay> removedRoadWaySet = new HashSet<>();
		Set<String> removedRoadIDSet = new HashSet<>();
		HashSet<String> removedGTIDSet = new HashSet<>();   // set of ground-truth removed road
		for (RoadWay w : gtRemovedRoadWayList) {
			removedGTIDSet.add(w.getID());
		}
		HashMap<String, String> extraPrintOut = new HashMap<>();
		
		HashMap<Double, List<RoadWay>> score2RoadList = new HashMap<>();
		List<InverselyComparableObject<Double, String>> combinedScoreList = new ArrayList<>();
		
		int newRoadCount = 0;
		double maxInfluenceScore = Double.NEGATIVE_INFINITY;
		double maxConfidenceScore = Double.NEGATIVE_INFINITY;
		
		for (RoadWay way : currMap.getWays()) {
			if (way.isNewRoad()) {
				maxInfluenceScore = maxInfluenceScore < way.getInfluenceScore() ? way.getInfluenceScore() : maxInfluenceScore;
				maxConfidenceScore = maxConfidenceScore < way.getConfidenceScore() ? way.getConfidenceScore() : maxConfidenceScore;
			}
		}
		if (maxInfluenceScore <= 0 || maxConfidenceScore <= 0) {
			LOG.info("The current iteration have no influence score, terminate the iteration: " + maxInfluenceScore + "," + maxConfidenceScore);
			return new Triplet<>(currMap, new ArrayList<>(), -1.0);
		}
		for (RoadWay way : currMap.getWays()) {
			if (way.isNewRoad()) {
				newRoadCount++;
//                currScore = lambda * way.getInfluenceScore() + (1 - lambda) * way.getConfidenceScore(); // linear combine
				double currScore = way.getInfluenceScore() * way.getConfidenceScore() / maxInfluenceScore / maxConfidenceScore; //
				// multiplication
				double deNormScore = way.getInfluenceScore() * way.getConfidenceScore();
				double linearScore = way.getInfluenceScore() + way.getConfidenceScore();
				if (score2RoadList.containsKey(currScore)) {
					score2RoadList.get(currScore).add(way);
				} else {
					List<RoadWay> wayList = new ArrayList<>();
					wayList.add(way);
					score2RoadList.put(currScore, wayList);
					combinedScoreList.add(new InverselyComparableObject<>(deNormScore, currScore, linearScore, way.getID()));
				}
			}
		}
		
		Collections.sort(combinedScoreList);
//        Collections.reverse(combinedScoreList);
		
		double highScoreSum = 0;
		double lowScoreSum = 0;
		HashSet<String> highScoreSet = new HashSet<>();
		HashSet<String> lowScoreSet = new HashSet<>();
		int scorePosition = (int) (Math.floor(combinedScoreList.size() / (double) 100 * scoreThreshold));
		for (int i = 0; i < scorePosition; i++) {
			List<RoadWay> currWayList = score2RoadList.get(combinedScoreList.get(i).getFirstProbability());
			for (RoadWay roadWay : currWayList) {
				highScoreSet.add(roadWay.getID());
				highScoreSum += combinedScoreList.get(i).getObject();
				extraPrintOut.put(roadWay.getID(), ", High");
			}
		}
		for (int i = scorePosition; i < combinedScoreList.size(); i++) {
			List<RoadWay> currWayList = score2RoadList.get(combinedScoreList.get(i).getFirstProbability());
			for (RoadWay roadWay : currWayList) {
				lowScoreSet.add(roadWay.getID());
				lowScoreSum += combinedScoreList.get(i).getObject();
				removedRoadWaySet.add(roadWay);
				removedRoadIDSet.add(roadWay.getID());
				extraPrintOut.put(roadWay.getID(), ", Low");
			}
		}
		displayScoreList(currMap.getWays(), removedGTIDSet, combinedScoreList, score2RoadList, extraPrintOut);
		
		int savedRoadCount = highScoreSet.size() + lowScoreSet.size();
		if (savedRoadCount != newRoadCount)
			LOG.error("ERROR! some roads are missing: " + savedRoadCount + "," + newRoadCount);
		
		// refine the map according to cost function
		currMap.removeRoadWayList(removedRoadWaySet);
		currMap.isolatedNodeRemoval();
//        double totalBenefit = highScoreSum > lastCost * 0.5 ? lowScoreSum : -1;
		double totalBenefit = highScoreSet.size() != 0 && highScoreSum > lowScoreSum ? highScoreSum - lowScoreSum : -1;
		
		// List all trajectories that require rematch
		List<Trajectory> rematchTrajectoryList = new ArrayList<>();
		rematchCheck(matchResultTriplet, removedRoadIDSet, rematchTrajectoryList);
		
		LOG.info("Map refinement finished, total road removed: " + removedRoadWaySet.size() + ", trajectory affected: " +
				rematchTrajectoryList.size());
		LOG.info("Remaining items score: " + df.format(highScoreSum) + ", remove items score: " + df.format(lowScoreSum));
		
		return new Triplet<>(currMap, rematchTrajectoryList, totalBenefit);
	}
	
	Triplet<RoadNetworkGraph, Set<String>, Double> indexedCombinedScoreCostCalc
			(HashMap<String, List<Pair<String, MatchResultWithUnmatchedTraj>>> trajID2MatchResultUpdate, List<RoadWay> gtRemovedRoadWayList,
			 RoadNetworkGraph currMap, double scoreThreshold, double lambda, double lastCost) {
		
		DecimalFormat df = new DecimalFormat("0.000");
		Set<RoadWay> removedRoadWaySet = new HashSet<>();
		Set<String> removedRoadIDSet = new HashSet<>();
		HashSet<String> removedGTIDSet = new HashSet<>();   // set of ground-truth removed road
		for (RoadWay w : gtRemovedRoadWayList) {
			removedGTIDSet.add(w.getID());
		}
		HashMap<String, String> extraPrintOut = new HashMap<>();
		
		HashMap<Double, List<RoadWay>> score2RoadList = new HashMap<>();
		List<InverselyComparableObject<Double, String>> combinedScoreList = new ArrayList<>();
		
		int newRoadCount = 0;
		double maxInfluenceScore = Double.NEGATIVE_INFINITY;
		double maxConfidenceScore = Double.NEGATIVE_INFINITY;
		
		for (Map.Entry<String, RoadWay> entry : id2NewRoadWay.entrySet()) {
			maxInfluenceScore = Math.max(maxInfluenceScore, entry.getValue().getInfluenceScore());
			maxConfidenceScore = Math.max(maxConfidenceScore, entry.getValue().getConfidenceScore());
		}
		for (Map.Entry<String, RoadWay> entry : id2NewRoadWay.entrySet()) {
			newRoadCount++;
			RoadWay way = entry.getValue();
//                currScore = lambda * way.getInfluenceScore() + (1 - lambda) * way.getConfidenceScore(); // linear combine
			double currScore = way.getInfluenceScore() * way.getConfidenceScore() / maxInfluenceScore / maxConfidenceScore; //
			// multiplication
			double denormScore = way.getInfluenceScore() * way.getConfidenceScore();
			double linearScore = way.getInfluenceScore() + way.getConfidenceScore();
			if (score2RoadList.containsKey(currScore)) {
				score2RoadList.get(currScore).add(way);
			} else {
				List<RoadWay> wayList = new ArrayList<>();
				wayList.add(way);
				score2RoadList.put(currScore, wayList);
				
				combinedScoreList.add(new InverselyComparableObject<>(denormScore, currScore, linearScore, way.getID()));
			}
		}
		
		Collections.sort(combinedScoreList);
//        Collections.reverse(combinedScoreList);
		
		double highScoreSum = 0;
		double lowScoreSum = 0;
		HashSet<String> highScoreSet = new HashSet<>();
		HashSet<String> lowScoreSet = new HashSet<>();
		int scorePosition = (int) (Math.floor(combinedScoreList.size() / (double) 100 * scoreThreshold));
		for (int i = 0; i < scorePosition; i++) {
			List<RoadWay> currWayList = score2RoadList.get(combinedScoreList.get(i).getFirstProbability());
			for (RoadWay roadWay : currWayList) {
				highScoreSet.add(roadWay.getID());
				highScoreSum += combinedScoreList.get(i).getObject();
				extraPrintOut.put(roadWay.getID(), ", High");
			}
		}
		for (int i = scorePosition; i < combinedScoreList.size(); i++) {
			List<RoadWay> currWayList = score2RoadList.get(combinedScoreList.get(i).getFirstProbability());
			for (RoadWay roadWay : currWayList) {
				lowScoreSet.add(roadWay.getID());
				lowScoreSum += combinedScoreList.get(i).getObject();
				removedRoadWaySet.add(roadWay);
				removedRoadIDSet.add(roadWay.getID());
				extraPrintOut.put(roadWay.getID(), ", Low");
			}
		}
		displayScoreList(currMap.getWays(), removedGTIDSet, combinedScoreList, score2RoadList, extraPrintOut);
		
		int savedRoadCount = highScoreSet.size() + lowScoreSet.size();
		if (savedRoadCount != newRoadCount)
			LOG.error("ERROR! some roads are missing: " + savedRoadCount + "," + newRoadCount);
		
		// refine the map according to cost function
		currMap.removeRoadWayList(removedRoadWaySet);
		currMap.isolatedNodeRemoval();
		for (RoadWay w : currMap.getWays())
			w.setNewRoad(false);
//        double totalBenefit = highScoreSum > lastCost * 0.5 ? lowScoreSum : -1;
		double totalBenefit = highScoreSet.size() != 0 && highScoreSum > lowScoreSum ? highScoreSum - lowScoreSum : -1;
		
		LOG.info("Remaining items score: " + df.format(highScoreSum) + ", remove items score: " + df.format(lowScoreSum));
		
		return new Triplet<>(currMap, removedRoadIDSet, totalBenefit);
	}
	
	private void rematchCheck(Pair<List<MultipleTrajectoryMatchResult>, List<Triplet<Trajectory, String, String>>> matchResultTriplet,
							  Set<String> removedRoadIDSet, List<Trajectory> rematchTrajectoryList) {
		for (MultipleTrajectoryMatchResult matchResult : matchResultTriplet._1()) {
			boolean isRematchRequired = false;
			for (int i = 0; i < matchResult.getActualMatchCount(); i++) {
				for (String s : matchResult.getCompleteMatchRouteAtRank(i).getRoadIDList()) {
					if (removedRoadIDSet.contains(s)) {
						isRematchRequired = true;
						rematchTrajectoryList.add(matchResult.getTrajectory());
						break;
					}
				}
				if (isRematchRequired)
					break;
			}
		}
	}
	
	private void displayScoreList(List<RoadWay> roadWays, HashSet<String> removedGTIDSet,
								  List<InverselyComparableObject<Double, String>> combinedScoreList,
								  HashMap<Double,
										  List<RoadWay>> score2RoadList, HashMap<String, String> extraPrintOut) {
		List<Double> influenceScoreList = new ArrayList<>();
		List<Double> confidenceScoreList = new ArrayList<>();
		HashMap<Double, List<RoadWay>> influenceScore2RoadList = new HashMap<>();
		HashMap<Double, List<RoadWay>> confidenceScore2RoadList = new HashMap<>();
		
		initScoreLists(influenceScoreList, confidenceScoreList, influenceScore2RoadList, confidenceScore2RoadList, roadWays);
		
		// influence score display
		if (influenceScoreList.size() != confidenceScoreList.size())
			LOG.error("ERROR! The count of influence score and confident score is not the same.");
		
		// display the influence and confidence score list
		LOG.info("The current list of influence/confidence score:");
		LOG.info("Format(influence/confidence): score, road_id, affected_traj_count is_correct_road");
		for (int i = 0; i < influenceScoreList.size(); i++) {
			double influenceScore = influenceScoreList.get(i);
			double confidenceScore = confidenceScoreList.get(i);
			if (influenceScore2RoadList.containsKey(influenceScore) && confidenceScore2RoadList.containsKey(confidenceScore)) {
				RoadWay influenceRoad = influenceScore2RoadList.get(influenceScore).get(0);
				RoadWay confidenceRoad = confidenceScore2RoadList.get(confidenceScore).get(0);
				int influenceCount = newRoad2AffectedTrajIDAndAmount.containsKey(influenceRoad.getID()) ?
						newRoad2AffectedTrajIDAndAmount.get(influenceRoad.getID()).size() : 0;
				LOG.info(influenceScore + "," + influenceRoad.getID() + "," + influenceCount + "," + removedGTIDSet.contains(influenceRoad.getID())
						+ "\t\t" + confidenceScore + "," + confidenceRoad.getID() + "," + removedGTIDSet.contains(confidenceRoad.getID()));
				influenceScore2RoadList.get(influenceScore).remove(0);
				confidenceScore2RoadList.get(confidenceScore).remove(0);
				if (influenceScore2RoadList.get(influenceScore).size() == 0)
					influenceScore2RoadList.remove(influenceScore);
				if (confidenceScore2RoadList.get(confidenceScore).size() == 0)
					confidenceScore2RoadList.remove(confidenceScore);
			} else LOG.error("ERROR! The corresponding influence/confidence score is not found.");
		}
		
		for (InverselyComparableObject<Double, String> score : combinedScoreList) {
			for (RoadWay w : score2RoadList.get(score.getFirstProbability())) {
				StringBuilder print = new StringBuilder();
				print.append("Road: ").append(w.getID());
				print.append(", affectedTrajCount=").append(newRoad2AffectedTrajIDAndAmount.containsKey(w.getID()) ?
						newRoad2AffectedTrajIDAndAmount.get(w.getID()).size() : 0);
				print.append(", combinedScore=").append(score.getFirstProbability()).append(", originalRoad=").append(removedGTIDSet.contains(w.getID()));
				if (extraPrintOut.containsKey(w.getID())) {
					print.append(extraPrintOut.get(w.getID()));
				} else
					LOG.error("ERROR! The road way ID is not found for its co-optimization result.");
				LOG.info(String.valueOf(print));
			}
		}
	}
	
	private void initScoreLists(List<Double> influenceScoreList, List<Double> confidenceScoreList,
								HashMap<Double, List<RoadWay>> influenceScore2RoadList,
								HashMap<Double, List<RoadWay>> confidenceScore2RoadList, List<RoadWay> wayList) {
		for (RoadWay w : wayList) {
			if (w.isNewRoad()) {
				if (Double.isNaN(w.getInfluenceScore()))
					w.setInfluenceScore(0);
				influenceScoreList.add(w.getInfluenceScore());
				if (influenceScore2RoadList.containsKey(w.getInfluenceScore())) {
					influenceScore2RoadList.get(w.getInfluenceScore()).add(w);
				} else {
					List<RoadWay> idList = new ArrayList<>();
					idList.add(w);
					influenceScore2RoadList.put(w.getInfluenceScore(), idList);
				}
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
		Collections.sort(influenceScoreList);
		Collections.reverse(influenceScoreList);
		Collections.sort(confidenceScoreList);
		Collections.reverse(confidenceScoreList);
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
	 * @param matchResult The top-k matching result of the given trajectory
	 * @return The certainty value
	 */
	private double certaintyCalc(MultipleTrajectoryMatchResult matchResult) {
		double probabilitySum = 0;
		double firstNormalizedProbability = Math.pow(matchResult.getProbabilityAtRank(0), 1.0 / matchResult.getTrajSize());
		for (int i = 1; i < matchResult.getActualMatchCount(); i++) {
			double normalizedProbability = Math.pow(matchResult.getProbabilityAtRank(i), 1.0 / matchResult.getTrajSize());
			probabilitySum += -normalizedProbability * Math.log(normalizedProbability);
		}
		return firstNormalizedProbability * (probabilitySum == 0 ? 1 : probabilitySum);
	}
	
	/**
	 * Sum up the probabilities of all top k matching results.
	 *
	 * @param matchResult The top-k matching result of the given trajectory
	 * @return The summary of probabilities
	 */
	private double probabilitySum(MultipleTrajectoryMatchResult matchResult) {
		double probSum = 0;
		for (int i = 0; i < matchResult.getActualMatchCount(); i++) {
			probSum += matchResult.getProbabilityAtRank(i);
		}
		return probSum;
	}
}