package algorithm.cooptimization;

import algorithm.mapinference.kde.KDEMapInference;
import algorithm.mapinference.lineclustering.LineClusteringMapInference;
import algorithm.mapmatching.hmm.HMMMapMatching;
import algorithm.mapmerge.MapMerge;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.index.rtree.STRTree;
import util.io.*;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;
import util.object.spatialobject.Trajectory;
import util.object.structure.*;
import util.settings.BaseProperty;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Hellisk
 * @since 30/03/2019
 */
class CoOptimization {
	
	private static final Logger LOG = Logger.getLogger(CoOptimization.class);
	
	static Pair<RoadNetworkGraph, List<TrajectoryMatchResult>> coOptimisationProcess(Stream<Trajectory> trajectoryStream, RoadNetworkGraph prevMap,
																					 List<RoadWay> removedWayList, BaseProperty prop) throws InterruptedException, ExecutionException, IOException {
		long startTaskTime = System.currentTimeMillis();
		long prevTime = System.currentTimeMillis();
		
		STRTree<Point> trajectoryPointIndex = null;   // index for location-based trajectory search
		// result
		String cacheFolder = prop.getPropertyString("algorithm.cooptimization.path.CacheFolder");
		DistanceFunction distFunc = prevMap.getDistanceFunction();
		int percentage = prop.getPropertyInteger("algorithm.cooptimization.data.RoadRemovalPercentage");
		// step 0: map-matching process, start the initial map-matching
		CoOptimizationFunc initialOptimizationFunc = new CoOptimizationFunc();  // not useful, only for filling the arguments
		List<Trajectory> inputTrajList = trajectoryStream.collect(Collectors.toList());
		
		Pair<List<TrajectoryMatchResult>, List<Triplet<Trajectory, String, String>>> initialMatchResultPair =
				parallelMapMatchingBeijing(inputTrajList.stream(), prevMap, 0, "normal", initialOptimizationFunc, prop);
		LOG.info("Initial map matching finished, time elapsed:" + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
		prevTime = System.currentTimeMillis();
		int indexFilterType = prop.getPropertyInteger("algorithm.cooptimization.IndexFilter");
		if (indexFilterType != 0) {
			String inputTrajFolder = prop.getPropertyString("path.InputTrajectoryFolder");
			trajectoryPointIndex = TrajectoryIndex.buildTrajectoryIndex(inputTrajFolder, distFunc);
			LOG.info("Trajectory index is built for subsequent queries. Total number of points in index: " + trajectoryPointIndex.count() +
					", time elapsed:" + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
		}
		
		int matchingTime = 0;
		int updateTime = 0;
		int refineMatchingTime = 0;
		int refinementTime = 0;
		
		Pair<List<TrajectoryMatchResult>, List<Triplet<Trajectory, String, String>>> prevMatchResultPair = initialMatchResultPair;
		long totalIterationStartTime = System.currentTimeMillis();
		int iteration = 1;  // start the iteration
		double costFunc = 0;
		int correctRoadPercentage = prop.getPropertyInteger("algorithm.cooptimization.CorrectRoadPercentage");
		double scoreLambda = prop.getPropertyDouble("algorithm.cooptimization.ScoreLambda");
		prop.setProperty("algorithm.cooptimization.isNewRoadIncluded", "true");    // set new road as
		while (iteration <= 8) {
//                while (costFunc >= 0) {
			LOG.info("Start the " + iteration + " round of iteration.");
			long currIterationStartTime = System.currentTimeMillis();
			
			HashMap<String, Pair<HashSet<String>, HashSet<String>>> newRoadID2AnchorPoints = new HashMap<>();
			List<RoadWay> inferenceResult;
			String inferenceMethod = prop.getPropertyString("algorithm.mapinference.InferenceMethod");
			if (inferenceMethod.equals("LC")) {
				// step 1: Trace clustering map inference
				List<Triplet<Trajectory, String, String>> unmatchedTrajList = prevMatchResultPair._2();
				LineClusteringMapInference mapInference = new LineClusteringMapInference();
				inferenceResult = mapInference.roadWayUpdateProcess(unmatchedTrajList, newRoadID2AnchorPoints, prop, distFunc);
				// write inference result
				int trajCount = 0;
				for (RoadWay w : inferenceResult) {
					w.setId("temp_" + trajCount);
					trajCount++;
				}
				MapWriter.writeWays(inferenceResult, cacheFolder + "inference/" + iteration + "/edges_" + percentage + ".txt");
			} else {
				// step 1-old: KDE map inference
				KDEMapInference mapInference = new KDEMapInference(prop);
				String localDir = System.getProperty("user.dir");
				mapInference.mapInferenceProcess(localDir + "/src/main/python/",
						cacheFolder + "unmatchedTrajectoryNextInput/" + iteration + "/",
						cacheFolder + "inference/" + iteration + "/");
				inferenceResult = MapReader.readWays(cacheFolder + "inference/" + iteration + "/", new HashMap<>(), distFunc);
				for (RoadWay roadWay : inferenceResult) {
					roadWay.setId("temp_" + roadWay.getID());
					roadWay.setNewRoad(true);
				}
			}
			LOG.info("Map inference finished, " + inferenceResult.size() + " new roads inferred, time elapsed: " +
					(System.currentTimeMillis() - currIterationStartTime) / 1000 + " " + "seconds");
			
			// step 2: map merge
			if (inferenceResult.size() == 0) {
				LOG.info("Current iteration does not have new road inferred. Finish the iteration.");
				break;
			}
			
			MapMerge spMapMerge = new MapMerge(prevMap, removedWayList, distFunc, prop);
			List<RoadWay> newWayList;
			newWayList = spMapMerge.nearestNeighbourMapMerge(inferenceResult, newRoadID2AnchorPoints);
			if (newWayList.size() == 0) {
				LOG.info("Current iteration does not have new road added. Finish the iteration.");
				break;
			}
			
			updateTime += (System.currentTimeMillis() - currIterationStartTime) / 1000;
			prevTime = System.currentTimeMillis();
			
			if (indexFilterType != 2) {  // no index or index on one-pass co-optimisation
				LOG.info("One-pass map merge finished, time elapsed: " + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
				CoOptimizationFunc coOptimizationFunc = new CoOptimizationFunc();
				prevMap.addWays(newWayList);
//                        MapWriter updatedMapWriter = new MapWriter(prevMap, CACHE_FOLDER);
//                        updatedMapWriter.writeMap(PERCENTAGE, iteration, true);
				
				// map update evaluation
				
				// step 3: map-matching process on updated map
				Pair<List<TrajectoryMatchResult>, List<Triplet<Trajectory, String, String>>> matchResultPair;
				if (indexFilterType == 1) {
					HashSet<String> trajIDSet = TrajectoryIndex.trajectoryIDSearch(newWayList, trajectoryPointIndex, prop);
					List<Trajectory> filteredTrajList = new ArrayList<>();
					for (Trajectory trajectory : inputTrajList) {
						if (trajIDSet.contains(trajectory.getID()))
							filteredTrajList.add(trajectory);
					}
					LOG.info("One-pass trajectory filtering finished, " + trajIDSet.size() + " trajectories are " +
							"selected, time elapsed: " + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
					matchResultPair = parallelMapMatchingBeijing(filteredTrajList.stream(), prevMap, iteration, "partial",
							coOptimizationFunc, prop);
					LOG.info("Map matching finished, time elapsed: " + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
				} else {
					matchResultPair = parallelMapMatchingBeijing(inputTrajList.stream(), prevMap, iteration, "normal",
							coOptimizationFunc, prop);
					LOG.info("Map matching finished, time elapsed: " + (System.currentTimeMillis() - prevTime) / 1000 + " seconds");
				}
				
				matchingTime += (System.currentTimeMillis() - prevTime) / 1000;
				prevTime = System.currentTimeMillis();
				
				// step 4: co-optimization model
//                        Triplet<RoadNetworkGraph, List<Trajectory>, Double> refinementResult = coOptimizationFunc.percentageBasedCostCalc
//                                (matchResultPair, removedWayList, SCORE_THRESHOLD, costFunc);
				Triplet<RoadNetworkGraph, List<Trajectory>, Double> refinementResult = coOptimizationFunc.combinedScoreCostCalc
						(matchResultPair, removedWayList, prevMap, correctRoadPercentage, scoreLambda, costFunc);
				Stream<Trajectory> refinedTrajectory = refinementResult._2().stream();
				Pair<List<TrajectoryMatchResult>, List<Triplet<Trajectory, String, String>>> refinedMatchResult = parallelMapMatchingBeijing
						(refinedTrajectory, refinementResult._1(), iteration, "refinement", coOptimizationFunc, prop);
				
				// step 5: write refinement result
				MapWriter.writeMap(refinementResult._1(), cacheFolder + "map/" + iteration + "/" + percentage + ".txt");
				refinementTime += (System.currentTimeMillis() - prevTime) / 1000;
				prevTime = System.currentTimeMillis();
				
				List<TrajectoryMatchResult> iterationFinalMatchResult = MatchResultWriter.writeAndMergeMatchResults(matchResultPair._1(),
						refinedMatchResult._1(), cacheFolder + "matchResult/" + iteration + "/");
				
				// TODO unmatched trajectory merge improvement
				Set<String> rematchTrajIDSet = new HashSet<>();
				List<Trajectory> unmatchedTraj = new ArrayList<>();
				List<Triplet<Trajectory, String, String>> iterationFinalUnmatchedResult = new ArrayList<>();
				for (Trajectory s : refinementResult._2()) {
					rematchTrajIDSet.add(s.getID());
				}
				for (Triplet<Trajectory, String, String> triplet : matchResultPair._2()) {
					String originalTrajID = triplet._1().getID().split("U")[0];
					if (!rematchTrajIDSet.contains(originalTrajID)) {
						unmatchedTraj.add(triplet._1());
						iterationFinalUnmatchedResult.add(triplet);
					}
				}
				for (Triplet<Trajectory, String, String> triplet : refinedMatchResult._2()) {
					unmatchedTraj.add(triplet._1());
					iterationFinalUnmatchedResult.add(triplet);
				}
				TrajectoryWriter.writeUnmatchedTrajectories(unmatchedTraj, cacheFolder + "unmatchedTrajectory/" + iteration + "/",
						cacheFolder + "unmatchedTrajectoryNextInput/" + iteration + "/");
				
				costFunc = refinementResult._3();
				refineMatchingTime += (System.currentTimeMillis() - prevTime) / 1000;
				
				if (matchResultPair._2().size() == 0)  // no unmatched trajectory, iteration terminates
					costFunc = -1;
				// evaluation: map update evaluation
				LOG.info("Evaluate the map update result and compare the map accuracy before and after refinement.");
				prevMatchResultPair = new Pair<>(iterationFinalMatchResult, iterationFinalUnmatchedResult);
			} else {    // index-based parallel map update
				CoOptimizationFunc coOptimizationFunc = new CoOptimizationFunc(prevMap, newWayList);
				// read the previous map-matching result
				HashMap<String, List<Pair<String, MatchResultWithUnmatchedTraj>>> trajID2MatchResultUpdate = new LinkedHashMap<>();
//                        TrajectoryReader csvMatchedTrajectoryReader = new TrajectoryReader(0);
//                        List<TrajectoryMatchResult> prevMatchResultList = csvMatchedTrajectoryReader.readMatchedResult(CACHE_FOLDER, iteration - 1);
				for (TrajectoryMatchResult mr : prevMatchResultPair._1()) {
					if (!trajID2MatchResultUpdate.containsKey(mr.getTrajID())) {
						List<Pair<String, MatchResultWithUnmatchedTraj>> matchResultList = new ArrayList<>();
						MatchResultWithUnmatchedTraj prevMatchResultWithUnmatchedTraj = new MatchResultWithUnmatchedTraj(mr, new ArrayList<>());
						matchResultList.add(new Pair<>("", prevMatchResultWithUnmatchedTraj));
						trajID2MatchResultUpdate.put(mr.getTrajID(), matchResultList);
					} else LOG.error("ERROR! The same trajectory matching result occurred twice: " + mr.getTrajID());
				}
				
				prevMap.addWays(newWayList);
				HMMMapMatching mapMatching = new HMMMapMatching(prevMap, prop);
				HashMap<String, List<RoadWay>> id2DDWayList = new LinkedHashMap<>();
				
				// combine the double-directed road to one entry.
				for (RoadWay w : newWayList) {
					String id = w.getID().replace("-", "");
					if (id2DDWayList.containsKey(id))
						id2DDWayList.get(id).add(w);
					else {
						List<RoadWay> wayList = new ArrayList<>();
						wayList.add(w);
						id2DDWayList.put(id, wayList);
					}
				}
				int totalTrajCount = 0;
				LinkedHashMap<String, Integer> trajectoryMatchCount = new LinkedHashMap<>();
				for (Map.Entry<String, List<RoadWay>> entry : id2DDWayList.entrySet()) {
					if (entry.getValue().size() > 2)
						LOG.warn("More than two roads have the same id");
					List<RoadWay> oneRoadList = new ArrayList<>();
					oneRoadList.add(entry.getValue().get(0));
					LinkedHashSet<String> trajIDSet = TrajectoryIndex.trajectoryIDSearch(oneRoadList, trajectoryPointIndex, prop);
					totalTrajCount += trajIDSet.size();
					for (String trajID : trajIDSet) {
						if (!trajectoryMatchCount.containsKey(trajID)) {
							trajectoryMatchCount.put(trajID, 1);
						} else
							trajectoryMatchCount.put(trajID, trajectoryMatchCount.get(trajID) + 1);
					}
					List<String> roadIDList = new ArrayList<>();
					for (RoadWay w : entry.getValue()) {
						roadIDList.add(w.getID());
					}
					List<Trajectory> filteredTrajList = new ArrayList<>();
					for (Trajectory trajectory : inputTrajList) {
						if (trajIDSet.contains(trajectory.getID()))
							filteredTrajList.add(trajectory);
					}
					singleDDRoadMapMatchingBeijing(filteredTrajList.stream(), mapMatching, roadIDList, trajID2MatchResultUpdate,
							coOptimizationFunc);
					
				}
				
				int maxMatchCount = 0;
				for (Map.Entry<String, Integer> entry : trajectoryMatchCount.entrySet()) {
					maxMatchCount = maxMatchCount > entry.getValue() ? maxMatchCount : entry.getValue();
				}
				
				LOG.info("Map matching finished, " + totalTrajCount + " trajectories involved, " + trajectoryMatchCount.size() +
						" unique trajectories, max duplicated matching count: " + maxMatchCount + ", time elapsed: " + (System.currentTimeMillis() - prevTime) / 1000 + "seconds");
				matchingTime += (System.currentTimeMillis() - prevTime) / 1000;
				prevTime = System.currentTimeMillis();
				
				Triplet<RoadNetworkGraph, Set<String>, Double> refinementResult = coOptimizationFunc.indexedCombinedScoreCostCalc
						(trajID2MatchResultUpdate, removedWayList, prevMap, correctRoadPercentage, scoreLambda, costFunc);
				Triplet<List<TrajectoryMatchResult>, List<Triplet<Trajectory, String, String>>, Integer> refinedMatchResult =
						refineMatchResult(trajID2MatchResultUpdate, refinementResult._2());
				
				LOG.info("Map refinement finished, total road removed: " + refinementResult._2().size() + ", trajectory affected: " +
						refinedMatchResult._3());
				
				// step 5: write refinement result
				MapWriter.writeMap(refinementResult._1(), cacheFolder + "map/" + iteration + "/" + percentage + ".txt");
				refinementTime += (System.currentTimeMillis() - prevTime) / 1000;
				prevTime = System.currentTimeMillis();
				
				MatchResultWriter.writeMatchResults(refinedMatchResult._1(), cacheFolder + "matchResult/" + iteration + "/");
				List<Trajectory> unmatchedTrajList = new ArrayList<>();
				for (Triplet<Trajectory, String, String> triplet : refinedMatchResult._2()) {
					unmatchedTrajList.add(triplet._1());
				}
				TrajectoryWriter.writeUnmatchedTrajectories(unmatchedTrajList, cacheFolder + "unmatchedTrajectory/" + iteration + "/",
						cacheFolder + "unmatchedTrajectoryNextInput/" + iteration + "/");
				costFunc = refinementResult._3();
				refineMatchingTime += (System.currentTimeMillis() - prevTime) / 1000;
				
				if (refinedMatchResult._2().size() == 0)  // no unmatched trajectory, iteration terminates
					costFunc = -1;
				prevMatchResultPair = new Pair<>(refinedMatchResult._1(), refinedMatchResult._2());
			}
			
			LOG.info("Result refinement finished, the cost function: " + costFunc + ", time elapsed: " +
					(System.currentTimeMillis() - prevTime) / 1000 + " seconds.");
			
			LOG.info("Finish the " + iteration + " round of iteration, total time elapsed: " + (System.currentTimeMillis()
					- currIterationStartTime) / 1000 + " seconds.");
			iteration++;
		}
		
		// finish the iterations and write the final output
		String outputMapFolder = prop.getPropertyString("path.OutputMapFolder");
		String outputMatchResultFolder = prop.getPropertyString("path.OutputMatchResultFolder");
		MapWriter.writeMap(prevMap, outputMapFolder + percentage + ".txt");
		MatchResultWriter.writeMatchResults(prevMatchResultPair._1(), outputMatchResultFolder);
		LOG.info("Co-optimization finish. Total running time: " + (System.currentTimeMillis() - startTaskTime) / 1000 +
				" seconds, matching time: " + matchingTime + ", update time: " + updateTime + ", refinement time: " +
				refinementTime + ", refinement matching time: " + refineMatchingTime + ", average time per " +
				"iteration: " + (System.currentTimeMillis() - totalIterationStartTime) / (iteration - 1) / 1000 + ", total number of " +
				"iterations: " + (iteration - 1));
		return new Pair<>(prevMap, prevMatchResultPair._1());
	}
	
	private static Triplet<List<TrajectoryMatchResult>, List<Triplet<Trajectory, String, String>>, Integer> refineMatchResult(HashMap<String,
			List<Pair<String, MatchResultWithUnmatchedTraj>>> trajID2MatchResultUpdate, Set<String> removedRoadIDSet) {
		int affectedTrajCount = 0;
		int multipleMatchTraj = 0;  // number of trajectories that affected by multiple new roads
		List<TrajectoryMatchResult> matchResultList = new ArrayList<>();
		List<Triplet<Trajectory, String, String>> unmatchedTrajList = new ArrayList<>();
		for (Map.Entry<String, List<Pair<String, MatchResultWithUnmatchedTraj>>> entry : trajID2MatchResultUpdate.entrySet()) {
			List<Pair<String, MatchResultWithUnmatchedTraj>> matchResult = entry.getValue();
			if (matchResult.size() == 1) { // map-matching not changed
				matchResultList.add(matchResult.get(0)._2().getMatchResult());
				if (matchResult.get(0)._2().getUnmatchedTrajectoryList().size() != 0)
					unmatchedTrajList.addAll(matchResult.get(0)._2().getUnmatchedTrajectoryList());
			} else if (matchResult.size() > 1) { //map-matching result changed
				MatchResultWithUnmatchedTraj finalResult = matchResult.get(0)._2();
				double maxProbability = finalResult.getMatchResult().getProbability();
				boolean isAffectedByRemoval = false;
				if (matchResult.size() > 2)
					multipleMatchTraj++;
				for (int i = 1; i < matchResult.size(); i++) {
					String roadID = matchResult.get(i)._1();
					MatchResultWithUnmatchedTraj currMatchResult = matchResult.get(i)._2();
					if (!removedRoadIDSet.contains(roadID)) {
						if (currMatchResult.getMatchResult().getProbability() > maxProbability) {
							finalResult = currMatchResult;
							maxProbability = currMatchResult.getMatchResult().getProbability();
						}
					} else isAffectedByRemoval = true;
				}
				if (isAffectedByRemoval)
					affectedTrajCount++;
				
				matchResultList.add(finalResult.getMatchResult());
				if (finalResult.getUnmatchedTrajectoryList().size() != 0)
					unmatchedTrajList.addAll(finalResult.getUnmatchedTrajectoryList());
			}
		}
		
		LOG.info("Total number of trajectories that can be matched to multiple new roads: " + multipleMatchTraj);
		return new Triplet<>(matchResultList, unmatchedTrajList, affectedTrajCount);
	}
	
	/**
	 * Map-matching and influence score generation on map that contains only one new road.
	 *
	 * @param mapMatching              Map-matching class which contains routing graph.
	 * @param roadIDList               List of new road IDs.
	 * @param trajID2MatchResultUpdate Mapping between trajectory and its previous + new matching result.
	 * @param coOptimizationFunc       Co-optimization function.
	 * @throws ExecutionException   Parallel error.
	 * @throws InterruptedException Parallel error.
	 */
	private static void singleDDRoadMapMatchingBeijing(Stream<Trajectory> inputTrajStream, HMMMapMatching mapMatching,
													   List<String> roadIDList, HashMap<String, List<Pair<String,
			MatchResultWithUnmatchedTraj>>> trajID2MatchResultUpdate, CoOptimizationFunc coOptimizationFunc) throws ExecutionException,
			InterruptedException {
		for (String roadID : roadIDList) {
			List<XYObject<SegmentWithIndex>> indexEntry = mapMatching.insertRoadWayIntoMap(roadID);
			// start matching process
//            List<MatchResultWithUnmatchedTraj> currCombinedMatchResultList =
//                    mapMatching.trajectoryListMatchingProcess(rawTrajectoryList.collect(Collectors.toList()));
			Stream<MatchResultWithUnmatchedTraj> currCombinedMatchResultStream = mapMatching.trajectoryStreamMatchingProcess(inputTrajStream);
			List<MatchResultWithUnmatchedTraj> currCombinedMatchResultList = currCombinedMatchResultStream.collect(Collectors.toList());

//            Set<String> currMatchingIDSet = new HashSet<>();
//            // check duplicated matching results
//            for (MatchResultWithUnmatchedTraj mr : currCombinedMatchResultList) {
//                if (!currMatchingIDSet.contains(mr.getTrajID())) {
//                    currMatchingIDSet.add(mr.getTrajID());
//                } else LOG.error("ERROR! The current trajectory is matched twice: " + mr.getTrajID());
//            }
			coOptimizationFunc.singleRoadInfluenceScoreGen(currCombinedMatchResultList, trajID2MatchResultUpdate, roadID);
			mapMatching.removeRoadWayFromMap(roadID, indexEntry);
		}
	}
	
	/**
	 * The main entry of map-matching algorithm for Beijing dataset
	 *
	 * @return map-matched trajectory result
	 */
	private static Pair<List<TrajectoryMatchResult>, List<Triplet<Trajectory, String, String>>> parallelMapMatchingBeijing
	(Stream<Trajectory> rawTrajectoryList, RoadNetworkGraph roadMap, int iteration, String matchType,
	 CoOptimizationFunc coOptimizationFunc, BaseProperty prop) throws ExecutionException, InterruptedException {
		
		// start matching process
		HMMMapMatching mapMatching = new HMMMapMatching(roadMap, prop);
		Stream<MatchResultWithUnmatchedTraj> currCombinedMatchResultStream = mapMatching.trajectoryStreamMatchingProcess(rawTrajectoryList);
		List<MatchResultWithUnmatchedTraj> currCombinedMatchResultList = currCombinedMatchResultStream.collect(Collectors.toList());
		List<TrajectoryMatchResult> currMatchResultList = new ArrayList<>();
		List<Triplet<Trajectory, String, String>> unmatchedTrajInfo = new ArrayList<>();
		int brokenTrajCount = 0;
		for (MatchResultWithUnmatchedTraj currPair : currCombinedMatchResultList) {
			currMatchResultList.add(currPair.getMatchResult());
			if (!currPair.getUnmatchedTrajectoryList().isEmpty()) {
				brokenTrajCount++;
				unmatchedTrajInfo.addAll(currPair.getUnmatchedTrajectoryList());
			}
		}
		LOG.info("Matching complete, total number of broken trajectories: " + brokenTrajCount);
		return matchedResultPostProcess(roadMap, iteration, matchType, currMatchResultList, unmatchedTrajInfo, coOptimizationFunc, prop);
	}
	
	private static Pair<List<TrajectoryMatchResult>, List<Triplet<Trajectory, String, String>>> matchedResultPostProcess
			(RoadNetworkGraph roadMap, int iteration, String matchType, List<TrajectoryMatchResult> currMatchResultList,
			 List<Triplet<Trajectory, String, String>> unmatchedTrajInfo, CoOptimizationFunc coOptimizationFunc, BaseProperty prop) {
		String cacheMatchResultFolder = prop.getPropertyString("algorithm.cooptimization.path.CacheFolder") + "matchResult/";
		String cacheUnmatchedTrajFolder = prop.getPropertyString("algorithm.cooptimization.path.CacheFolder") + "unmatchedTrajectory/";
		String cacheUnmatchedTrajNextInputFolder = prop.getPropertyString("algorithm.cooptimization.path.CacheFolder") +
				"unmatchedTrajectoryNextInput/";
		
		switch (matchType) {
			case "normal":  // traditional iterative map-matching
				if (iteration != 0) {     // start processing the co-optimization model
					List<TrajectoryMatchResult> prevMatchResult =
							MatchResultReader.readMatchResultsToList(cacheMatchResultFolder + (iteration - 1) + "/",
									roadMap.getDistanceFunction());
					Map<String, TrajectoryMatchResult> id2PrevMatchResult = new HashMap<>();
					for (TrajectoryMatchResult mr : prevMatchResult) {
						if (!id2PrevMatchResult.containsKey(mr.getTrajID()))
							id2PrevMatchResult.put(mr.getTrajID(), mr);
						else
							LOG.error("The same trajectory matching result occurred twice: " + mr.getTrajID());
					}
					coOptimizationFunc.influenceScoreGen(currMatchResultList, id2PrevMatchResult, roadMap);
					return new Pair<>(currMatchResultList, unmatchedTrajInfo);
				} else {
					List<Trajectory> unmatchedTrajList = new ArrayList<>();
					for (Triplet<Trajectory, String, String> trajectoryStringStringTriplet : unmatchedTrajInfo) {
						unmatchedTrajList.add(trajectoryStringStringTriplet._1());
					}
					// initial map-matching step, write output matching result
					MatchResultWriter.writeMatchResults(currMatchResultList, cacheMatchResultFolder + iteration + "/");
					TrajectoryWriter.writeUnmatchedTrajectories(unmatchedTrajList, cacheUnmatchedTrajFolder + iteration + "/",
							cacheUnmatchedTrajNextInputFolder + iteration + "/");
					return new Pair<>(currMatchResultList, unmatchedTrajInfo);
				}
			case "partial": // index-based iterative map-matching
				if (iteration != 0) {     // start processing the co-optimization model
					Set<String> currMatchingIDSet = new HashSet<>();
					List<TrajectoryMatchResult> unchangedResultList = new ArrayList<>();
					for (TrajectoryMatchResult mr : currMatchResultList) {
						if (!currMatchingIDSet.contains(mr.getTrajID()))
							currMatchingIDSet.add(mr.getTrajID());
						else
							LOG.error("The current trajectory is matched twice: " + mr.getTrajID());
					}
					List<TrajectoryMatchResult> prevMatchResult =
							MatchResultReader.readMatchResultsToList(cacheMatchResultFolder + (iteration - 1) + "/",
									roadMap.getDistanceFunction());
					Map<String, TrajectoryMatchResult> id2PrevMatchResult = new HashMap<>();
					for (TrajectoryMatchResult mr : prevMatchResult) {
						if (currMatchingIDSet.contains(mr.getTrajID())) {
							if (!id2PrevMatchResult.containsKey(mr.getTrajID()))
								id2PrevMatchResult.put(mr.getTrajID(), mr);
							else
								LOG.error("The same trajectory matching result occurred twice: " + mr.getTrajID());
						} else
							unchangedResultList.add(mr);
					}
					if (id2PrevMatchResult.size() != currMatchingIDSet.size())
						LOG.error("The new matching result cannot match to the old ones: " + currMatchingIDSet.size() + "," + id2PrevMatchResult.size());
					coOptimizationFunc.influenceScoreGen(currMatchResultList, id2PrevMatchResult, roadMap);
					currMatchResultList.addAll(unchangedResultList);
					return new Pair<>(currMatchResultList, unmatchedTrajInfo);
				} else {
					LOG.error("Partial map-matching should not happen in the initialization step.");
					return new Pair<>(currMatchResultList, unmatchedTrajInfo);
				}
			case "refinement":  // rematch after map refinement
				return new Pair<>(currMatchResultList, unmatchedTrajInfo);
			default:
				LOG.error("The match type is unknown: " + matchType);
				return new Pair<>(currMatchResultList, unmatchedTrajInfo);
		}
	}
}