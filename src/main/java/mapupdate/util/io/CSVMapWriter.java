package mapupdate.util.io;

import mapupdate.util.function.GreatCircleDistanceFunction;
import mapupdate.util.index.grid.Grid;
import mapupdate.util.index.grid.GridPartition;
import mapupdate.util.object.SpatialInterface;
import mapupdate.util.object.datastructure.XYObject;
import mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import mapupdate.util.object.roadnetwork.RoadNode;
import mapupdate.util.object.roadnetwork.RoadWay;
import mapupdate.util.object.spatialobject.Point;
import mapupdate.util.object.spatialobject.Segment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

import static mapupdate.Main.MIN_ROAD_LENGTH;

/**
 * Created by uqpchao on 22/05/2017.
 */
public class CSVMapWriter implements SpatialInterface {
    private final RoadNetworkGraph roadGraph;
    private final String csvMapPath;
    private final GreatCircleDistanceFunction distanceFunction = new GreatCircleDistanceFunction();


    public CSVMapWriter(RoadNetworkGraph roadNetworkGraph, String csvMapPath) {
        this.roadGraph = roadNetworkGraph;
        this.csvMapPath = csvMapPath;
    }

    /**
     * Write a road network to files
     *
     * @param percentage percentage of removed edges
     * @throws IOException Failed map writing
     */
    public void writeMap(int percentage, int iteration, boolean isTempMap) throws IOException {

        String outputMapPath;
        if (iteration == -1)
            outputMapPath = this.csvMapPath;
        else
            outputMapPath = this.csvMapPath + "map/" + iteration + "/";
        // create directories before writing
        File file = new File(outputMapPath.substring(0, outputMapPath.lastIndexOf('/')));
        if (!file.exists()) {
            if (!file.mkdirs()) throw new IOException("ERROR! Failed to create folder.");
        }
        // write vertex file
        BufferedWriter bwVertices;
        if (isTempMap)
            bwVertices = new BufferedWriter(new FileWriter(outputMapPath + "temp_vertices_" + percentage + ".txt"));
        else bwVertices = new BufferedWriter(new FileWriter(outputMapPath + "vertices_" + percentage + ".txt"));
        for (RoadNode n : roadGraph.getNodes())
            bwVertices.write(n.toString() + "\n");
        bwVertices.close();

        // write road way file
        BufferedWriter bwEdges;
        if (isTempMap)
            bwEdges = new BufferedWriter(new FileWriter(outputMapPath + "temp_edges_" + percentage + ".txt"));
        else bwEdges = new BufferedWriter(new FileWriter(outputMapPath + "edges_" + percentage + ".txt"));
        for (RoadWay w : roadGraph.getWays())
            bwEdges.write(w.toString() + "\n");
        bwEdges.close();
//        System.out.println("Write " + percentage + "% road map finished.");
    }

    public void randomRoadRemoval(int percentage) throws IOException {

        DecimalFormat df = new DecimalFormat("0.00000");

        if (percentage == 0)
            return;

        // create directories before writing
        File file = new File(csvMapPath.substring(0, csvMapPath.lastIndexOf('/')));
        if (!file.exists()) {
            if (!file.mkdirs()) throw new IOException("ERROR! Failed to create folder.");
        }

        HashMap<String, Integer> nodeRemovalCount = new HashMap<>();   // for each vertex, the total count of its edges that are removed
        // write road way file
        BufferedWriter bwEdges = new BufferedWriter(new FileWriter(csvMapPath + "edges_" + percentage + ".txt"));
        BufferedWriter bwRemovedEdges = new BufferedWriter(new FileWriter(csvMapPath + "removedEdges_" + percentage + ".txt"));
        int roadRemovalCount = 0;
        int vertexRemovalCount = 0;
        Random random = new Random(1);
        for (RoadWay w : roadGraph.getWays()) {
            if (random.nextInt(100) >= percentage) {
                bwEdges.write(w.getID() + "|");
                if (w.isNewRoad())
                    bwEdges.write(w.getInfluenceScore() + "|" + w.getConfidenceScore());
                else bwEdges.write("null");
                for (RoadNode n : w.getNodes()) {
                    bwEdges.write("|" + n.getID() + "," + df.format(n.lon()) + "," + df.format(n.lat()));
                }
                bwEdges.write("\n");
            } else {
                bwRemovedEdges.write(w.getID() + "|");
                if (w.isNewRoad())
                    bwRemovedEdges.write(w.getInfluenceScore() + "," + w.getConfidenceScore());
                else bwRemovedEdges.write("null");
                for (RoadNode n : w.getNodes()) {
                    bwRemovedEdges.write("|" + n.getID() + "," + df.format(n.lon()) + "," + df.format(n.lat()));
                }
                bwRemovedEdges.write("\n");

                // remove one road way from the corresponding end points
                if (nodeRemovalCount.containsKey(w.getFromNode().getID())) {
                    nodeRemovalCount.replace(w.getFromNode().getID(), nodeRemovalCount.get(w.getFromNode().getID()) + 1);
                } else nodeRemovalCount.put(w.getFromNode().getID(), 1);
                if (nodeRemovalCount.containsKey(w.getToNode().getID())) {
                    nodeRemovalCount.replace(w.getToNode().getID(), nodeRemovalCount.get(w.getToNode().getID()) + 1);
                } else nodeRemovalCount.put(w.getToNode().getID(), 1);
                roadRemovalCount++;
            }
        }
        bwEdges.close();
        bwRemovedEdges.close();
        // write vertex file
        BufferedWriter bwVertices = new BufferedWriter(new FileWriter(csvMapPath + "vertices_" + percentage + ".txt"));
        for (RoadNode n : roadGraph.getNodes()) {
            if (!nodeRemovalCount.containsKey(n.getID()) || nodeRemovalCount.get(n.getID()) != n.getDegree()) {
                bwVertices.write(n.getID() + "," + df.format(n.lon()) + "," + df.format(n.lat()) + "\n");
            } else {
                vertexRemovalCount++;
            }
        }

        System.out.println("Random road Removal done. Total removed roads: " + roadRemovalCount + ", total removed nodes:" + vertexRemovalCount);
        bwVertices.close();
    }

    /**
     * Remove the edges from fully enclosed roads and separated double direction roads, both directions are guaranteed to be removed.
     * The removed roads should be long enough.
     *
     * @param percentage The removal percentage
     * @throws IOException File operation exception
     */
    public void popularityBasedRoadRemoval(int percentage, int distance) throws IOException {

        // build index for neighbour search
        Grid<Point> gridIndex = buildGridIndexForCenterPoints(distance);
        if (percentage == 0) {
            System.out.println("WARNING! The required remove road ratio is " + percentage + ", the removal is not needed.");
            return;
        }

        // create directories before writing
        File file = new File(csvMapPath.substring(0, csvMapPath.lastIndexOf('/')));
        if (!file.exists()) {
            if (!file.mkdirs()) throw new IOException("ERROR! Failed to create folder.");
        }
        // write road way file
        BufferedWriter bwVertices = new BufferedWriter(new FileWriter(csvMapPath + "vertices_" + percentage + ".txt"));
        BufferedWriter bwEdges = new BufferedWriter(new FileWriter(csvMapPath + "edges_" + percentage + ".txt"));
        BufferedWriter bwRemovedEdges = new BufferedWriter(new FileWriter(csvMapPath + "removedEdges_" + percentage + ".txt"));

        List<RoadWay> wayList = roadGraph.getWays();
        Set<String> removedEdgeIDSet = new LinkedHashSet<>();   // set of removed road ID
        Random random = new Random(10);
        List<RoadWay> removedWayList = new ArrayList<>();
        List<RoadWay> satisfiedRoadList = new ArrayList<>();    // list of road ways that satisfy the conditions
        HashMap<String, Integer> id2NodeDegreeMapping = new HashMap<>();
        Map<String, RoadWay> id2RoadWay = new HashMap<>();
        for (RoadWay w : wayList) {
            if (isSatisfiedRoad(w, distance, gridIndex)) {
                satisfiedRoadList.add(w);
                id2RoadWay.put(w.getID(), w);
            }
        }
        int satisfiedRoadCount = satisfiedRoadList.size();
        if (satisfiedRoadList.size() * 100 / (double) wayList.size() < percentage)
            throw new IllegalArgumentException("ERROR! The number of satisfied roads " + satisfiedRoadList.size() + " is less than the " +
                    "required road removal " + wayList.size() * percentage / 100 + ". Consider loose the condition or decrease the " +
                    "removal percentage.");
        while (removedWayList.size() * 100 / (double) wayList.size() < percentage) {
            int currIndex = random.nextInt(satisfiedRoadList.size());
            if (removedEdgeIDSet.contains(satisfiedRoadList.get(currIndex).getID()))
                continue;
            List<RoadWay> tempRemovedWayList = new ArrayList<>();
            tempRemovedWayList.add(satisfiedRoadList.get(currIndex));
            // put the reversed direction road to the removed road list
            if (satisfiedRoadList.get(currIndex).getID().contains("-")) {
                String reversedRoadID = satisfiedRoadList.get(currIndex).getID().substring(1);
                if (id2RoadWay.containsKey(reversedRoadID))
                    if (!removedEdgeIDSet.contains(reversedRoadID)) {
                        tempRemovedWayList.add(id2RoadWay.get(reversedRoadID));
                    }
            } else {
                String reversedRoadID = "-" + satisfiedRoadList.get(currIndex).getID();
                if (id2RoadWay.containsKey(reversedRoadID))
                    if (!removedEdgeIDSet.contains(reversedRoadID)) {
                        tempRemovedWayList.add(id2RoadWay.get(reversedRoadID));
                    }
            }
            // avoid road node removal
            if (tempRemovedWayList.get(0).getFromNode().getDegree() <= tempRemovedWayList.size() || tempRemovedWayList.get(0).getToNode().getDegree() <= tempRemovedWayList.size())
                continue;
            if (id2NodeDegreeMapping.containsKey(tempRemovedWayList.get(0).getFromNode().getID())) {
                if (id2NodeDegreeMapping.get(tempRemovedWayList.get(0).getFromNode().getID()) <= tempRemovedWayList.size())
                    continue;
                else id2NodeDegreeMapping.replace(tempRemovedWayList.get(0).getFromNode().getID(),
                        id2NodeDegreeMapping.get(tempRemovedWayList.get(0).getFromNode().getID()) - tempRemovedWayList.size());
            } else {
                id2NodeDegreeMapping.put(tempRemovedWayList.get(0).getFromNode().getID(),
                        tempRemovedWayList.get(0).getFromNode().getDegree() - tempRemovedWayList.size());
            }
            if (id2NodeDegreeMapping.containsKey(tempRemovedWayList.get(0).getToNode().getID())) {
                if (id2NodeDegreeMapping.get(tempRemovedWayList.get(0).getToNode().getID()) <= tempRemovedWayList.size())
                    continue;
                else id2NodeDegreeMapping.replace(tempRemovedWayList.get(0).getToNode().getID(),
                        id2NodeDegreeMapping.get(tempRemovedWayList.get(0).getToNode().getID()) - tempRemovedWayList.size());
            } else {
                id2NodeDegreeMapping.put(tempRemovedWayList.get(0).getToNode().getID(),
                        tempRemovedWayList.get(0).getToNode().getDegree() - tempRemovedWayList.size());
            }

            for (RoadWay w : tempRemovedWayList) {
                removedEdgeIDSet.add(w.getID());
                removedWayList.add(w);
            }

        }

        List<RoadNode> nodeList = this.roadGraph.getNodes();
        wayList.removeAll(removedWayList);
        RoadNetworkGraph newGraph = new RoadNetworkGraph();
        newGraph.setNodes(nodeList);
        newGraph.addWays(wayList);
        int removedNodeCount = newGraph.isolatedNodeRemoval();
        if (removedNodeCount != 0)
            throw new IllegalStateException("ERROR! The removed node should be zero: " + removedNodeCount);

        // write result to the files
        for (RoadNode n : newGraph.getNodes())
            bwVertices.write(n.toString() + "\n");
        for (RoadWay w : newGraph.getWays())
            bwEdges.write(w.toString() + "\n");
        for (RoadWay w : removedWayList)
            bwRemovedEdges.write(w.toString() + "\n");
        bwVertices.close();
        bwEdges.close();
        bwRemovedEdges.close();

        System.out.println("Random road Removal done. Total number of satisfied roads: " + satisfiedRoadCount + ", total removed " +
                "roads: " + removedWayList.size() + ".");
    }

    /**
     * Generate grid index for the virtual center point of each roadway, which is used for road removal selection.
     *
     * @param distance Grid index cell radius.
     * @return Grid index
     */
    private Grid<Point> buildGridIndexForCenterPoints(int distance) {
        // calculate the grid settings
        int rowNum;     // number of rows
        int columnNum;     // number of columns
        Set<String> locSet = new HashSet<>();  // ensure every inserted node is unique
        if (roadGraph.getNodes().isEmpty())
            throw new IllegalStateException("Cannot create location index of empty graph!");

        // calculate the total number of rows and columns. The size of each grid cell equals the candidate range
        double lonDistance = distanceFunction.pointToPointDistance(roadGraph.getMaxLon(), 0d, roadGraph.getMinLon(), 0d);
        double latDistance = distanceFunction.pointToPointDistance(0d, roadGraph.getMaxLat(), 0d, roadGraph.getMinLat());
        columnNum = (int) Math.round(lonDistance / distance);
        rowNum = (int) Math.round(latDistance / distance);
        double lonPerCell = (roadGraph.getMaxLon() - roadGraph.getMinLon()) / columnNum;
        double latPerCell = (roadGraph.getMaxLat() - roadGraph.getMinLat()) / columnNum;

        // add extra grid cells around the margin to cover outside trajectory points
        Grid<Point> grid = new Grid<>(columnNum + 2, rowNum + 2, roadGraph.getMinLon() - lonPerCell, roadGraph.getMinLat() -
                latPerCell, roadGraph.getMaxLon() + lonPerCell, roadGraph.getMaxLat() + latPerCell);

//        System.out.println("The grid contains " + (rowNum + 2) + " rows and " + (columnNum + 2) + " columns");

        int pointCount = 0;

        for (RoadWay t : roadGraph.getWays()) {
            for (Segment s : t.getEdges()) {
                Point centerPoint = new Point((s.x1() + s.x2()) / 2, (s.y1() + s.y2()) / 2);
                centerPoint.setId(t.getID().replaceAll("-", ""));
                if (!locSet.contains(centerPoint.x() + "_" + centerPoint.y())) {
                    XYObject<Point> centerIndex = new XYObject<>(centerPoint.x(), centerPoint.y(), centerPoint);
                    grid.insert(centerIndex);
                    locSet.add(centerPoint.x() + "_" + centerPoint.y());
                    pointCount++;
                }
            }
        }

//        System.out.println("Grid index build successfully, total number of segment center points in grid index: " + pointCount + ", ");
        return grid;
    }

    private boolean isSatisfiedRoad(RoadWay w, int distance, Grid<Point> gridIndex) {
//        if (w.getRoadWayLevel() <= 4 || w.getRoadWayLevel() == 9)
//            return false;
//        if (!w.getRoadWayType().get(1) && !w.getRoadWayType().get(2) && !w.getRoadWayType().get(21))
//            return false;
        if (w.getRoadLength() < MIN_ROAD_LENGTH || w.getVisitCount() < 5)
            return false;
        for (Segment s : w.getEdges()) {
            Point centerPoint = new Point((s.x1() + s.x2()) / 2, (s.y1() + s.y2()) / 2);

            // find all grid partitions that are close to the given point
            List<GridPartition<Point>> partitionList = new ArrayList<>();
            partitionList.add(gridIndex.partitionSearch(centerPoint.x(), centerPoint.y()));
            partitionList.addAll(gridIndex.adjacentPartitionSearch(centerPoint.x(), centerPoint.y()));
            for (GridPartition<Point> partition : partitionList) {
                if (partition != null)
                    for (XYObject<Point> item : partition.getObjectsList()) {
                        Point candidatePoint = item.getSpatialObject();
                        // if a point is found close enough to the given road, stop the search and return unsatisfied
                        if (Math.abs(Long.parseLong(candidatePoint.getId())) != Math.abs(Long.parseLong(w.getID())) && distanceFunction
                                .distance(centerPoint, candidatePoint) < distance)
                            return false;
                    }
            }
        }
        return true;
    }
}