package algorithm.mapmerge;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.index.grid.Grid;
import util.index.grid.GridPartition;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;
import util.object.structure.Pair;
import util.object.structure.Triplet;
import util.object.structure.XYObject;
import util.settings.BaseProperty;

import java.text.DecimalFormat;
import java.util.*;

public class MapMerge {
	
	private static final Logger LOG = Logger.getLogger(MapMerge.class);
	private final DistanceFunction distFunc;
	private final BaseProperty prop;
	private RoadNetworkGraph rawMap;
	private Map<String, String> loc2RemovedWayID = new HashMap<>();
	private Map<String, RoadWay> loc2RemovedWayMapping = new HashMap<>();
	private Map<String, Pair<RoadWay, Double>> loc2InsertedWayDist = new LinkedHashMap<>();    // a mapping between the road location and
	// its already inserted road with combined distance to its endpoints
	private Map<String, RoadWay> loc2RoadWayMapping = new HashMap<>();
	private Map<String, RoadWay> id2RoadWayMapping = new HashMap<>();
	private Map<String, List<RoadNode>> loc2RoadNodeListMapping = new HashMap<>();  // multiple road nodes may have the same location
	private Map<String, List<RoadWay>> tempPoint2EdgeIndexMapping = new LinkedHashMap<>();
	private Grid<Point> grid;
	private int mergeCandidateDist;    // the maximum distance that an intersection can be considered as the candidate of a
	// merge, usually equal to CandidateRange
	private int subTrajMergeDist = 10;    //
	// the following parameters are used to assign road number for new roads and nodes.
	private long maxAbsRoadWayID;
	private long maxMiniNodeID;
	
	public MapMerge(RoadNetworkGraph rawMap, List<RoadWay> removedWayList, DistanceFunction distFunc, BaseProperty prop) {
		this.rawMap = rawMap;
		for (RoadWay w : rawMap.getWays())
			w.setNewRoad(false);
		for (RoadWay w : removedWayList) {
			this.loc2RemovedWayID.put(w.getFromNode().lon() + "_" + w.getFromNode().lat() + "," + w.getToNode().lon() + "_" + w.getToNode()
					.lat(), w.getID());
			this.loc2RemovedWayID.put(w.getToNode().lon() + "_" + w.getToNode().lat() + "," + w.getFromNode().lon() + "_" + w.getFromNode()
					.lat(), w.getID().contains("-") ? w.getID().substring(w.getID().indexOf("-") + 1) : "-" + w.getID());
			this.loc2RemovedWayMapping.put(w.getFromNode().lon() + "_" + w.getFromNode().lat() + "," + w.getToNode().lon() + "_" + w.getToNode()
					.lat(), w);
		}
		this.prop = prop;
		int mergeCandidateDistance = prop.getPropertyInteger("algorithm.mapmatching.CandidateRange");
		if (mergeCandidateDistance > 0)
			this.mergeCandidateDist = mergeCandidateDistance;
		else
			LOG.error("The merge candidate search range is illegal: " + mergeCandidateDistance);
		int subTrajMergeDist = prop.getPropertyInteger("algorithm.mapmerge.SubTrajectoryMergeDistance");
		if (subTrajMergeDist > 0)
			this.subTrajMergeDist = subTrajMergeDist;
		else
			LOG.error("The sub-trajectory merge candidate search range is illegal: " + subTrajMergeDist);
		for (RoadWay w : rawMap.getWays()) {
			loc2RoadWayMapping.put(w.getFromNode().lon() + "_" + w.getFromNode().lat() + "," + w.getToNode().lon() + "_" + w.getToNode()
					.lat(), w);
			id2RoadWayMapping.put(w.getID(), w);
		}
		this.maxAbsRoadWayID = rawMap.getMaxAbsWayID();
		this.maxMiniNodeID = rawMap.getMaxMiniNodeID();
		this.distFunc = distFunc;
	}
	
	/**
	 * Connect the consecutive road and conjunct into one road, road are able to be merged if the connecting point only have degree of 2
	 * or they have the similar direction.
	 *
	 * @param inferredWayList The input list of road ways.
	 * @return The road ways after conjunction.
	 */
	private List<RoadWay> roadConjunction(List<RoadWay> inferredWayList) {
		for (Iterator<RoadWay> iterator = inferredWayList.iterator(); iterator.hasNext(); ) {
			RoadWay w = iterator.next();
			String fromLocIndex = w.getFromNode().lon() + "_" + w.getFromNode().lat();
			if (!insertWay2Index(w, fromLocIndex)) {
				iterator.remove();
				continue;
			}
			String toLocIndex = w.getToNode().lon() + "_" + w.getToNode().lat();
			insertWay2Index(w, toLocIndex);
		}

//        LOG.info("Total number of inferred road is: " + inferredWayList.size() + ", number of additional nodes is: " + tempPoint2EdgeIndexMapping.entrySet().size());
		
		List<RoadWay> roadWayResult = new ArrayList<>();
		double minRoadLength = prop.getPropertyDouble("algorithm.mapmerge.MinimumRoadLength");
		for (RoadWay w : inferredWayList) {
			if (w.getVisitCount() != 1) {
				w.setVisitCount(1);
				RoadWay extendedWay = roadExtension(w, w.getToNode().toPoint(), true);
				extendedWay = roadExtension(extendedWay, w.getFromNode().toPoint(), false);
				if (extendedWay.getLength() > minRoadLength / 2)
					roadWayResult.add(extendedWay);
			}
		}
		
		for (Map.Entry<String, List<RoadWay>> entry : tempPoint2EdgeIndexMapping.entrySet()) {
			if (entry.getValue().size() != 0)
				LOG.error("Unvisited road remaining.");
		}
//        LOG.info("Total number of roads after conjunction: " + roadWayResult.size());
		return roadWayResult;
	}
	
	/**
	 * Insert the road to the temporary road node index so that the roads on the same node can be connected in the future.
	 *
	 * @param w        The road way to be inserted.
	 * @param locIndex The point location index.
	 * @return True if the road is inserted successfully.
	 */
	private boolean insertWay2Index(RoadWay w, String locIndex) {
		String tempIndex = w.getFromNode().lon() + "_" + w.getFromNode().lat();
		String oppoLocIndex = tempIndex.equals(locIndex) ? w.getToNode().lon() + "_" + w.getToNode().lat() : tempIndex;   // the
		// index of the other endpoint of inserting road
		if (!tempPoint2EdgeIndexMapping.containsKey(locIndex)) {
			List<RoadWay> currWayList = new ArrayList<>();
			currWayList.add(w);
			tempPoint2EdgeIndexMapping.put(locIndex, currWayList);
		} else {
			List<RoadWay> wayList = tempPoint2EdgeIndexMapping.get(locIndex);
			for (RoadWay currWay : wayList) {
				String currTempIndex = currWay.getFromNode().lon() + "_" + currWay.getFromNode().lat();
				String currOppoLocIndex = currTempIndex.equals(locIndex) ? currWay.getToNode().lon() + "_" + currWay.getToNode().lat()
						: currTempIndex;
				if (oppoLocIndex.equals(currOppoLocIndex))  // definitely there exists entry for the other endpoint, return straight away
					return false;
			}
			wayList.add(w);
		}
		if (!tempPoint2EdgeIndexMapping.containsKey(oppoLocIndex)) {
			List<RoadWay> currWayList = new ArrayList<>();
			currWayList.add(w);
			tempPoint2EdgeIndexMapping.put(oppoLocIndex, currWayList);
		} else {
			tempPoint2EdgeIndexMapping.get(locIndex).add(w);
		}
		return true;
	}
	
	/**
	 * Extend the current road way to its left or right.
	 *
	 * @param currWay            The current road way.
	 * @param lastConnectedPoint The endpoint of the last connected road way, which is the endpoint of the current road way if it
	 *                           has not been extended, or the endpoint of the most recently joined road way.
	 * @param isLeft             The extension direction, true if extend through its from node, or false if extend through its to node.
	 * @return The extended road.
	 */
	private RoadWay roadExtension(RoadWay currWay, Point lastConnectedPoint, boolean isLeft) {
		if (isLeft) {
			String centerLocIndex = currWay.getFromNode().lon() + "_" + currWay.getFromNode().lat();
			Point centerPoint = currWay.getFromNode().toPoint();
			if (tempPoint2EdgeIndexMapping.get(centerLocIndex).size() == 1) {
				// the current extension reach the end
				tempPoint2EdgeIndexMapping.remove(centerLocIndex);
				return currWay;
			} else {
				// at least two road ways(including itself) connect to the current intersection
				boolean isExtended = false;
				RoadWay resultRoadWay = null;
				Iterator<RoadWay> it = tempPoint2EdgeIndexMapping.get(centerLocIndex).iterator();
				while (it.hasNext()) {
					RoadWay iter = it.next();
					Point endPoint = iter.getFromNode().toPoint().equals2D(centerPoint) ? iter.getToNode().toPoint() :
							iter.getFromNode().toPoint();
					if (endPoint.equals2D(lastConnectedPoint)) {
						// the connected road found, remove it from the intersection
						it.remove();
					} else if (!isExtended && (calculateAngle(centerPoint, endPoint, currWay.getNode(1).toPoint()) < 30)) {
						RoadWay roadWayResult = roadWayMerge(currWay, iter, centerPoint);
						resultRoadWay = roadExtension(roadWayResult, centerPoint, true);
						it.remove();
						isExtended = true;
					}
				}
				return resultRoadWay == null ? currWay : resultRoadWay;
			}
		} else {
			String centerLocIndex = currWay.getToNode().lon() + "_" + currWay.getToNode().lat();
			Point centerPoint = currWay.getToNode().toPoint();
			if (tempPoint2EdgeIndexMapping.get(centerLocIndex).size() == 1) {
				// the current extension reach the end
				tempPoint2EdgeIndexMapping.remove(centerLocIndex);
				return currWay;
			} else {
				// at least two road ways(including itself) connect to the current intersection
				boolean isExtended = false;
				RoadWay resultRoadWay = null;
				Iterator<RoadWay> it = tempPoint2EdgeIndexMapping.get(centerLocIndex).iterator();
				while (it.hasNext()) {
					RoadWay iter = it.next();
					Point endPoint = iter.getFromNode().toPoint().equals2D(centerPoint) ? iter.getToNode().toPoint() :
							iter.getFromNode().toPoint();
					if (endPoint.equals2D(lastConnectedPoint)) {
						// the connected road found, remove it from the intersection
						it.remove();
					} else if (!isExtended && calculateAngle(centerPoint, endPoint, currWay.getNode(currWay.size() - 2).toPoint()) < 30) {
						RoadWay roadWayResult = roadWayMerge(currWay, iter, centerPoint);
						resultRoadWay = roadExtension(roadWayResult, centerPoint, false);
						it.remove();
						isExtended = true;
					}
				}
				return resultRoadWay == null ? currWay : resultRoadWay;
			}
		}
	}
	
	private RoadWay roadWayMerge(RoadWay currWay, RoadWay mergingWay, Point intersectPoint) {
		mergingWay.setVisitCount(1);
		List<RoadNode> mergeRoadNodeList = new ArrayList<>();
		
		// check if the intersection is correct
		if (!((currWay.getFromNode().toPoint().equals2D(intersectPoint) || currWay.getToNode().toPoint().equals2D(intersectPoint)) &&
				(mergingWay.getFromNode().toPoint().equals2D(intersectPoint) || mergingWay.getToNode().toPoint().equals2D(intersectPoint))))
			LOG.error("ERROR! The merging road ways are not connected.");
		
		if (currWay.getFromNode().toPoint().equals2D(intersectPoint)) {
			// the intersection is the start point of the existing road, insert the new road first
			if (mergingWay.getFromNode().toPoint().equals2D(intersectPoint)) {
				for (int i = mergingWay.getNodes().size() - 1; i >= 0; i--) {
					mergeRoadNodeList.add(mergingWay.getNode(i));
				}
			} else {
				mergeRoadNodeList.addAll(mergingWay.getNodes());
			}
			// remove the endpoint as it will be re-added later, then add the current way into the list
			mergeRoadNodeList.remove(mergeRoadNodeList.size() - 1);
			mergeRoadNodeList.addAll(currWay.getNodes());
		} else {
			// the intersection is the end point of the existing road, insert the current way first
			mergeRoadNodeList.addAll(currWay.getNodes());
			// remove the endpoint as it will be re-added later, then add the new way into the list
			mergeRoadNodeList.remove(mergeRoadNodeList.size() - 1);
			if (mergingWay.getFromNode().toPoint().equals2D(intersectPoint)) {
				mergeRoadNodeList.addAll(mergingWay.getNodes());
			} else {
				for (int i = mergingWay.getNodes().size() - 1; i >= 0; i--) {
					mergeRoadNodeList.add(mergingWay.getNode(i));
				}
			}
		}
		RoadWay mergeResult = new RoadWay(currWay.getID(), mergeRoadNodeList, distFunc);
		mergeResult.setNewRoad(true);
		mergeResult.setConfidenceScore((currWay.getConfidenceScore() + mergingWay.getConfidenceScore()) / 2.0);
		return mergeResult;
	}
	
	/**
	 * Calculate the angle between two lines: (newPoint,centerPoint) and (centerPoint, prevPoint)
	 *
	 * @param centerPoint The point that two line intersects
	 * @param newPoint    Another endpoint of the new line
	 * @param prevPoint   Another endpoint of the previous line
	 * @return The angle in degree(positive)
	 */
	private double calculateAngle(Point centerPoint, Point newPoint, Point prevPoint) {
		double angle1 = Math.atan2(newPoint.y() - centerPoint.y(), newPoint.x() - centerPoint.x());
		double angle2 = Math.atan2(centerPoint.y() - prevPoint.y(), centerPoint.x() - prevPoint.x());
		return Math.toDegrees(Math.abs(angle1 - angle2));
	}
	
	public List<RoadWay> nearestNeighbourMapMerge(List<RoadWay> inferredWayList,
												  HashMap<String, Pair<HashSet<String>, HashSet<String>>> newRoadID2AnchorPoints) {
		
		// the inference result from KDE map inference requires a road conjunction step
		List<RoadWay> inferredList;
		List<RoadWay> insertWayList;
		if (newRoadID2AnchorPoints.isEmpty())
			inferredList = inferredWayList;
		else
			inferredList = roadConjunction(inferredWayList);
		
		buildGridIndex();
		
		for (RoadWay w : inferredList) {
//            LOG.info("start current road way connection, road length:" + w.getLength());
			HashMap<String, Pair<Point, Double>> startPointMatchCandidate = new HashMap<>();
			HashMap<String, Pair<Point, Double>> endPointMatchCandidate = new HashMap<>();
			// anchor points are prioritized
			if (!newRoadID2AnchorPoints.isEmpty() && newRoadID2AnchorPoints.containsKey(w.getID())) {
				HashSet<String> startRoadWayList = newRoadID2AnchorPoints.get(w.getID())._1();
				HashSet<String> endRoadWayList = newRoadID2AnchorPoints.get(w.getID())._2();
				int candidateRange = prop.getPropertyInteger("algorithm.mapmatching.CandidateRange");
				for (String s : startRoadWayList) {
					if (!id2RoadWayMapping.containsKey(s))
						LOG.error("ERROR! Road doesn't exist:" + s);
					Point firstPoint = id2RoadWayMapping.get(s).getToNode().toPoint();
					if (!this.grid.getModel().getBoundary().contains(firstPoint))
						continue;
					if (distFunc.distance(w.getFromNode().toPoint(), firstPoint) > 4 * candidateRange) {
						continue;
					}
					String locIndex = firstPoint.x() + "_" + firstPoint.y();
					startPointMatchCandidate.put(locIndex, new Pair<>(firstPoint, distFunc.distance(w.getFromNode().toPoint(), firstPoint)));
					
					Point secondPoint = id2RoadWayMapping.get(s).getFromNode().toPoint();
					if (distFunc.distance(w.getFromNode().toPoint(), secondPoint) > 4 * candidateRange) {
						continue;
					}
					locIndex = secondPoint.x() + "_" + secondPoint.y();
					startPointMatchCandidate.put(locIndex, new Pair<>(secondPoint, distFunc.distance(w.getFromNode().toPoint(), secondPoint)));
				}
				for (String s : endRoadWayList) {
					if (!id2RoadWayMapping.containsKey(s))
						LOG.error("ERROR! Road doesn't exist:" + s);
					Point firstPoint = id2RoadWayMapping.get(s).getFromNode().toPoint();
					if (!this.grid.getModel().getBoundary().contains(firstPoint))
						continue;
					if (distFunc.distance(w.getToNode().toPoint(), firstPoint) > 4 * candidateRange) {
						continue;
					}
					String locIndex = firstPoint.x() + "_" + firstPoint.y();
					endPointMatchCandidate.put(locIndex, new Pair<>(firstPoint, distFunc.distance(w.getToNode().toPoint(), firstPoint)));
					
					Point secondPoint = id2RoadWayMapping.get(s).getToNode().toPoint();
					if (distFunc.distance(w.getToNode().toPoint(), secondPoint) > 4 * candidateRange) {
						continue;
					}
					locIndex = secondPoint.x() + "_" + secondPoint.y();
					endPointMatchCandidate.put(locIndex, new Pair<>(secondPoint, distFunc.distance(w.getToNode().toPoint(), secondPoint)));
				}
				if (startPointMatchCandidate.size() > 0 && endPointMatchCandidate.size() > 0) {
					Point startPoint;
					Point endPoint;
					String currRoadID;
					Triplet<Point, Point, Triplet<String, Double, HashSet<String>>> bestMatch = findBestMatch(startPointMatchCandidate,
							endPointMatchCandidate);
					startPoint = bestMatch._1();
					endPoint = bestMatch._2();
					currRoadID = bestMatch._3()._1();
					if (!currRoadID.equals("")) {   // the inferred road has been assigned to a removed road
						if (startPoint != null) {  // not matched to an already inserted removed road which has better quality, insert it
							String currLoc = startPoint.x() + "_" + startPoint.y() + "," + endPoint.x() + "_" + endPoint.y();
							w.setId(currRoadID);
							RoadWay newWay = roadMapConnection(w.getID(), w.getNodes(), w.getConfidenceScore(), startPoint, endPoint, true);
							loc2InsertWayDistUpdate(bestMatch, currLoc, newWay);
							continue;
						} else if (!bestMatch._3()._3().isEmpty()) {    // assigned to a inserted removed road way, add the confidence
							// score to it
							for (String s : bestMatch._3()._3()) {
								double currConfidenceScore = loc2InsertedWayDist.get(s)._1().getConfidenceScore();
								loc2InsertedWayDist.get(s)._1().setConfidenceScore(currConfidenceScore + w.getConfidenceScore());
							}
							continue;
						}
					}
				}
			}
			// find an possible intersection pair which the edge can be added to, prioritize the intersection pairs of the removed edge.
			startPointMatchCandidate.putAll(findPointMatchCandidate(w.getFromNode().lon(), w.getFromNode().lat(), mergeCandidateDist));
			endPointMatchCandidate.putAll(findPointMatchCandidate(w.getToNode().lon(), w.getToNode().lat(), mergeCandidateDist));
			if (startPointMatchCandidate.size() > 0 && endPointMatchCandidate.size() > 0) {
				Point startPoint;
				Point endPoint;
				String currRoadID;
				Triplet<Point, Point, Triplet<String, Double, HashSet<String>>> bestMatch = findBestMatch(startPointMatchCandidate,
						endPointMatchCandidate);
				startPoint = bestMatch._1();
				endPoint = bestMatch._2();
				currRoadID = bestMatch._3()._1();
				
				if (startPoint != null) {   // at least one pair of intersections is found
//                    LOG.info("Both endpoints can be matched to the map");
					if (!currRoadID.equals("")) {
						// the inferred road has been assigned to a removed road
						String currLoc = startPoint.x() + "_" + startPoint.y() + "," + endPoint.x() + "_" + endPoint.y();
						w.setId(currRoadID);
						RoadWay newWay = roadMapConnection(w.getID(), w.getNodes(), w.getConfidenceScore(), startPoint, endPoint, true);
						loc2InsertWayDistUpdate(bestMatch, currLoc, newWay);
					} else {
						// the inferred road is assigned to a new road, check whether the new road cover any existing removed road
						roadRefinement(w, startPoint, endPoint, mergeCandidateDist);
					}
				} else if (!currRoadID.equals("")) {    // assigned to a inserted removed road way, add the confidence
					if (!bestMatch._3()._3().isEmpty()) {
						// score to it
						for (String s : bestMatch._3()._3()) {
							double currConfidenceScore = loc2InsertedWayDist.get(s)._1().getConfidenceScore();
							loc2InsertedWayDist.get(s)._1().setConfidenceScore(currConfidenceScore + w.getConfidenceScore());
						}
					}
				} else {    // no point pairs can be found, find sub matches
					findSubRoadConnection(w);   // find sub-trajectories that can be connected to the existing road ways
				}
			} else {
//                LOG.info("Not all endpoint can be matched to the map, start subRoadConnection");
				findSubRoadConnection(w);   // find sub-trajectories that can be connected to the existing road ways
			}
		}
		insertWayList = doubleDirectedRoadWayInsertion();
		LOG.info("Nearest neighbour map merge completed. Total number of road way added:" + insertWayList.size());
		return insertWayList;
	}
	
	private void loc2InsertWayDistUpdate(Triplet<Point, Point, Triplet<String, Double, HashSet<String>>> bestMatch, String currLoc, RoadWay newWay) {
		if (loc2InsertedWayDist.containsKey(currLoc)) {      // an worse removed road has been inserted, replace it
			double prevConfScore = loc2InsertedWayDist.get(currLoc)._1().getConfidenceScore();
			newWay.setConfidenceScore(newWay.getConfidenceScore() + prevConfScore);
			loc2InsertedWayDist.replace(currLoc, new Pair<>(newWay, bestMatch._3()._2()));
		} else
			loc2InsertedWayDist.put(currLoc, new Pair<>(newWay, bestMatch._3()._2()));
	}
	
	private void roadRefinement(RoadWay currRoad, Point startPoint, Point endPoint, int distance) {
		List<RoadNode> currRoadIntermediatePoint = new ArrayList<>();
		Point prevMatchPoint = startPoint;
		boolean removedRoadFound = false;
		for (int i = 0; i < currRoad.getNodes().size() - 1; i++) {
			List<Point> intermediatePoint = edgeSegmentation(currRoad.getNode(i), currRoad.getNode(i + 1));
			for (Point p : intermediatePoint) {
				HashMap<String, Pair<Point, Double>> currCandidate = findPointMatchCandidate(p.x(), p.y(), distance);
				if (currCandidate.size() > 0) {
					double matchDistance = Double.POSITIVE_INFINITY;
					Point nextPoint = null;
					for (Map.Entry<String, Pair<Point, Double>> end : currCandidate.entrySet()) {
						if (end.getValue()._1().equals2D(prevMatchPoint))
							continue;
						String locIndex = prevMatchPoint.x() + "_" + prevMatchPoint.y() + "," + end.getValue()._1().x() + "_" + end.getValue()._1().y();
						if (loc2RemovedWayID.containsKey(locIndex)) {
							String currRemovedID = loc2RemovedWayID.get(locIndex);
//                            LOG.info("Found removed edge: " + currRemovedID);
							RoadWay newWay = roadMapConnection(currRemovedID, currRoadIntermediatePoint, currRoad.getConfidenceScore(), prevMatchPoint,
									end.getValue()._1(), true);
							if (loc2InsertedWayDist.containsKey(locIndex)) {  // removed road already inserted, add the confidence score
								RoadWay currWay = loc2InsertedWayDist.get(locIndex)._1();
								currWay.setConfidenceScore(currWay.getConfidenceScore() + newWay.getConfidenceScore());
							} else if (currRoadIntermediatePoint.size() != 0) {
								double startDist = distFunc.distance(prevMatchPoint, currRoadIntermediatePoint.get(0).toPoint());
								double endDist =
										distFunc.distance(currRoadIntermediatePoint.get(currRoadIntermediatePoint.size() - 1).toPoint(), end.getValue()._1());
								loc2InsertedWayDist.put(locIndex, new Pair<>(newWay, (startDist + endDist) / 2));
							} else
								loc2InsertedWayDist.put(locIndex, new Pair<>(newWay, 0d));
							prevMatchPoint = end.getValue()._1();
							nextPoint = null;
							removedRoadFound = true;
							break;
						} else if (loc2RoadWayMapping.containsKey(locIndex) && end.getValue()._2() < matchDistance) {
							// refinement is possible
							matchDistance = end.getValue()._2();
							nextPoint = end.getValue()._1();
						}
					}
					if (nextPoint != null) {
						// start refinement
						prevMatchPoint = nextPoint;
						currRoadIntermediatePoint.clear();
					} else {
						currRoadIntermediatePoint.clear();
					}
				}
			}
			currRoadIntermediatePoint.add(currRoad.getNode(i + 1));
		}
		// deal the final segment
		String locIndex = prevMatchPoint.x() + "_" + prevMatchPoint.y() + "," + endPoint.x() + "_" + endPoint.y();
		if (loc2RemovedWayID.containsKey(locIndex)) {
			String currRemovedID = loc2RemovedWayID.get(locIndex);
//            LOG.info("Found removed edge: " + currRemovedID);
			RoadWay newWay = roadMapConnection(currRemovedID, currRoadIntermediatePoint, currRoad.getConfidenceScore(), prevMatchPoint,
					endPoint, true);
			if (loc2InsertedWayDist.containsKey(locIndex)) {  // removed road already inserted, add the confidence score
				RoadWay currWay = loc2InsertedWayDist.get(locIndex)._1();
				currWay.setConfidenceScore(currWay.getConfidenceScore() + newWay.getConfidenceScore());
			} else {
				double startDist = distFunc.distance(prevMatchPoint, currRoadIntermediatePoint.get(0).toPoint());
				double endDist = distFunc.distance(currRoadIntermediatePoint.get(currRoadIntermediatePoint.size() - 1).toPoint(), endPoint);
				loc2InsertedWayDist.put(locIndex, new Pair<>(newWay, (startDist + endDist) / 2));
			}
		} else if (!loc2RoadWayMapping.containsKey(locIndex) && !removedRoadFound) {
			String currNewID = (++maxAbsRoadWayID) + "";
//            LOG.info("Create new edge: " + currNewID);
			if (!prevMatchPoint.equals2D(endPoint)) {
				RoadWay newWay = roadMapConnection(currNewID, currRoadIntermediatePoint, currRoad.getConfidenceScore(), prevMatchPoint,
						endPoint, false);
				newWay.setConfidenceScore(currRoad.getConfidenceScore());
				double startDist = distFunc.distance(prevMatchPoint, currRoadIntermediatePoint.get(0).toPoint());
				double endDist = distFunc.distance(currRoadIntermediatePoint.get(currRoadIntermediatePoint.size() - 1).toPoint(), endPoint);
				loc2InsertedWayDist.put(locIndex, new Pair<>(newWay, (startDist + endDist) / 2));
			}
		}
	}
	
	private List<RoadWay> doubleDirectedRoadWayInsertion() {
		List<RoadWay> insertRoadWayList = new ArrayList<>();
		HashMap<String, RoadWay> insertRoadIDMapping = new HashMap<>();
		for (Map.Entry<String, Pair<RoadWay, Double>> entry : loc2InsertedWayDist.entrySet()) {
			RoadWay newWay = entry.getValue()._1();
			boolean isValidRoad = true;
			for (int i = 0; i < newWay.getNodes().size() - 1; i++) {
				if (distFunc.distance(newWay.getNode(i).toPoint(), newWay.getNode(i + 1).toPoint()) == 0) {
					isValidRoad = false;
					break;
				}
			}
			if (!isValidRoad)
				continue;
			if (insertRoadIDMapping.containsKey(newWay.getID())) {
				RoadWay currWay = insertRoadIDMapping.get(newWay.getID());
				currWay.setConfidenceScore(currWay.getConfidenceScore() + newWay.getConfidenceScore());
			} else {
				for (int i = 1; i < newWay.getNodes().size() - 1; i++) {
					RoadNode n = newWay.getNode(i);
					maxMiniNodeID++;
					n.setId(maxMiniNodeID + "-");
				}
				newWay.setNewRoad(true);
				if (newWay.getConfidenceScore() == 0)
					LOG.error("ERROR! New road has zero confidence score.");
				insertRoadIDMapping.put(newWay.getID(), newWay);
				insertRoadWayList.add(newWay);
			}
			String reverseID = newWay.getID().contains("-") ? newWay.getID().substring(newWay.getID().indexOf("-") + 1) : "-" + newWay.getID();
			if (insertRoadIDMapping.containsKey(reverseID)) {
				RoadWay currWay = insertRoadIDMapping.get(reverseID);
				currWay.setConfidenceScore(currWay.getConfidenceScore() + newWay.getConfidenceScore());
			} else {
				RoadWay reverseRoad = new RoadWay(reverseID, distFunc);
				reverseRoad.addNode(newWay.getToNode());
				for (int i = newWay.getNodes().size() - 2; i > 0; i--) {
					maxMiniNodeID++;
					RoadNode reverseNode = new RoadNode(maxMiniNodeID + "-", newWay.getNode(i).lon(), newWay.getNode(i).lat(), distFunc);
					reverseRoad.addNode(reverseNode);
				}
				reverseRoad.addNode(newWay.getFromNode());
				reverseRoad.setNewRoad(true);
				reverseRoad.setConfidenceScore(newWay.getConfidenceScore());
				insertRoadIDMapping.put(reverseID, reverseRoad);
				insertRoadWayList.add(reverseRoad);
			}
		}
		insertRoadWayList.removeIf(w -> this.rawMap.containsWay(w.getID()));
		return insertRoadWayList;
	}
	
	/**
	 * Find removed road among all possible candidate pairs. If no removed road found, find the road that is the closest.
	 *
	 * @param startPointMatchCandidate Start end point candidates of the inferred road.
	 * @param endPointMatchCandidate   End point candidate of the inferred road.
	 * @return The pair of candidates that form the new road. If it is an existing removed road, the road ID is assigned, otherwise empty.
	 */
	private Triplet<Point, Point, Triplet<String, Double, HashSet<String>>> findBestMatch(HashMap<String, Pair<Point, Double>> startPointMatchCandidate,
																						  HashMap<String, Pair<Point, Double>> endPointMatchCandidate) {
		Point startPoint = null;
		Point endPoint = null;
		String currRoadID = "";
		double currDistance = Double.POSITIVE_INFINITY;     // the minimum distance of all possible pairs
		double currRemovePairDistance = Double.POSITIVE_INFINITY;   // the minimum distance among all detected removed roads
		boolean containsRemovedRoad = false;    //  if a removed road is contained in the candidates, other normal roads will be ignored
		HashSet<String> generatedLocList = new HashSet<>();
		for (Map.Entry<String, Pair<Point, Double>> start : startPointMatchCandidate.entrySet()) {
			for (Map.Entry<String, Pair<Point, Double>> end : endPointMatchCandidate.entrySet()) {
				if (start.getValue()._1().equals2D(end.getValue()._1()))
					continue;
				String locIndex = start.getValue()._1().x() + "_" + start.getValue()._1().y() + "," + end.getValue()._1().x() + "_" + end.getValue()._1().y();
				if (loc2RemovedWayID.containsKey(locIndex) && (!containsRemovedRoad || (start.getValue()._2() + end.getValue()._2()) / 2 < currRemovePairDistance)) {
					// a better removed road is found if 1) in the mapping 2) either no removed road found before or closer than existing
					// candidate
					containsRemovedRoad = true;
					currRemovePairDistance = (start.getValue()._2() + end.getValue()._2()) / 2;
					currDistance = currRemovePairDistance;
					currRoadID = loc2RemovedWayID.get(locIndex);
					if (loc2InsertedWayDist.containsKey(locIndex) && (start.getValue()._2() + end.getValue()._2()) / 2 > loc2InsertedWayDist.get(locIndex)._2()) {
						generatedLocList.add(locIndex);
						startPoint = null;
						endPoint = null;
						// a better removed road already inferred, skip
						continue;
					}
					startPoint = start.getValue()._1();
					endPoint = end.getValue()._1();
//                    LOG.info("Found removed road:" + currRoadID);
				} else if (!containsRemovedRoad && (start.getValue()._2() + end.getValue()._2()) / 2 < currDistance) {
					// a regular road is selected if 1) no removed road exist 2) closer than existing candidate 3) not in the mapping
					currDistance = (start.getValue()._2() + end.getValue()._2()) / 2;
					startPoint = start.getValue()._1();
					endPoint = end.getValue()._1();
				}
			}
		}
		return new Triplet<>(startPoint, endPoint, new Triplet<>(currRoadID, currDistance, generatedLocList));
	}
	
	private HashMap<String, Pair<Point, Double>> findPointMatchCandidate(double lon, double lat, int thresholdDist) {
		HashMap<String, Pair<Point, Double>> result = new HashMap<>();
		List<GridPartition<Point>> partitionList = new ArrayList<>();
		partitionList.add(this.grid.partitionSearch(lon, lat));
		partitionList.addAll(this.grid.adjacentPartitionSearch(lon, lat));
		for (GridPartition<Point> partition : partitionList) {
			if (partition != null) {
				for (XYObject<Point> item : partition.getObjectsList()) {
					double distance = distFunc.pointToPointDistance(lon, lat, item.x(), item.y());
					if (distance < thresholdDist) {
						String loc = item.getSpatialObject().x() + "_" + item.getSpatialObject().y();
						result.put(loc, new Pair<>(item.getSpatialObject(), distance));
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Find the road segments hidden in an isolated road. Try to divide the road into multiple pieces so that some of them can be
	 * connected to the existing network. Other pieces will be waived.
	 *
	 * @param roadWay The input inferred road
	 */
	private void findSubRoadConnection(RoadWay roadWay) {
		List<RoadNode> currNodeList = new ArrayList<>();
		RoadNode startNode = roadWay.getNode(0);
		HashMap<String, Pair<Point, Double>> prevCandidateList = findPointMatchCandidate(startNode.lon(), startNode.lat(), subTrajMergeDist);
		boolean newRoadStarted = prevCandidateList.size() > 0;
		double minRoadLength = prop.getPropertyDouble("algorithm.mapmerge.MinimumRoadLength");
		for (int i = 0; i < roadWay.getNodes().size() - 1; i++) {
			List<Point> intermediatePoint = edgeSegmentation(roadWay.getNode(i), roadWay.getNode(i + 1));
			intermediatePoint.add(roadWay.getNode(i + 1).toPoint());
			for (Point p : intermediatePoint) {
				HashMap<String, Pair<Point, Double>> currCandidateList = findPointMatchCandidate(p.x(), p.y(), subTrajMergeDist);
				if (currCandidateList.size() > 0) {
					if (prevCandidateList.size() > 0) {
						Triplet<Point, Point, Triplet<String, Double, HashSet<String>>> bestMatch = findBestMatch(prevCandidateList,
								currCandidateList);
						String currID = bestMatch._3()._1();
						if (bestMatch._1() == null || distFunc.distance(bestMatch._1(), bestMatch._2()) < minRoadLength) {
							if (bestMatch._1() == null && !currID.equals("")) {
								if (!bestMatch._3()._3().isEmpty()) {
									for (String s : bestMatch._3()._3()) {
										double currConfidenceScore = loc2InsertedWayDist.get(s)._1().getConfidenceScore();
										loc2InsertedWayDist.get(s)._1().setConfidenceScore(currConfidenceScore + roadWay.getConfidenceScore());
									}
								}
							}
							continue;
						}
						String currLoc = bestMatch._1().x() + "_" + bestMatch._1().y() + "," + bestMatch._2().x() + "_" + bestMatch._2().y();
						if (loc2RoadWayMapping.containsKey(currLoc))   // matched to existing road, skip
							continue;
						if (!currID.equals("")) {   // removed road related
							RoadWay newWay = roadMapConnection(currID, currNodeList, roadWay.getConfidenceScore(), bestMatch._1(),
									bestMatch._2(), true);
							loc2InsertWayDistUpdate(bestMatch, currLoc, newWay);
						} else if (loc2InsertedWayDist.containsKey(currLoc)) { // a new road has already been inserted to the current location
							RoadWay currWay = loc2InsertedWayDist.get(currLoc)._1();
							currWay.setConfidenceScore(currWay.getConfidenceScore() + roadWay.getConfidenceScore());
						} else {    // completely new road
							currID = (++maxAbsRoadWayID) + "";
							RoadWay newWay = roadMapConnection(currID, currNodeList, roadWay.getConfidenceScore(), bestMatch._1(),
									bestMatch._2(), false);
							loc2InsertWayDistUpdate(bestMatch, currLoc, newWay);
//                                LOG.info("Create new edge: " + currID);
						}
						prevCandidateList = currCandidateList;
						currNodeList.clear();
						newRoadStarted = true;
					} else {
						prevCandidateList = currCandidateList;
						currNodeList.clear();
						newRoadStarted = true;
					}
				}
			}
			if (newRoadStarted)
				currNodeList.add(roadWay.getNode(i + 1));
		}
	}
	
	private List<Point> edgeSegmentation(RoadNode firstNode, RoadNode secondNode) {
		DecimalFormat df = new DecimalFormat("0.00000");
		double distance = distFunc.distance(firstNode.toPoint(), secondNode.toPoint());
		List<Point> nodeList = new ArrayList<>();
		if (distance > subTrajMergeDist * 2) {
			int tempPointCount = (int) Math.ceil(distance / (subTrajMergeDist * 2));
			double lonDiff = (secondNode.toPoint().x() - firstNode.toPoint().x()) / tempPointCount;
			double latDiff = (secondNode.toPoint().y() - firstNode.toPoint().y()) / tempPointCount;
			for (int i = 1; i < tempPointCount; i++) {
				Point newPoint = new Point(Double.parseDouble(df.format(firstNode.toPoint().x() + lonDiff * i)), Double.parseDouble(df.format
						(firstNode.toPoint().y() + latDiff * i)), distFunc);
				nodeList.add(newPoint);
			}
		}
		return nodeList;
	}
	
	/**
	 * Connect the current road to the existing endpoints.
	 *
	 * @param roadID          The road ID of the current road.
	 * @param nodeList        The node sequence to be connected.
	 * @param confidenceScore The confidence score of the new road.
	 * @param startPoint      The start point to be connected.
	 * @param endPoint        The end point to be connected.
	 * @param isRemovedRoad   True if the endpoints are from an removed road.
	 * @return The new road way.
	 */
	private RoadWay roadMapConnection(String roadID, List<RoadNode> nodeList, double confidenceScore, Point startPoint, Point endPoint, boolean isRemovedRoad) {
		RoadNode startNode;
		RoadNode endNode;
		if (isRemovedRoad) {
			String locIndex = startPoint.x() + "_" + startPoint.y() + "," + endPoint.x() + "_" + endPoint.y();
			if (!loc2RemovedWayMapping.containsKey(locIndex)) {
				String reverseLocIndex = endPoint.x() + "_" + endPoint.y() + "," + startPoint.x() + "_" + startPoint.y();
				if (!loc2RemovedWayMapping.containsKey(reverseLocIndex)) {
					throw new IllegalArgumentException("ERROR! Removed road way not found. " + locIndex);
				} else {
					startNode = loc2RemovedWayMapping.get(reverseLocIndex).getToNode();
					endNode = loc2RemovedWayMapping.get(reverseLocIndex).getFromNode();
				}
			} else {
				startNode = loc2RemovedWayMapping.get(locIndex).getFromNode();
				endNode = loc2RemovedWayMapping.get(locIndex).getToNode();
			}
		} else {
			String startLoc = startPoint.x() + "_" + startPoint.y();
			String endLoc = endPoint.x() + "_" + endPoint.y();
			if (!loc2RoadNodeListMapping.containsKey(startLoc) || !loc2RoadNodeListMapping.containsKey(endLoc))
				System.out.println("ERROR! Road node not found. " + startLoc + "," + endLoc);
			startNode = loc2RoadNodeListMapping.get(startLoc).get(0);
			endNode = loc2RoadNodeListMapping.get(endLoc).get(0);
		}
		List<RoadNode> refinedWay = new ArrayList<>();
		refinedWay.add(startNode);
		if (nodeList.size() > 1) {
			int i;
			for (i = 0; i < nodeList.size() - 1; i++) {
				if (distFunc.distance(nodeList.get(i).toPoint(), startPoint) < distFunc.distance(nodeList.get(i + 1).toPoint(), startPoint)) {
					refinedWay.add(nodeList.get(i));
					break;
				}
			}
			int sequenceEnd;    // decide the end of the sequence
			for (sequenceEnd = nodeList.size() - 1; sequenceEnd > i; sequenceEnd--) {
				if (distFunc.distance(nodeList.get(sequenceEnd).toPoint(), endPoint) < distFunc.distance(nodeList.get(sequenceEnd - 1).toPoint(),
						endPoint)) {
					break;
				}
			}
			// add all remaining points
			for (i += 1; i <= sequenceEnd; i++) {
				refinedWay.add(nodeList.get(i));
			}
		} else if (nodeList.size() == 1) {
			if (distFunc.distance(nodeList.get(0).toPoint(), startPoint) > subTrajMergeDist || distFunc.distance(nodeList.get(0).toPoint(), endPoint) > subTrajMergeDist) {
				refinedWay.add(nodeList.get(0));
			}
		}
		refinedWay.add(endNode);
		RoadWay resultWay = new RoadWay(roadID, refinedWay, distFunc);
		resultWay.setConfidenceScore(confidenceScore);
		return resultWay;

//        throw new IllegalArgumentException("ERROR! At least one of the end points of the inferred road is not found in the raw map.");
	}
	
	
	private void buildGridIndex() {
		// calculate the grid settings
		int rowNum;     // number of rows
		int columnNum;     // number of columns

//        Set<String> nodeLocationList = new HashSet<>();
//        for (RoadWay w : rawMap.getWays()) {
//            for (RoadNode n : w.getVertices()) {
//                if (!nodeLocationList.contains(n.lon() + "_" + n.lat())) {
//                    nodeLocationList.add(n.lon() + "_" + n.lat());
//                    nodeCount++;
//                } else {
//                    LOG.info("Duplicated road nodes in nearest neighbour network index");
//                }
//            }
//        }
		
		// calculate the total number of rows and columns. The size of each grid cell equals the candidate range
		double lonDistance = distFunc.pointToPointDistance(rawMap.getMaxLon(), 0d, rawMap.getMinLon(), 0d);
		double latDistance = distFunc.pointToPointDistance(0d, rawMap.getMaxLat(), 0d, rawMap.getMinLat());
		double gridRadius = mergeCandidateDist;
		columnNum = (int) Math.round(lonDistance / gridRadius);
		rowNum = (int) Math.round(latDistance / gridRadius);
		double lonPerCell = (rawMap.getMaxLon() - rawMap.getMinLon()) / columnNum;
		double latPerCell = (rawMap.getMaxLat() - rawMap.getMinLat()) / columnNum;
		
		// add extra grid cells around the margin to cover outside trajectory points
		this.grid = new Grid<>(columnNum + 2, rowNum + 2, rawMap.getMinLon() - lonPerCell, rawMap.getMinLat() - latPerCell, rawMap
				.getMaxLon() + lonPerCell, rawMap.getMaxLat() + latPerCell, distFunc);
		
		for (RoadNode n : rawMap.getNodes()) {
			Point nodeIndex = new Point(n.lon(), n.lat(), distFunc);
			nodeIndex.setID(n.getID());
			XYObject<Point> nodeIndexObject = new XYObject<>(nodeIndex.x(), nodeIndex.y(), nodeIndex);
			this.grid.insert(nodeIndexObject);
			String locIndex = nodeIndex.x() + "_" + nodeIndex.y();
			if (!loc2RoadNodeListMapping.containsKey(locIndex)) {
				List<RoadNode> nodeList = new ArrayList<>();
				nodeList.add(n);
				this.loc2RoadNodeListMapping.put(locIndex, nodeList);
			} else {
				this.loc2RoadNodeListMapping.get(locIndex).add(n);
			}
		}
	}

//        LOG.info("Total number of nodes in grid index:" + rawMap.getNodes().size());
//        LOG.info("The grid contains " + rowNum + " rows and columns");
}