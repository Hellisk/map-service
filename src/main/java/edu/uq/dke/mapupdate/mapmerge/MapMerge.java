package edu.uq.dke.mapupdate.mapmerge;

import edu.uq.dke.mapupdate.util.function.GreatCircleDistanceFunction;
import edu.uq.dke.mapupdate.util.index.grid.Grid;
import edu.uq.dke.mapupdate.util.index.grid.GridPartition;
import edu.uq.dke.mapupdate.util.object.datastructure.*;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;
import edu.uq.dke.mapupdate.util.object.spatialobject.Point;

import java.text.DecimalFormat;
import java.util.*;

import static edu.uq.dke.mapupdate.Main.MIN_ROAD_LENGTH;

public class MapMerge {
    private RoadNetworkGraph rawMap;
    private List<RoadWay> inferredWayList;
    private Map<String, String> loc2RemovedWayID = new HashMap<>();
    private Map<String, RoadWay> loc2RoadWayMapping = new HashMap<>();
    private Map<Point, RoadNode> point2RoadNodeMapping = new HashMap<>();
    private Map<String, List<RoadWay>> tempPoint2EdgeIndexMapping = new LinkedHashMap<>();
    private GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
    private Grid<Point> grid;
    private int mergeCandidateDistance;    // the maximum distance that an intersection can be considered as the candidate of a
    // merge
    private int subTrajectoryMergeDistance = 10;    //
    // the following parameters are used to assign road number for new roads and nodes.
    private long maxAbsRoadWayID;
    private long maxMiniNodeID;

    public MapMerge(RoadNetworkGraph rawMap, List<RoadWay> inferredWayList, List<RoadWay> removedWayList, int
            mergeCandidateDistance, int subTrajectoryMergeDistance) {
        this.rawMap = rawMap;
        for (RoadWay w : rawMap.getWays())
            w.setNewRoad(false);
        this.inferredWayList = roadConjunction(inferredWayList);
        for (RoadWay w : removedWayList)
            this.loc2RemovedWayID.put(w.getFromNode().lon() + "_" + w.getFromNode().lat() + "," + w.getToNode().lon() + "_" + w.getToNode()
                    .lat(), w.getID());
        if (mergeCandidateDistance > 0)
            this.mergeCandidateDistance = mergeCandidateDistance;
        else
            System.out.println("ERROR! The merge candidate search range is illegal: " + mergeCandidateDistance);
        if (subTrajectoryMergeDistance > 0)
            this.subTrajectoryMergeDistance = subTrajectoryMergeDistance;
        else
            System.out.println("ERROR! The sub-trajectory merge candidate search range is illegal: " + subTrajectoryMergeDistance);
        for (RoadWay w : rawMap.getWays())
            loc2RoadWayMapping.put(w.getFromNode().lon() + "_" + w.getFromNode().lat() + "," + w.getToNode().lon() + "_" + w.getToNode()
                    .lat(), w);
        this.maxAbsRoadWayID = rawMap.getMaxAbsWayID();
        this.maxMiniNodeID = rawMap.getMaxMiniNodeID();
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

//        System.out.println("Total number of inferred road is: " + inferredWayList.size() + ", number of additional nodes is: " + tempPoint2EdgeIndexMapping.entrySet().size());

        List<RoadWay> roadWayResult = new ArrayList<>();
        for (RoadWay w : inferredWayList) {
            if (w.getVisitCount() != 1) {
                w.setVisitCount(1);
                RoadWay extendedWay = roadExtension(w, w.getToNode().toPoint(), true);
                extendedWay = roadExtension(extendedWay, w.getFromNode().toPoint(), false);
                if (extendedWay.getRoadLength() > MIN_ROAD_LENGTH / 2)
                    roadWayResult.add(extendedWay);
            }
        }

        for (Map.Entry<String, List<RoadWay>> entry : tempPoint2EdgeIndexMapping.entrySet()) {
            if (entry.getValue().size() != 0)
                System.out.println("ERROR! Unvisited road remaining.");
        }
//        System.out.println("Total number of roads after conjunction: " + roadWayResult.size());
        return roadWayResult;
    }

    private boolean insertWay2Index(RoadWay w, String locIndex) {
        if (!tempPoint2EdgeIndexMapping.containsKey(locIndex)) {
            List<RoadWay> currWayList = new ArrayList<>();
            currWayList.add(w);
            tempPoint2EdgeIndexMapping.put(locIndex, currWayList);
            return true;
        } else {
            List<RoadWay> wayList = tempPoint2EdgeIndexMapping.get(locIndex);
            String tempIndex = w.getFromNode().lon() + "_" + w.getFromNode().lat();
            String actualLocIndex = tempIndex.equals(locIndex) ? w.getToNode().lon() + "_" + w.getToNode().lat() : tempIndex;   // the
            // index of the other endpoint of inserting road
            for (RoadWay currWay : wayList) {
                String currTempIndex = currWay.getFromNode().lon() + "_" + currWay.getFromNode().lat();
                String actualCurrLocIndex = currTempIndex.equals(locIndex) ? currWay.getToNode().lon() + "_" + currWay.getToNode().lat()
                        : currTempIndex;
                if (actualLocIndex.equals(actualCurrLocIndex))
                    return false;
            }
            wayList.add(w);
            return true;
        }
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
            System.out.println("ERROR! The merging road ways are not connected.");

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
        RoadWay mergeResult = new RoadWay(currWay.getID(), mergeRoadNodeList);
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

    public Pair<RoadNetworkGraph, Boolean> nearestNeighbourMapMerge() {
        buildGridIndex();
        int prevWayListSize = rawMap.getWays().size();  // for future new road ID assignment

        for (RoadWay w : inferredWayList) {
            // find an possible intersection pair which the edge can be added to, prioritize the intersection pairs of the removed edge.
//            System.out.println("start current road way connection, road length:" + w.getRoadLength());
            List<Pair<Point, Double>> startPointMatchCandidate = findPointMatchCandidate(w.getFromNode().lon(), w.getFromNode().lat(),
                    mergeCandidateDistance, distFunc);
            List<Pair<Point, Double>> endPointMatchCandidate = findPointMatchCandidate(w.getToNode().lon(), w.getToNode().lat(),
                    mergeCandidateDistance, distFunc);
            if (startPointMatchCandidate.size() > 0 && endPointMatchCandidate.size() > 0) {
                Point startPoint;
                Point endPoint;
                String currRoadID = "";
                Triplet<Point, Point, String> bestMatch = findBestMatch(startPointMatchCandidate, endPointMatchCandidate);
                startPoint = bestMatch._1();
                endPoint = bestMatch._2();
                currRoadID = bestMatch._3();

                if (startPoint != null) {
                    // at least one pair of intersections is found
//                    System.out.println("Both endpoints can be matched to the map");
                    if (!currRoadID.equals("")) {
                        // the inferred road has been assigned to a removed road
                        RoadWay newWay = roadMapConnection(w, startPoint, endPoint);
                        doubleDirectedRoadWayInsertion(newWay, currRoadID, w.getConfidenceScore());
                    } else {
                        // the inferred road is assigned to a new road, check whether the new road cover any existing removed road
                        roadRefinement(w, startPoint, endPoint, mergeCandidateDistance);
                    }
                } else {
//                    System.out.println("The current endpoints match is absurd: The start point and end point is the same.");
                    findSubRoadConnection(w);   // find sub-trajectories that can be connected to the existing road ways
                }
            } else {
//                System.out.println("Not all endpoint can be matched to the map, start subRoadConnection");
                findSubRoadConnection(w);   // find sub-trajectories that can be connected to the existing road ways
            }
        }
        System.out.println("Nearest neighbour map merge completed. Total number of road way added:" + (rawMap.getWays().size() - prevWayListSize));
        return new Pair<>(rawMap, (rawMap.getWays().size() - prevWayListSize) == 0);
    }

    private void roadRefinement(RoadWay currRoad, Point startPoint, Point endPoint, int distance) {
        List<RoadNode> currRoadIntermediatePoint = new ArrayList<>();
        Point prevMatchPoint = startPoint;
        boolean removedRoadFound = false;
        for (int i = 0; i < currRoad.getNodes().size() - 1; i++) {
            List<Point> intermediatePoint = edgeSegmentation(currRoad.getNode(i), currRoad.getNode(i + 1));
            for (Point p : intermediatePoint) {
                List<Pair<Point, Double>> currCandidateList = findPointMatchCandidate(p.x(), p.y(), distance, distFunc);
                if (currCandidateList.size() > 0) {
                    double matchDistance = Double.POSITIVE_INFINITY;
                    Point nextPoint = null;
                    for (Pair<Point, Double> end : currCandidateList) {
                        if (end._1().equals2D(prevMatchPoint))
                            continue;
                        String locIndex = prevMatchPoint.x() + "_" + prevMatchPoint.y() + "," + end._1().x() + "_" + end._1().y();
                        if (loc2RemovedWayID.containsKey(locIndex)) {
                            String currRemovedID = loc2RemovedWayID.get(locIndex);
//                            System.out.println("Found removed edge: " + currRemovedID);
                            List<RoadNode> roadNodeList = new ArrayList<>();
                            if (!point2RoadNodeMapping.containsKey(prevMatchPoint) || !point2RoadNodeMapping.containsKey(end._1()))
                                System.out.println("ERROR! The matched point cannot be found in the road node list.");
                            roadNodeList.add(point2RoadNodeMapping.get(prevMatchPoint));
                            roadNodeList.addAll(currRoadIntermediatePoint);
                            roadNodeList.add(point2RoadNodeMapping.get(end._1()));
                            RoadWay newWay = new RoadWay(currRemovedID, roadNodeList);
                            doubleDirectedRoadWayInsertion(newWay, currRemovedID, currRoad.getConfidenceScore());
                            prevMatchPoint = end._1();
                            nextPoint = null;
                            removedRoadFound = true;
                            break;
                        } else if (loc2RoadWayMapping.containsKey(locIndex) && end._2() < matchDistance) {
                            // refinement is possible
                            matchDistance = end._2();
                            nextPoint = end._1();
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
//            System.out.println("Found removed edge: " + currRemovedID);
            List<RoadNode> roadNodeList = new ArrayList<>();
            roadNodeList.add(point2RoadNodeMapping.get(prevMatchPoint));
            roadNodeList.addAll(currRoadIntermediatePoint);
            roadNodeList.add(point2RoadNodeMapping.get(endPoint));
            RoadWay newWay = new RoadWay(currRemovedID, roadNodeList);
            doubleDirectedRoadWayInsertion(newWay, currRemovedID, currRoad.getConfidenceScore());
        } else if (!loc2RoadWayMapping.containsKey(locIndex) && !removedRoadFound) {
            String currNewID = (++maxAbsRoadWayID) + "";
//            System.out.println("Create new edge: " + currNewID);
            List<RoadNode> roadNodeList = new ArrayList<>();
            roadNodeList.add(point2RoadNodeMapping.get(prevMatchPoint));
            roadNodeList.addAll(currRoadIntermediatePoint);
            roadNodeList.add(point2RoadNodeMapping.get(endPoint));
            RoadWay newWay = new RoadWay(currNewID, roadNodeList);
            doubleDirectedRoadWayInsertion(newWay, currNewID, currRoad.getConfidenceScore());
        }
    }

    private void doubleDirectedRoadWayInsertion(RoadWay newWay, String currRoadID, double confidenceScore) {
        newWay.setId(currRoadID);
        for (int i = 1; i < newWay.getNodes().size() - 1; i++) {
            RoadNode n = newWay.getNode(i);
            maxMiniNodeID++;
            n.setId(maxMiniNodeID + "-");
        }
        newWay.setConfidenceScore(confidenceScore);
        newWay.setNewRoad(true);
        rawMap.addWay(newWay);
        RoadWay reverseRoad = new RoadWay(currRoadID.contains("-") ? currRoadID.substring(currRoadID.indexOf("-") + 1) : "-" +
                currRoadID);
        reverseRoad.addNode(newWay.getToNode());
        for (int i = newWay.getNodes().size() - 2; i > 0; i--) {
            maxMiniNodeID++;
            RoadNode reverseNode = new RoadNode(maxMiniNodeID + "-", newWay.getNode(i).lon(), newWay.getNode(i).lat());
            reverseRoad.addNode(reverseNode);
        }
        reverseRoad.addNode(newWay.getFromNode());
        reverseRoad.setNewRoad(true);
        reverseRoad.setConfidenceScore(confidenceScore);
        rawMap.addWay(reverseRoad);
    }

    private Triplet<Point, Point, String> findBestMatch
            (List<Pair<Point, Double>> startPointMatchCandidate, List<Pair<Point, Double>> endPointMatchCandidate) {
        Point startPoint = null;
        Point endPoint = null;
        String currRoadID = "";
        double currDistance = Double.POSITIVE_INFINITY;
        boolean containsRemovedRoad = false;    //  if a removed road is contained in the candidates, other normal roads will be ignored
        for (Pair<Point, Double> start : startPointMatchCandidate) {
            for (Pair<Point, Double> end : endPointMatchCandidate) {
                if (start._1().equals2D(end._1()))
                    continue;
                String locIndex = start._1().x() + "_" + start._1().y() + "," + end._1().x() + "_" + end._1().y();
                if (loc2RemovedWayID.containsKey(locIndex) && (!containsRemovedRoad || (start._2() + end._2()) / 2 < currDistance)) {
                    // a better removed road is found if 1) in the mapping 2) either no removed road found before or closer than existing
                    // candidate
                    currDistance = (start._2() + end._2()) / 2;
                    startPoint = start._1();
                    endPoint = end._1();
                    currRoadID = loc2RemovedWayID.get(locIndex);
//                    System.out.println("Found removed road:" + currRoadID);
                    containsRemovedRoad = true;
                } else if (!containsRemovedRoad && (start._2() + end._2()) / 2 < currDistance) {
                    // a regular road is selected if 1) no removed road exist 2) closer than existing candidate 3) not in the mapping
                    currDistance = (start._2() + end._2()) / 2;
                    startPoint = start._1();
                    endPoint = end._1();
                }
            }
        }
        return new Triplet<>(startPoint, endPoint, currRoadID);
    }

    private List<Pair<Point, Double>> findPointMatchCandidate(double lon, double lat, int thresholdDist, GreatCircleDistanceFunction
            distFunc) {
        List<Pair<Point, Double>> result = new ArrayList<>();
        List<GridPartition<Point>> partitionList = new ArrayList<>();
        partitionList.add(this.grid.partitionSearch(lon, lat));
        partitionList.addAll(this.grid.adjacentPartitionSearch(lon, lat));
        for (GridPartition<Point> partition : partitionList) {
            if (partition != null) {
                for (XYObject<Point> item : partition.getObjectsList()) {
                    double distance = distFunc.pointToPointDistance(lon, lat, item.x(), item.y());
                    if (distance < thresholdDist)
                        result.add(new Pair<>(item.getSpatialObject(), distance));
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
        List<Pair<Point, Double>> prevCandidateList = findPointMatchCandidate(startNode.lon(), startNode.lat(), subTrajectoryMergeDistance,
                distFunc);
        boolean newRoadStarted = prevCandidateList.size() > 0;
        for (int i = 0; i < roadWay.getNodes().size() - 1; i++) {
            List<Point> intermediatePoint = edgeSegmentation(roadWay.getNode(i), roadWay.getNode(i + 1));
            intermediatePoint.add(roadWay.getNode(i + 1).toPoint());
            for (Point p : intermediatePoint) {
                List<Pair<Point, Double>> currCandidateList = findPointMatchCandidate(p.x(), p.y(), subTrajectoryMergeDistance, distFunc);
                if (currCandidateList.size() > 0) {
                    if (prevCandidateList.size() > 0) {
                        Triplet<Point, Point, String> bestMatch = findBestMatch(prevCandidateList, currCandidateList);
                        if (bestMatch._1() == null || distFunc.distance(bestMatch._1(), bestMatch._2()) < MIN_ROAD_LENGTH)
                            continue;
                        String locIndex = bestMatch._1().x() + "_" + bestMatch._1().y() + "," + bestMatch._2().x() + "_" + bestMatch._2().y();
                        if (loc2RemovedWayID.containsKey(locIndex) || distFunc.distance(bestMatch._1(), bestMatch._2()) > MIN_ROAD_LENGTH) {
                            // a removed road found or an inferred road is long enough
                            String currID = bestMatch._3();
                            if (loc2RoadWayMapping.containsKey(locIndex)) {
                                currID = loc2RoadWayMapping.get(locIndex).getID();
//                                System.out.println("Found existing edge: " + loc2RoadWayMapping.get(locIndex));
                            } else if (!loc2RemovedWayID.containsKey(locIndex)) {
                                currID = (++maxAbsRoadWayID) + "";
//                                System.out.println("Found new edge: " + currID);
                            }
                            List<RoadNode> roadNodeList = new ArrayList<>();
                            roadNodeList.add(point2RoadNodeMapping.get(bestMatch._1()));
                            roadNodeList.addAll(currNodeList);
                            roadNodeList.add(point2RoadNodeMapping.get(bestMatch._2()));
                            RoadWay newWay = new RoadWay(currID, roadNodeList);
                            doubleDirectedRoadWayInsertion(newWay, currID, roadWay.getConfidenceScore());
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
        if (distance > subTrajectoryMergeDistance * 2) {
            int tempPointCount = (int) Math.ceil(distance / (subTrajectoryMergeDistance * 2));
            double lonDiff = (secondNode.toPoint().x() - firstNode.toPoint().x()) / tempPointCount;
            double latDiff = (secondNode.toPoint().y() - firstNode.toPoint().y()) / tempPointCount;
            for (int i = 1; i < tempPointCount; i++) {
                Point newPoint = new Point(Double.parseDouble(df.format(firstNode.toPoint().x() + lonDiff * i)), Double.parseDouble(df.format
                        (firstNode.toPoint().y() + latDiff * i)));
                nodeList.add(newPoint);
            }
        }
        return nodeList;
    }


    private RoadWay roadMapConnection(RoadWay candidateRoadWay, Point startPoint, Point endPoint) {
        if (point2RoadNodeMapping.containsKey(startPoint) && point2RoadNodeMapping.containsKey(endPoint)) {
            List<RoadNode> refinedWay = new ArrayList<>();
            refinedWay.add(point2RoadNodeMapping.get(startPoint));
            if (distFunc.distance(candidateRoadWay.getFromNode().toPoint(), startPoint) > subTrajectoryMergeDistance) {
                // the start point of the inferred edge is not that close to the connected start point, add it anyway
                refinedWay.add(candidateRoadWay.getFromNode());
            }
            // add the intermediate points
            for (int i = 1; i < candidateRoadWay.size() - 1; i++) {
                refinedWay.add(candidateRoadWay.getNode(i));
            }
            if (distFunc.distance(candidateRoadWay.getToNode().toPoint(), endPoint) > subTrajectoryMergeDistance) {
                // the start point of the inferred edge is not that close to the connected start point, add it anyway
                refinedWay.add(candidateRoadWay.getToNode());
            }
            refinedWay.add(point2RoadNodeMapping.get(endPoint));
            return new RoadWay(candidateRoadWay.getID(), refinedWay);
        } else
            throw new IllegalArgumentException("ERROR! At least one of the end points of the inferred road is not found in the raw map.");
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
//                    System.out.println("Duplicated road nodes in nearest neighbour network index");
//                }
//            }
//        }

        // calculate the total number of rows and columns. The size of each grid cell equals the candidate range
        double lonDistance = distFunc.pointToPointDistance(rawMap.getMaxLon(), 0d, rawMap.getMinLon(), 0d);
        double latDistance = distFunc.pointToPointDistance(0d, rawMap.getMaxLat(), 0d, rawMap.getMinLat());
        double gridRadius = mergeCandidateDistance;
        columnNum = (int) Math.round(lonDistance / gridRadius);
        rowNum = (int) Math.round(latDistance / gridRadius);
        double lonPerCell = (rawMap.getMaxLon() - rawMap.getMinLon()) / columnNum;
        double latPerCell = (rawMap.getMaxLat() - rawMap.getMinLat()) / columnNum;

        // add extra grid cells around the margin to cover outside trajectory points
        this.grid = new Grid<>(columnNum + 2, rowNum + 2, rawMap.getMinLon() - lonPerCell, rawMap.getMinLat() - latPerCell, rawMap
                .getMaxLon() + lonPerCell, rawMap.getMaxLat() + latPerCell);

        for (RoadNode n : rawMap.getNodes()) {
            Point nodeIndex = new Point(n.lon(), n.lat());
            nodeIndex.setId(n.getID());
            XYObject<Point> nodeIndexObject = new XYObject<>(nodeIndex.x(), nodeIndex.y(), nodeIndex);
            this.grid.insert(nodeIndexObject);
            this.point2RoadNodeMapping.put(nodeIndex, n);
        }

//        System.out.println("Total number of nodes in grid index:" + rawMap.getNodes().size());
//        System.out.println("The grid contains " + rowNum + " rows and columns");
    }
}