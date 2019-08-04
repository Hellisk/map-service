package algorithm.mapmatching.hmm;

import org.apache.log4j.Logger;
import util.dijkstra.RoutingGraph;
import util.function.DistanceFunction;
import util.index.grid.Grid;
import util.index.grid.GridPartition;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;
import util.object.spatialobject.Segment;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.*;
import util.settings.BaseProperty;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Stream;

/**
 * Map-matching algorithm implemented according to the paper:
 * <p>
 * Newson, Paul, and John Krumm. "Hidden Markov map matching through noise and sparseness." Proceedings of the 17th ACM SIGSPATIAL
 * international conference on advances in geographic information systems. ACM, 2009.
 * <p>
 * The top-k matching result strategy is also implemented to be used in co-optimization process.
 *
 * @author Hellisk
 * @since 22/05/2017
 */
public class HMMMapMatching implements Serializable {
	
	private static final Logger LOG = Logger.getLogger(HMMMapMatching.class);
	
	private final int candidateRange;    // in meter
	private final int gapExtensionDist; // in meter
	private final int rankLength; // in meter
	
	/**
	 * The distance method to use between points
	 */
	private final DistanceFunction distFunc;
	
	/**
	 * The probabilities of the HMM lattice
	 */
	private final HMMProbabilities hmmProbabilities;
	private final BaseProperty prop;
	/**
	 * The graph for Dijkstra shortest distance calculation
	 */
	private final RoutingGraph routingGraph;
	/**
	 * The grid index for candidate generation
	 */
	private Grid<SegmentWithIndex> grid;
	/**
	 * the threshold for extra indexing point, segments that exceed such threshold will generate extra indexing point(s)
	 */
	private double intervalLength;
	private HashMap<String, List<RoadWay>> id2DDWayMapping = new HashMap<>(); // the mapping between road id and double-directed roads.
	
	public HMMMapMatching(RoadNetworkGraph roadNetworkGraph, BaseProperty prop) {
		this.distFunc = roadNetworkGraph.getDistanceFunction();
		this.prop = prop;
		// TODO add this flag to other cases
		boolean isNewRoadIncluded = prop.contains("algorithm.cooptimization.isNewRoadIncluded") && prop.getPropertyBoolean("algorithm.cooptimization" +
				".isNewRoadIncluded");
		this.candidateRange = prop.getPropertyInteger("algorithm.mapmatching.hmm.CandidateRange");
		this.gapExtensionDist = prop.contains("algorithm.cooptimization.GapExtensionDistance") ? prop.getPropertyInteger("algorithm.cooptimization" +
				".GapExtensionDistance") : 15;
		this.rankLength = prop.getPropertyInteger("algorithm.mapmatching.hmm.RankLength");
		double sigma = prop.getPropertyDouble("algorithm.mapmatching.hmm.Sigma");
		double beta = prop.getPropertyDouble("algorithm.mapmatching.hmm.Beta");
		this.hmmProbabilities = new HMMProbabilities(sigma, beta);
		this.intervalLength = (4 * Math.sqrt(2) - 2) * candidateRange;   // given such length limit, none of the candidate segment can escape
		// the grid search
		for (RoadWay w : roadNetworkGraph.getWays()) {
			String id = w.getID().replace("-", "");
			if (id2DDWayMapping.containsKey(id))
				id2DDWayMapping.get(id).add(w);
			else {
				List<RoadWay> wayList = new ArrayList<>();
				wayList.add(w);
				id2DDWayMapping.put(id, wayList);
			}
		}
		buildGridIndex(roadNetworkGraph, isNewRoadIncluded);   // build grid index
		this.routingGraph = new RoutingGraph(roadNetworkGraph, isNewRoadIncluded, prop);
	}
	
	/**
	 * Temporarily insert a new road into the HMM model, including adding entries in the index and edges to the routing graph. Note that
	 * this function is only called when indexFilterType = 2.
	 *
	 * @param roadID The ID of the road(s) to be inserted. Double directed.
	 * @return List of entries added to the index. Will be removed in the future.
	 */
	public List<XYObject<SegmentWithIndex>> insertRoadWayIntoMap(String roadID) {
		this.routingGraph.addRoadByID(roadID);
		String id = roadID.replace("-", "");
		if (!id2DDWayMapping.containsKey(id))
			throw new IllegalArgumentException("ERROR! The road to be inserted to the HMM model has wrong ID.");
		List<XYObject<SegmentWithIndex>> insertedItemList = new ArrayList<>();
		for (RoadWay w : id2DDWayMapping.get(id)) {
			if (w.getID().equals(roadID)) {
				for (Segment s : w.getEdges()) {
					// -1: left endpoint of the segment, 0: right endpoint of the segment, >0: intermediate point
					SegmentWithIndex segmentItemLeft = new SegmentWithIndex(s, -1, w.getID(), intervalLength, distFunc);
					XYObject<SegmentWithIndex> segmentIndexLeft = new XYObject<>(segmentItemLeft.x(), segmentItemLeft.y(), segmentItemLeft);
					SegmentWithIndex segmentItemRight = new SegmentWithIndex(s, 0, w.getID(), intervalLength, distFunc);
					XYObject<SegmentWithIndex> segmentIndexRight = new XYObject<>(segmentItemRight.x(), segmentItemRight.y(), segmentItemRight);
					this.grid.insert(segmentIndexLeft);
					this.grid.insert(segmentIndexRight);
					insertedItemList.add(segmentIndexLeft);
					insertedItemList.add(segmentIndexRight);
					// if the length of the segment is longer than two times of the candidate range, insert the intermediate points of the
					// segment
					double segmentDistance = distFunc.distance(s.p1(), s.p2());
					int intermediateID = 1;
					while (segmentDistance > intervalLength) {
						SegmentWithIndex segmentItemIntermediate = new SegmentWithIndex(s, intermediateID, w.getID(), intervalLength, distFunc);
						XYObject<SegmentWithIndex> segmentIndexIntermediate = new XYObject<>(segmentItemIntermediate.x(), segmentItemIntermediate.y(),
								segmentItemIntermediate);
						this.grid.insert(segmentIndexIntermediate);
						segmentDistance = segmentDistance - intervalLength;
						intermediateID++;
						insertedItemList.add(segmentIndexIntermediate);
					}
				}
			}
		}
		return insertedItemList;
	}
	
	/**
	 * Remove a temporary road from the HMM model, which is just inserted at the start of the current loop.
	 *
	 * @param roadID         ID of the road to be removed.
	 * @param indexEntryList List of entries to be removed.
	 */
	public void removeRoadWayFromMap(String roadID, List<XYObject<SegmentWithIndex>> indexEntryList) {
		this.routingGraph.removeRoadByID(roadID);
		String id = roadID.replace("-", "");
		if (!id2DDWayMapping.containsKey(id))
			throw new IllegalArgumentException("ERROR! The road to be inserted to the HMM model has wrong ID.");
		
		this.grid.removeAll(indexEntryList);
	}
	
	public List<MatchResultWithUnmatchedTraj> trajectoryListMatchingProcess(List<Trajectory> rawTrajectory) {
		
		// sequential test
		List<MatchResultWithUnmatchedTraj> result = new ArrayList<>();
//        int matchCount = 0;
		for (Trajectory traj : rawTrajectory) {
			MatchResultWithUnmatchedTraj matchResult = doMatching(traj);
			result.add(matchResult);
			System.out.println(traj.getID());
//            if (rawTrajectory.size() > 100)
//                if (matchCount % (rawTrajectory.size() / 100) == 0 && matchCount / (rawTrajectory.size() / 100) <= 100)
//                    LOG.info("Map matching finish " + matchCount / (rawTrajectory.size() / 100) + "%.");
//            matchCount++;
		}
		return result;
	}
	
	public Stream<MatchResultWithUnmatchedTraj> trajectoryStreamMatchingProcess(Stream<Trajectory> inputTrajectory)
			throws ExecutionException, InterruptedException {
		
		if (inputTrajectory == null) {
			throw new IllegalArgumentException("Trajectory stream for map-matching must not be null.");
		}
		if (this.grid == null || this.grid.isEmpty()) {
			throw new IllegalArgumentException("Grid index must not be empty nor null.");
		}
		
		// parallel processing
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		ForkJoinTask<Stream<MatchResultWithUnmatchedTraj>> taskResult =
				forkJoinPool.submit(() -> inputTrajectory.parallel().map(this::doMatching));
		while (!taskResult.isDone())
			Thread.sleep(5);
		return taskResult.get();
	}
	
	/**
	 * Matching entry for map-matching in Global dataset. Only provide matching for single thread.
	 *
	 * @param inputTrajectory Input trajectory in Global dataset
	 * @return Map-matching result
	 */
	public MultipleTrajectoryMatchResult trajectorySingleMatchingProcess(Trajectory inputTrajectory) {
		
		// sequential test
		MatchResultWithUnmatchedTraj matchResult = doMatching(inputTrajectory);
//            if (inputTrajectory.size() > 100)
//                if (matchCount % (inputTrajectory.size() / 100) == 0)
//                    LOG.info("Map matching finish " + matchCount / (inputTrajectory.size() / 100) + "%. Broken trajectory count:" + hmmMapMatching.getBrokenTrajCount() + ".");
//            matchCount++;
		LOG.info("Matching finished:" + inputTrajectory.getID());
		return matchResult.getMatchResult();
	}
	
	/**
	 * Create grid index for fast candidate computing.
	 *
	 * @param inputMap           The input road network.
	 * @param isNewRoadsIncluded Is new roads not inserted into the index. isNewRoadsIncluded = true only when partial map merge is called.
	 */
	private void buildGridIndex(RoadNetworkGraph inputMap, boolean isNewRoadsIncluded) {
		
		// calculate the grid settings
		int rowNum;     // number of rows
		int columnNum;     // number of columns
		
		if (inputMap.getNodes().isEmpty())
			throw new IllegalStateException("Cannot create location index of empty graph!");
		
		// calculate the total number of rows and columns. The size of each grid cell equals the candidate range
		double lonDistance = distFunc.pointToPointDistance(inputMap.getMaxLon(), 0d, inputMap.getMinLon(), 0d);
		double latDistance = distFunc.pointToPointDistance(0d, inputMap.getMaxLat(), 0d, inputMap.getMinLat());
		double gridRadius = candidateRange >= 15 ? 2 * candidateRange : 2 * 15;
		columnNum = (int) Math.floor(lonDistance / gridRadius);
		rowNum = (int) Math.floor(latDistance / gridRadius);
		double lonPerCell = (inputMap.getMaxLon() - inputMap.getMinLon()) / columnNum;
		double latPerCell = (inputMap.getMaxLat() - inputMap.getMinLat()) / columnNum;
		
		// add extra grid cells around the margin to cover outside trajectory points
		this.grid = new Grid<>(columnNum + 2, rowNum + 2, inputMap.getMinLon() - lonPerCell, inputMap.getMinLat() - latPerCell, inputMap
				.getMaxLon() + lonPerCell, inputMap.getMaxLat() + latPerCell, distFunc);

//        LOG.info("The grid contains " + rowNum + 2 + " rows and " + columnNum + 2 + " columns");
		
		int pointCount = 0;
		int intermediatePointCount = 0;
		
		for (RoadWay t : inputMap.getWays()) {
			if (!isNewRoadsIncluded || !t.isNewRoad()) {
				for (Segment s : t.getEdges()) {
					// -1: left endpoint of the segment, 0: right endpoint of the segment, >0: intermediate point
					SegmentWithIndex segmentItemLeft = new SegmentWithIndex(s, -1, t.getID(), intervalLength, distFunc);
					XYObject<SegmentWithIndex> segmentIndexLeft = new XYObject<>(segmentItemLeft.x(), segmentItemLeft.y(), segmentItemLeft);
					SegmentWithIndex segmentItemRight = new SegmentWithIndex(s, 0, t.getID(), intervalLength, distFunc);
					XYObject<SegmentWithIndex> segmentIndexRight = new XYObject<>(segmentItemRight.x(), segmentItemRight.y(), segmentItemRight);
					this.grid.insert(segmentIndexLeft);
					pointCount++;
					this.grid.insert(segmentIndexRight);
					pointCount++;
					// if the length of the segment is longer than two times of the candidate range, insert the intermediate points of the
					// segment
					double segmentDistance = distFunc.distance(s.p1(), s.p2());
					int intermediateID = 1;
					while (segmentDistance > intervalLength) {
						SegmentWithIndex segmentItemIntermediate = new SegmentWithIndex(s, intermediateID, t.getID(), intervalLength, distFunc);
						XYObject<SegmentWithIndex> segmentIndexIntermediate = new XYObject<>(segmentItemIntermediate.x(), segmentItemIntermediate.y(),
								segmentItemIntermediate);
						this.grid.insert(segmentIndexIntermediate);
						segmentDistance = segmentDistance - intervalLength;
						intermediateID++;
						intermediatePointCount++;
						pointCount++;
					}
				}
			}
		}
		
		LOG.info("Grid index build successfully, total number of segment items in grid index: " + pointCount + ", number of " +
				"newly created middle points: " + intermediatePointCount);
	}
	
	/**
	 * Map-matching process.
	 *
	 * @param trajectory Input trajectory.
	 * @return Pair(Map - matching result, List ( unmatched trajectory, preceding match way, succeeding match way)).
	 */
	// TODO Null result occurred, find the reason.
	public MatchResultWithUnmatchedTraj doMatching(final Trajectory trajectory) {
		// Compute the candidate road segment list for every GPS point through grid index
//        long startTime = System.currentTimeMillis();
		int indexBeforeCurrBreak = -1;   // the index of the last point before current broken position, -1 = currently no breakpoint
		final Map<TrajectoryPoint, Collection<PointMatch>> candidatesMap = new HashMap<>(); //Map each point to a list of candidate nodes
		computeCandidatesFromIndex(trajectory, candidatesMap, grid);
//        computeCandidates(trajectory);
//        LOG.info("Time cost on candidate generation is: " + (System.currentTimeMillis() - startTime));
		boolean isBrokenTraj = false;
//        Set<Integer> currBreakIndex = new LinkedHashSet<>();    // points that will be put into the unmatched trajectory
		Map<Integer, Integer> breakPoints = new LinkedHashMap<>();  // the points that break the connectivity and the reason, =1 if
		// the break is caused by no candidate, =2 if it is caused by broken transition, =3 if it is caused by broken transition but used
		// as the initial point of the new sequence
		
		List<Triplet<Trajectory, String, String>> unmatchedTrajectoryList = new ArrayList<>();   // unmatched trajectories
		
		ViterbiAlgorithm<PointMatch, TrajectoryPoint, RoadPath> viterbi = new ViterbiAlgorithm<>(rankLength);
		TimeStep<PointMatch, TrajectoryPoint, RoadPath> prevTimeStep = null;
		List<Pair<List<SequenceState<PointMatch, TrajectoryPoint, RoadPath>>, Double>> rankedRoadPositionList = new ArrayList<>(rankLength);
		for (int i = 0; i < rankLength; i++) {       // first fill all top k results with empty array
			rankedRoadPositionList.add(new Pair<>(new ArrayList<>(), 0d));
		}
		
		// start the process
		for (int i = 0; i < trajectory.size(); i++) {
			Collection<PointMatch> candidates;
			TimeStep<PointMatch, TrajectoryPoint, RoadPath> timeStep;
			TrajectoryPoint gpsPoint = trajectory.get(i);
			if (candidatesMap.get(gpsPoint).size() == 0) {  // no candidate for the current point, definitely a break point
				isBrokenTraj = true;
				breakPoints.put(i, 1);
			} else {
				candidates = candidatesMap.get(gpsPoint);
				timeStep = new TimeStep<>(gpsPoint, candidates);
				if (prevTimeStep == null) {     // start of the trajectory or the current matching has just been cut off
					computeEmissionProbabilities(timeStep);
					viterbi.startWithInitialObservation(timeStep.observation, timeStep.candidates, timeStep.emissionLogProbabilities);
					if (breakPoints.containsKey(i))
						breakPoints.put(i, 3);  // start the new match from the current point, set it as the breakpoint type 3
					// successful initialization
					prevTimeStep = timeStep;
				} else {   // continue the matching process
					final double timeDiff = (timeStep.observation.time() - prevTimeStep.observation.time());
					if (timeDiff > 180) {   // huge time gap, split the trajectory matching result
						if (indexBeforeCurrBreak != -1) {
							// we finish the matching before last break point and start a new matching sequence from break point to the
							// current gap, the previous matching sequence is finished here
							List<Pair<List<SequenceState<PointMatch, TrajectoryPoint, RoadPath>>, Double>> temporalRoadPositions =
									viterbi.computeMostLikelySequence();
							resultMerge(rankedRoadPositionList, temporalRoadPositions, trajectory, breakPoints, indexBeforeCurrBreak + 1,
									candidatesMap);
							
							// restart the matching from the last break point
							i = indexBeforeCurrBreak;
							indexBeforeCurrBreak = -1;
							prevTimeStep = null;
							continue;
						}
						List<Pair<List<SequenceState<PointMatch, TrajectoryPoint, RoadPath>>, Double>> temporalRoadPositions =
								viterbi.computeMostLikelySequence();
						resultMerge(rankedRoadPositionList, temporalRoadPositions, trajectory, breakPoints, i, candidatesMap);
						
						// set the current point as break point and restart the matching
						i--;
						prevTimeStep = null;
						continue;
					}
					
					//  no time gap, continue the matching process
					computeEmissionProbabilities(timeStep);
					computeTransitionProbabilitiesWithConnectivity(prevTimeStep, timeStep);
					viterbi.nextStep(
							timeStep.observation,
							timeStep.candidates, prevTimeStep.candidates,
							timeStep.emissionLogProbabilities,
							timeStep.transitionLogProbabilities,
							timeStep.roadPaths);
					
					if (viterbi.isBroken()) {
						// the match stops due to no connection, add the current point and its predecessor to the broken list
						isBrokenTraj = true;
						if (indexBeforeCurrBreak == -1) {
							indexBeforeCurrBreak = i - 1;
							while (breakPoints.containsKey(indexBeforeCurrBreak) && breakPoints.get(indexBeforeCurrBreak) != 3) {
								indexBeforeCurrBreak--;
							}
							if (indexBeforeCurrBreak < rankedRoadPositionList.get(0)._1().size())
								throw new IndexOutOfBoundsException("ERROR! The current breakpoint index falls into the matched result area");
						}
						// mark the broken points and expect a reconnection
						breakPoints.put(i, 2);
						viterbi.setToUnbroken();
					} else {
						// the match continues
						if (indexBeforeCurrBreak != -1) {
							// remove the break flag and continue
							indexBeforeCurrBreak = -1;
						}
						breakPoints.remove(i);
						prevTimeStep = timeStep;
					}
				}
			}
			if (i == trajectory.size() - 1) {  // the last point
				// complete the final part of the matching sequence
				if (indexBeforeCurrBreak != -1) {
					// we finish the matching before last break point and start a new matching sequence from break point to the
					// current gap and restart the match from the break point
					List<Pair<List<SequenceState<PointMatch, TrajectoryPoint, RoadPath>>, Double>> temporalRoadPositions = viterbi
							.computeMostLikelySequence();
					resultMerge(rankedRoadPositionList, temporalRoadPositions, trajectory, breakPoints, indexBeforeCurrBreak + 1,
							candidatesMap);
					
					// restart the matching from the last break point
					i = indexBeforeCurrBreak;
					indexBeforeCurrBreak = -1;
					prevTimeStep = null;
				} else {
					List<Pair<List<SequenceState<PointMatch, TrajectoryPoint, RoadPath>>, Double>> temporalRoadPositions = viterbi
							.computeMostLikelySequence();
					resultMerge(rankedRoadPositionList, temporalRoadPositions, trajectory, breakPoints, trajectory.size(), candidatesMap);
				}
			}
		}
		
		// Check whether the HMM occurred in the last time step
		if (viterbi.isBroken()) {
			throw new RuntimeException("ERROR! The hmm break still exists after the trajectory is processed.");
		}

//        for (Pair<List<SequenceState<PointMatch, TrajectoryPoint, RoadPath>>, Double> positionList : rankedRoadPositionList) {   // sort the matching result according to the trajectory point sequence
//            positionList._1().sort(Comparator.comparingLong(m -> m.observation.time()));
//        }
//        LOG.info("Time cost on matching is: " + (System.currentTimeMillis() - startTime));
		
		if (isBrokenTraj) {
			// generate unmatched trajectories
			List<Integer> breakPointList = new ArrayList<>(breakPoints.keySet());
//            List<Integer> breakPointList = new ArrayList<>(breakPoints);
			breakPointList.sort(Comparator.comparingInt(m -> m));
			Set<Integer> extendedBreakPoints = simpleBreakPointExtension(breakPointList, trajectory, candidatesMap);
			if (!extendedBreakPoints.isEmpty()) {
				List<Integer> extendedBreakPointList = new ArrayList<>(extendedBreakPoints);
				extendedBreakPointList.sort(Comparator.comparingInt(m -> m));
				int start = extendedBreakPointList.get(0);
				int end;
				for (int i = 1; i < extendedBreakPointList.size(); i++) {
					if (extendedBreakPointList.get(i) != extendedBreakPointList.get(i - 1) + 1) {
						end = extendedBreakPointList.get(i - 1);
						if (start != 0 && end != trajectory.size() - 1 && start != end) {
							Triplet<Trajectory, String, String> currUnmatchedTrajectory = new Triplet<>(trajectory.subTrajectory(start,
									end + 1), rankedRoadPositionList.get(0)._1().get(start - 1).state.getRoadID(),
									rankedRoadPositionList.get(0)._1().get(end + 1).state.getRoadID());
							currUnmatchedTrajectory._1().setID(trajectory.getID() + "U" + unmatchedTrajectoryList.size());
							unmatchedTrajectoryList.add(currUnmatchedTrajectory);
						}
						start = extendedBreakPointList.get(i);
					}
				}
				// the unmatched trajectory that includes the last trajectory point should be removed.
				end = extendedBreakPointList.get(extendedBreakPointList.size() - 1);
				if (end != trajectory.size() - 1 && start != 0 && start != end) {
					Triplet<Trajectory, String, String> currUnmatchedTrajectory = new Triplet<>(trajectory.subTrajectory(start,
							end + 1), rankedRoadPositionList.get(0)._1().get(start - 1).state.getRoadID(),
							rankedRoadPositionList.get(0)._1().get(end + 1).state.getRoadID());
					currUnmatchedTrajectory._1().setID(trajectory.getID() + "U" + unmatchedTrajectoryList.size());
					if (!currUnmatchedTrajectory._2().equals("") && !currUnmatchedTrajectory._3().equals(""))
						unmatchedTrajectoryList.add(currUnmatchedTrajectory);
				}
//            } else {
//                LOG.info("The break point(s) cannot be extended and thus removed. No unmatched trajectory output");
			}
		}
		return new MatchResultWithUnmatchedTraj(getResult(trajectory, rankedRoadPositionList), unmatchedTrajectoryList);
	}
	
	/**
	 * Extend the breakpoints to sub trajectories that are probably unmatchable.
	 *
	 * @param breakPointList The breakpoint list
	 * @param trajectory     The raw trajectory
	 * @param candidatesMap  The candidate matches mapping
	 * @return List of trajectory point index representing sub trajectories
	 */
	private Set<Integer> simpleBreakPointExtension(List<Integer> breakPointList, Trajectory trajectory, Map<TrajectoryPoint,
			Collection<PointMatch>> candidatesMap) {
		Set<Integer> extendedBreakPoints = new LinkedHashSet<>();
		int lastUnmatchedPoint = 0;
		for (int i : breakPointList) {
			boolean hasNeighbour = false;
			for (int p = 1; i - p > lastUnmatchedPoint; p++) {
				if (findMinDist(trajectory.get(i - p), candidatesMap.get(trajectory.get(i - p))) > gapExtensionDist) {
					extendedBreakPoints.add(i - p);
				} else {
					if (p != 1) {
						hasNeighbour = true;
					}
					break;
				}
			}
			for (int p = 1; i + p < trajectory.size(); p++) {
				if (findMinDist(trajectory.get(i + p), candidatesMap.get(trajectory.get(i + p))) > gapExtensionDist) {
					extendedBreakPoints.add(i + p);
					hasNeighbour = true;
				} else {
					if (p == 1 && !hasNeighbour)
						break;
					extendedBreakPoints.add(i);
					lastUnmatchedPoint = i + p - 1;
					hasNeighbour = false;
					break;
				}
			}
			if (hasNeighbour) { // if no successive point is extendable
				extendedBreakPoints.add(i);
				lastUnmatchedPoint = i;
			}
		}
		return extendedBreakPoints;
	}

//    /**
//     * Extend the breakpoints to sub trajectories that are probably unmatchable.
//     *
//     * @param breakPointList The breakpoint list
//     * @param trajectory     The raw trajectory
//     * @param candidatesMap  The candidate matches mapping
//     * @return List of trajectory point index representing sub trajectories
//     */
//    private Set<Integer> advancedBreakPointExtension(List<Integer> breakPointList, Trajectory trajectory, Map<TrajectoryPoint,
//            Collection<PointMatch>> candidatesMap) {
//        Set<Integer> extendedBreakPoints = new LinkedHashSet<>();
//        int lastUnmatchedPoint = 0;
//        for (int i : breakPointList) {
//            boolean hasNeighbour = false;
//            for (int p = 1; i - p > lastUnmatchedPoint; p++) {
//                if (findMinDist(trajectory.get(i - p), candidatesMap.get(trajectory.get(i - p))) > gapExtensionDist) {
//                    extendedBreakPoints.add(i - p);
//                } else {
//                    if (p != 1) {
//                        hasNeighbour = true;
//                    }
//                    break;
//                }
//            }
//            for (int p = 1; i + p < trajectory.size(); p++) {
//                if (findMinDist(trajectory.get(i + p), candidatesMap.get(trajectory.get(i + p))) > gapExtensionDist) {
//                    extendedBreakPoints.add(i + p);
//                    hasNeighbour = true;
//                } else {
//                    if (p == 1 && !hasNeighbour)
//                        break;
//                    extendedBreakPoints.add(i);
//                    lastUnmatchedPoint = i + p - 1;
//                    hasNeighbour = false;
//                    break;
//                }
//            }
//            if (hasNeighbour) { // if no successive point is extendable
//                extendedBreakPoints.add(i);
//                lastUnmatchedPoint = i;
//            } else {
//                for (int j = -2; j < 3; j++) {
//                    int currIndex = i + j;
//                    if (lastUnmatchedPoint <= currIndex && trajectory.size() > currIndex)
//                        extendedBreakPoints.add(currIndex);
//                }
//            }
//        }
//        return extendedBreakPoints;
//    }
	
	/**
	 * Find the closest match candidate given the trajectory point
	 *
	 * @param trajPoint        Trajectory point
	 * @param trajPointMatches Candidate matches
	 * @return The closest match candidate
	 */
	private PointMatch findNearestMatch(TrajectoryPoint trajPoint, Collection<PointMatch> trajPointMatches) {
		PointMatch nearestPointMatch = trajPointMatches.iterator().next();
		double distance = Double.POSITIVE_INFINITY;
		for (PointMatch m : trajPointMatches) {
			double currDistance = getDistance(trajPoint.x(), trajPoint.y(), m.lon(), m.lat());
			if (currDistance < distance) {
				nearestPointMatch = m;
				distance = currDistance;
			}
		}
		return nearestPointMatch;
	}
	
	/**
	 * Find the minimum distance between given trajectory point and all its candidate matches
	 *
	 * @param trajectoryPoint Trajectory point
	 * @param pointMatches    Candidate matches
	 * @return The distance between the trajectory point and its closest candidate
	 */
	private double findMinDist(TrajectoryPoint trajectoryPoint, Collection<PointMatch> pointMatches) {
		double minDistance = Double.POSITIVE_INFINITY;
		for (PointMatch p : pointMatches) {
			double dist = distFunc.distance(p.getMatchPoint(), trajectoryPoint);
			minDistance = dist < minDistance ? dist : minDistance;
		}
		return minDistance;
	}
	
	/**
	 * Compute the candidates list for every GPS point using a radius query.
	 *
	 * @param pointsList    List of GPS trajectory points to map.
	 * @param candidatesMap the candidate list for every trajectory point
	 */
	private void computeCandidatesFromIndex(Collection<TrajectoryPoint> pointsList, Map<TrajectoryPoint, Collection<PointMatch>> candidatesMap,
											Grid<SegmentWithIndex> grid) {
//        int candidateCount = 0;
		for (TrajectoryPoint p : pointsList) {
			Set<String> candidateFilter = new HashSet<>();
			// As we set the grid size as the candidateRange, only the partition that contains the query point and its neighbouring
			// partitions can potentially generate candidates
			candidatesMap.put(p, new ArrayList<>());
			List<GridPartition<SegmentWithIndex>> partitionList = new ArrayList<>();
			partitionList.add(grid.partitionSearch(p.x(), p.y()));
			partitionList.addAll(grid.adjacentPartitionSearch(p.x(), p.y()));
			for (GridPartition<SegmentWithIndex> partition : partitionList) {
				if (partition != null)
					for (XYObject<SegmentWithIndex> item : partition.getObjectsList()) {
						SegmentWithIndex indexItem = item.getSpatialObject();
						if (!candidateFilter.contains(indexItem.getSegment().x1() + "," + indexItem.getSegment().y1() + "_" +
								indexItem.getSegment().x2() + "," + indexItem.getSegment().y2() + "_" + indexItem.getRoadID())) {
							Point matchingPoint = distFunc.getClosestPoint(p, indexItem.getSegment());
							if (distFunc.distance(p, matchingPoint) < candidateRange) {
								PointMatch candidate = new PointMatch(matchingPoint, indexItem.getSegment(), indexItem.getRoadID());
								candidatesMap.get(p).add(candidate);
//                                candidateCount++;
								candidateFilter.add(indexItem.getSegment().x1() + "," + indexItem.getSegment().y1() + "_" +
										indexItem.getSegment().x2() + "," + indexItem.getSegment().y2() + "_" + indexItem.getRoadID());
							}
						}
					}
			}
		}
//        LOG.info("Total candidate count: " + candidateCount + ", trajectory point count: " + pointsList.size());
	}
	
	/**
	 * Compute the emission probabilities between every GPS point and its candidates.
	 *
	 * @param timeStep the observation and its candidate
	 */
	private void computeEmissionProbabilities(TimeStep<PointMatch, TrajectoryPoint, RoadPath> timeStep) {
		for (PointMatch candidate : timeStep.candidates) {
			double distance = getDistance(
					timeStep.observation.x(), timeStep.observation.y(),
					candidate.lon(), candidate.lat());
			timeStep.addEmissionLogProbability(candidate,
					hmmProbabilities.emissionLogProbability(distance));
		}
	}
	
	/**
	 * Compute the transition probabilities between every state, taking the connectivity of the road nodes into account.
	 *
	 * @param prevTimeStep the time step of the last trajectory point
	 * @param timeStep     the current time step
	 */
	private void computeTransitionProbabilitiesWithConnectivity(TimeStep<PointMatch, TrajectoryPoint, RoadPath>
																		prevTimeStep, TimeStep<PointMatch, TrajectoryPoint, RoadPath> timeStep) {
		final double linearDistance = getDistance(prevTimeStep.observation.x(), prevTimeStep.observation.y(), timeStep.observation.x(),
				timeStep.observation.y());
		final double timeDiff = (timeStep.observation.time() - prevTimeStep.observation.time());
		double maxDistance = (50 * timeDiff) < linearDistance * 8 ? 50 * timeDiff : linearDistance * 8; // limit the maximum speed to
		// 180km/h
//        double maxDistance = 50 * timeDiff;
		double uTurnPenalty = prop.getPropertyDouble("algorithm.mapmatching.hmm.UTurnPenalty");
		for (PointMatch from : prevTimeStep.candidates) {
			List<PointMatch> candidates = new ArrayList<>(timeStep.candidates);
			List<Pair<Double, List<String>>> shortestPathResultList = routingGraph.calculateShortestDistanceList(from, candidates, maxDistance);
			for (int i = 0; i < candidates.size(); i++) {
				if (shortestPathResultList.get(i)._1() != Double.POSITIVE_INFINITY) {
					if (shortestPathResultList.get(i)._2().contains(reverseID(from.getRoadID())))
						shortestPathResultList.get(i).set_1(shortestPathResultList.get(i)._1() + uTurnPenalty);
					timeStep.addRoadPath(from, candidates.get(i), new RoadPath(from, candidates.get(i), shortestPathResultList.get(i)._2()));
					double transitionLogProbability = hmmProbabilities.transitionLogProbability(shortestPathResultList.get(i)._1(),
							linearDistance, timeDiff);
//                    // apply the penalty if the path incurs an u-turn
//                    if (!from.getRoadID().equals(candidates.get(i).getRoadID()) && Math.abs(Long.parseLong(from.getRoadID())) == Math.abs
//                            (Long.parseLong(candidates.get(i).getRoadID())))
//                        transitionLogProbability += U_TURN_PENALTY;
					timeStep.addTransitionLogProbability(from, candidates.get(i), transitionLogProbability);
				}
			}
		}
	}
	
	private String reverseID(String roadID) {
		if (roadID.contains("-"))
			return roadID.substring(1);
		else return "-" + roadID;
	}
	
	/**
	 * Extract the map-matching result (point-node pairs) from the HMM algorithm result.
	 *
	 * @param matchResultList The Viterbi algorithm result list.
	 * @return The map-matching result for trajectory.
	 */
	private MultipleTrajectoryMatchResult getResult(Trajectory traj, List<Pair<List<SequenceState<PointMatch, TrajectoryPoint, RoadPath>>, Double>>
			matchResultList) {
		double[] probabilities = new double[rankLength];
		List<List<PointMatch>> pointMatchList = new ArrayList<>(rankLength);
		List<List<Route>> routeMatchList = new ArrayList<>(rankLength);
		List<BitSet> breakPointBSList = new ArrayList<>(rankLength);
		for (int i = 0; i < matchResultList.size(); i++) {     // rank
			Pair<List<SequenceState<PointMatch, TrajectoryPoint, RoadPath>>, Double> roadPosition = matchResultList.get(i);
			List<PointMatch> pointMatches = new ArrayList<>();
			List<Route> routeMatches = new ArrayList<>();
			BitSet breakPointBS = new BitSet(roadPosition._1().size());
			List<SequenceState<PointMatch, TrajectoryPoint, RoadPath>> sequenceStates = roadPosition._1();
			Point prevEndMatchPoint = null;
			for (int j = 0; j < sequenceStates.size(); j++) {    // trajectory length
				SequenceState<PointMatch, TrajectoryPoint, RoadPath> sequence = sequenceStates.get(j);
				PointMatch pointMatch = sequence.state;
				List<String> currRoadIDList = new ArrayList<>();
				if (pointMatch != null) {
					// make sure it returns a copy of the objects
					pointMatches.add(pointMatch);
				} else {
					throw new NullPointerException("Point matching result should not have NULL value");
				}
				if (sequence.transitionDescriptor != null && sequence.transitionDescriptor.from != null && sequence.transitionDescriptor.to != null) {
					if (prevEndMatchPoint != null && !prevEndMatchPoint.equals2D(sequence.transitionDescriptor.from.getMatchPoint()))
						breakPointBS.set(j);
					if (sequence.transitionDescriptor.passingRoadID.size() == 0) {
						if (sequence.transitionDescriptor.from.getRoadID().equals(sequence.transitionDescriptor.to.getRoadID())) {
							currRoadIDList.add(sequence.transitionDescriptor.from.getRoadID());
						} else {
							currRoadIDList.add(sequence.transitionDescriptor.from.getRoadID());
							currRoadIDList.add(sequence.transitionDescriptor.to.getRoadID());
						}
					} else {
						String prevID = "";
						if (!sequence.transitionDescriptor.from.getRoadID().equals(sequence.transitionDescriptor.passingRoadID.get(0))) {
							prevID = sequence.transitionDescriptor.from.getRoadID();
							currRoadIDList.add(prevID);
						}
						for (String s : sequence.transitionDescriptor.passingRoadID) {
							if (!s.equals(prevID)) {
								currRoadIDList.add(s);
								prevID = s;
							}
						}
						if (!sequence.transitionDescriptor.to.getRoadID().equals(prevID))
							currRoadIDList.add(sequence.transitionDescriptor.to.getRoadID());
					}
					Route currRoute = new Route(sequence.transitionDescriptor.from.getMatchPoint(),
							sequence.transitionDescriptor.to.getMatchPoint(), currRoadIDList);
					routeMatches.add(currRoute);
					prevEndMatchPoint = sequence.transitionDescriptor.to.getMatchPoint();
				} else if (sequence.transitionDescriptor == null && j != 0) {    // no match candidate. empty matching result
					breakPointBS.set(j);
					Route currRoute = new Route(sequence.state.getMatchPoint(), sequence.state.getMatchPoint(), currRoadIDList);
					routeMatches.add(currRoute);
				} else if (sequence.transitionDescriptor != null) {        // break point, match to the closest point
					breakPointBS.set(j);
					Route currRoute = new Route(sequence.state.getMatchPoint(), sequence.state.getMatchPoint(), currRoadIDList);
					routeMatches.add(currRoute);
				} else {    // the first point of the trajectory has no route match result
					currRoadIDList.add(sequence.state.getRoadID());
					Route currRoute = new Route(sequence.state.getMatchPoint(), sequence.state.getMatchPoint(), currRoadIDList);
					routeMatches.add(currRoute);
				}
			}
			pointMatchList.add(pointMatches);
			routeMatchList.add(routeMatches);
			breakPointBSList.add(breakPointBS);
			// the probability should be converted to non-log mode
//            probabilities[i] = roadPosition._2() == 0 ? 0 : Math.exp(roadPosition._2());
			// probability normalization
			if (roadPosition._2() == null || roadPosition._2() == 0 || traj.size() <= 1 || Double.isNaN((roadPosition._2() / traj.size())))
				LOG.info("TEST");
			probabilities[i] = roadPosition._2() == 0 ? 0 : Math.exp(roadPosition._2() / traj.size());
		}
		return new MultipleTrajectoryMatchResult(traj, rankLength, matchResultList.size(), pointMatchList, routeMatchList, probabilities,
				breakPointBSList);
	}
	
	/**
	 * The distance between the two given coordinates, using
	 * the specified distance function.
	 *
	 * @param x1 longitude of the first point
	 * @param y1 latitude of the first point
	 * @param x2 longitude of the second point
	 * @param y2 latitude of the second point
	 * @return Distance from (x1,y1) to (x2,y2).
	 */
	private double getDistance(double x1, double y1, double x2, double y2) {
		return distFunc.pointToPointDistance(x1, y1, x2, y2);
	}
	
	/**
	 * Insert the partial matching result into the final result, the broken points along the way should be inserted as well. The broken
	 * points are matched to its geographically closest point or null if no point is close to it. In addition, the probability is
	 * accumulated, but it will turn to zero if either the previous or the current probability is zero.
	 *
	 * @param rankedRoadPositionList final matching result list
	 * @param temporalRoadPositions  current temporal matching result list
	 * @param trajectory             raw trajectory
	 * @param breakPoints            the break point list
	 * @param destinationIndex       the size of the matching result after insertion, it should be temporal+breakPoints
	 * @param candidatesMap          the candidate map of each raw trajectory point
	 */
	private void resultMerge
	(List<Pair<List<SequenceState<PointMatch, TrajectoryPoint, RoadPath>>, Double>> rankedRoadPositionList,
	 List<Pair<List<SequenceState<PointMatch, TrajectoryPoint, RoadPath>>, Double>> temporalRoadPositions, Trajectory
			 trajectory, Map<Integer, Integer> breakPoints, int destinationIndex, Map<TrajectoryPoint,
			Collection<PointMatch>> candidatesMap) {
		for (int rank = 0; rank < rankLength; rank++) {
			if (temporalRoadPositions.size() == 0) {
				LOG.warn("The current trajectory has no matching result.");
				return;
			}
			int validRank = rank < temporalRoadPositions.size() ? rank : temporalRoadPositions.size() - 1;  // fill the rest of the rank
			// list with the last valid sequence
			int startPosition = rankedRoadPositionList.get(rank)._1().size();
			int cursor = 0;
			List<SequenceState<PointMatch, TrajectoryPoint, RoadPath>> roadPositionList = rankedRoadPositionList.get(rank)._1();
			double unmatchedProbability = 0;  // for each unmatched trajectory point, we add an emission probability and the
			// transition probabilities
			for (int k = startPosition; k < destinationIndex; k++) {
				// if the current point is a breaking point
				if (breakPoints.containsKey(k) && breakPoints.get(k) != 3) {
					if (breakPoints.get(k) == 1) { // if the point does not have candidate
						roadPositionList.add(new SequenceState<>(new PointMatch(distFunc), trajectory.get(k), null));
						unmatchedProbability += hmmProbabilities.emissionLogProbability(candidateRange);
						if (k != 0) {
							unmatchedProbability += hmmProbabilities.maxTransitionLogProbability(distFunc.distance(trajectory.get(k - 1),
									trajectory.get(k)), trajectory.get(k).time() - trajectory.get(k - 1).time());
						}
					} else {
						List<String> roadIdList = new ArrayList<>();
						PointMatch closestMatch = findNearestMatch(trajectory.get(k), candidatesMap.get(trajectory.get(k)));
						roadIdList.add(closestMatch.getRoadID());
						roadPositionList.add(new SequenceState<>(closestMatch, trajectory.get(k), new RoadPath(null, null,
								roadIdList)));
//                            double distance = distFunc.distance(closestMatch.getMatchPoint(), trajectory.get(k));
						unmatchedProbability += hmmProbabilities.emissionLogProbability(candidateRange);
						if (k != 0) {
							unmatchedProbability += hmmProbabilities.maxTransitionLogProbability(distFunc.distance(trajectory.get(k - 1),
									trajectory.get(k)), trajectory.get(k).time() - trajectory.get(k - 1).time());
						}
					}
				} else {
					if (breakPoints.containsKey(k) && breakPoints.get(k) == 3 && cursor != 0)
						LOG.error("The current sequence contains a type 3 break point");
					if (temporalRoadPositions.get(validRank)._1().get(cursor).observation.equals2D(trajectory.get(k))) {
						roadPositionList.add(temporalRoadPositions.get(validRank)._1().get(cursor));
						cursor++;
					} else
						LOG.error("The matching result mismatch!"); // the result sequence doesn't match the raw
					// trajectory sequence
				}
			}
			double prevProbability = rankedRoadPositionList.get(rank)._2();
			double currProbability = temporalRoadPositions.get(validRank)._2();
			// add probability, probability = 0 if either of the probability is 0
			if (prevProbability == Double.NEGATIVE_INFINITY || currProbability == Double.NEGATIVE_INFINITY)
				rankedRoadPositionList.get(rank).set_2(Double.NEGATIVE_INFINITY);
			else if (prevProbability == 0)  // empty probability list
				rankedRoadPositionList.get(rank).set_2(currProbability + unmatchedProbability);
			else {
				if (startPosition == 0)
					LOG.error("ERROR! Non-zero probability for an empty sequence.");
				unmatchedProbability += hmmProbabilities.maxTransitionLogProbability(distFunc.distance(trajectory.get(startPosition - 1),
						trajectory.get(startPosition)), trajectory.get(startPosition).time() - trajectory.get(startPosition - 1).time());
				rankedRoadPositionList.get(rank).set_2(prevProbability + currProbability + unmatchedProbability);
			}
//
//                for (int k = rankedRoadPositionList.get(rank)._1().size(); k < destinationIndex; k++) {
//                    rankedRoadPositionList.get(rank)._1().add(new SequenceState<>(new PointMatch(), trajectory.get(k), null));
//                }
//                rankedRoadPositionList.get(rank).set_2(Double.NEGATIVE_INFINITY);
		}
	}
}