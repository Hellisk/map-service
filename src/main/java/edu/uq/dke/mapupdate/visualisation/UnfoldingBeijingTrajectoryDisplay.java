package edu.uq.dke.mapupdate.visualisation;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.utils.MapUtils;
import edu.uq.dke.mapupdate.util.io.CSVMapReader;
import edu.uq.dke.mapupdate.util.io.CSVTrajectoryReader;
import edu.uq.dke.mapupdate.util.object.datastructure.Pair;
import edu.uq.dke.mapupdate.util.object.datastructure.TrajectoryMatchingResult;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;
import edu.uq.dke.mapupdate.util.object.spatialobject.STPoint;
import edu.uq.dke.mapupdate.util.object.spatialobject.Trajectory;
import processing.core.PApplet;

import java.io.IOException;
import java.util.*;

import static edu.uq.dke.mapupdate.Main.*;

/**
 * Simple Unfolding map app demo.
 *
 * @author uqdalves
 */
//http://unfoldingmaps.org/
public class UnfoldingBeijingTrajectoryDisplay extends PApplet {

    private UnfoldingMap currTrajDisplay;
    private UnfoldingMap fullMapDisplay;    // full map visualization
    private UnfoldingMap[] trajDisplay = new UnfoldingMap[7];   // 0,1,2 = matching result comparison for three different trajectories,
    // 3,4,5,6 = unmatched trajectory,initial inference road, merged road, refined road comparison

    public void setup() {
        size(1760, 990, JAVA2D);
        this.fullMapDisplay = new UnfoldingMap(this, new BlankMap.BlankProvider());
        MapUtils.createMouseEventDispatcher(this, fullMapDisplay);
        for (int i = 0; i < trajDisplay.length; i++) {
            this.trajDisplay[i] = new UnfoldingMap(this, new BlankMap.BlankProvider());
            MapUtils.createMouseEventDispatcher(this, this.trajDisplay[i]);
        }
        int[] blue = {0, 128, 255};
        int[] green = {102, 255, 178};
        int[] red = {255, 0, 0};
        int[] lightPurple = {255, 0, 255};
        int[] pink = {255, 153, 153};

        // the iteration to be visualized
        int iteration = 1;

        try {
            // read the input map
            CSVMapReader csvInputMapReader = new CSVMapReader(INPUT_MAP);
            RoadNetworkGraph roadNetworkGraph = csvInputMapReader.readMap(PERCENTAGE, -1, false);

            // set the map center
            Location mapCenter = new Location((float) (roadNetworkGraph.getMaxLat() + roadNetworkGraph.getMinLat()) / 2, (float) (roadNetworkGraph
                    .getMaxLon() + roadNetworkGraph.getMinLon()) / 2);
            fullMapDisplay.zoomAndPanTo(14, mapCenter);
            for (UnfoldingMap mapDisplay : trajDisplay)
                mapDisplay.zoomAndPanTo(14, mapCenter);

            // read the removed roads
            List<RoadWay> removedRoadList = csvInputMapReader.readRemovedEdges(PERCENTAGE, -1);
            List<Marker> baseMapMarker = roadWayMarkerGen(roadNetworkGraph.getWays(), blue);
            List<Marker> removedMapMarker = roadWayMarkerGen(removedRoadList, green);
            List<Marker> removedWeightedMapMarker = weightedRoadWayMarkerGen(removedRoadList);
            fullMapDisplay.addMarkers(baseMapMarker);
            fullMapDisplay.addMarkers(removedMapMarker);
            trajDisplay[3].addMarkers(removedWeightedMapMarker);
            trajDisplay[4].addMarkers(removedWeightedMapMarker);
            trajDisplay[5].addMarkers(removedWeightedMapMarker);
            trajDisplay[6].addMarkers(removedWeightedMapMarker);

            // read the most completed map for trajectory comparison. the map before refinement has the most number of roads
            Map<String, RoadWay> id2RoadWay = new HashMap<>();
            CSVMapReader iterationMapReader = new CSVMapReader(CACHE_FOLDER);
            RoadNetworkGraph iterationMap = iterationMapReader.readMap(PERCENTAGE, iteration, true);
            // update the road way mapping for the look up of matched result
            for (RoadWay w : iterationMap.getWays())
                id2RoadWay.put(w.getId(), w);
            for (RoadWay w : removedRoadList)
                id2RoadWay.put(w.getId(), w);
            List<Marker> backGroundMapMarker = roadWayMarkerGen(iterationMap.getWays(), blue);
            trajDisplay[0].addMarkers(backGroundMapMarker);
            trajDisplay[1].addMarkers(backGroundMapMarker);
            trajDisplay[2].addMarkers(backGroundMapMarker);

            // read unmatched trajectory for break point evaluation
            CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
            List<Trajectory> unmatchedTraj = csvTrajectoryReader.readTrajectoryFilesList(CACHE_FOLDER + "unmatchedTraj/TP" +
                    MIN_TRAJ_POINT_COUNT + "_TI" + MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/0/");
            for (Trajectory traj : unmatchedTraj)
                trajDisplay[3].addMarkers(trajMarkerGen(traj, blue, 2));

            // read inferred edges and display on the map
            CSVMapReader inferredEdgeReader = new CSVMapReader(INFERENCE_FOLDER);
            List<RoadWay> inferredEdges = inferredEdgeReader.readInferredEdges();
            List<Marker> inferredEdgeMarker = roadWayMarkerGen(inferredEdges, blue);
            trajDisplay[4].addMarkers(inferredEdgeMarker);

            // read merged edges and display on the map
            CSVMapReader mergedEdgeReader = new CSVMapReader(CACHE_FOLDER);
            List<RoadWay> mergedEdges = mergedEdgeReader.readNewMapEdge(PERCENTAGE, 1, true);
            List<Marker> mergedEdgeMarker = roadWayMarkerGen(mergedEdges, blue);
            trajDisplay[5].addMarkers(mergedEdgeMarker);

            // read refined edges and display on the map
            List<RoadWay> updatedMap = iterationMapReader.readNewMapEdge(PERCENTAGE, 1, false);
            List<Marker> updatedMapMarker = roadWayMarkerGen(updatedMap, blue);
            trajDisplay[5].addMarkers(updatedMapMarker);

            // randomly select and visualize a trajectory and the corresponding matching result on the map
            Map<String, Trajectory> id2rawTraj = new HashMap<>();
            Map<String, TrajectoryMatchingResult> id2matchedTraj = new HashMap<>();
            Map<String, List<String>> id2GroundTruthTraj = new HashMap<>();
            Random random = new Random();
            List<Trajectory> rawTraj = csvTrajectoryReader.readTrajectoryFilesList(INPUT_TRAJECTORY);
            for (Trajectory traj : rawTraj)
                id2rawTraj.put(traj.getId(), traj);
            List<TrajectoryMatchingResult> matchedTraj = csvTrajectoryReader.readMatchedResult(OUTPUT_FOLDER, -1);
            for (TrajectoryMatchingResult matchingResult : matchedTraj)
                id2matchedTraj.put(matchingResult.getTrajID(), matchingResult);
            List<Pair<Integer, List<String>>> groundTruthMatchingResult = csvTrajectoryReader.readGroundTruthMatchingResult(GT_MATCHING_RESULT);
            for (Pair<Integer, List<String>> gtMatchingResult : groundTruthMatchingResult) {
                id2GroundTruthTraj.put(gtMatchingResult._1() + "", gtMatchingResult._2());
            }
            for (int i = 0; i < 3; i++) {
                String currIndex = random.nextInt(rawTraj.size()) + "";
                List<Marker> rawTrajMarker = trajMarkerGen(id2rawTraj.get(currIndex), red, 1);
                List<Marker> matchedTrajMarker = matchedTrajMarkerGen(id2matchedTraj.get(currIndex), id2RoadWay, lightPurple, 2);
                List<Marker> groundTruthMatchedTrajMarker = groundTruthMatchedTrajMarkerGen(id2GroundTruthTraj.get(currIndex), id2RoadWay,
                        green, 3);
                trajDisplay[i].addMarkers(rawTrajMarker);
                trajDisplay[i].addMarkers(groundTruthMatchedTrajMarker);
                trajDisplay[i].addMarkers(matchedTrajMarker);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        currTrajDisplay = fullMapDisplay;
    }

    public void draw() {
        currTrajDisplay.draw();
    }

    private List<Marker> weightedRoadWayMarkerGen(List<RoadWay> w) {
        List<Marker> result = new ArrayList<>();
        int colorGradeSize = w.size() / 256 > 0 ? w.size() / 256 : 1;   // calculate the rough size of each color grade
        Queue<RoadWay> weightedQueue = new PriorityQueue<>(Comparator.comparingInt(RoadWay::getVisitCount));
        weightedQueue.addAll(w);
        int processedRoadSize = 0;
        int prevVisitCount = -1;
        int lastColor = -1;
        while (!weightedQueue.isEmpty()) {
            RoadWay currWay = weightedQueue.remove();
            List<Location> locationList = new ArrayList<>();
            for (RoadNode n : currWay.getNodes()) {
                Location pointLocation = new Location(n.lat(), n.lon());
                locationList.add(pointLocation);
            }
            SimpleLinesMarker currLineMarker = new SimpleLinesMarker(locationList);
            int currColor = 0;
            if (currWay.getVisitCount() > prevVisitCount) {
                currColor = colorGradeSize == 0 ? 0 : processedRoadSize / colorGradeSize;
            } else if (currWay.getVisitCount() == prevVisitCount) {
                currColor = lastColor;
            } else System.out.println("ERROR! The visit count should not decrease.");
            currLineMarker.setColor(color(255, 255 - currColor, 0));
            currLineMarker.setStrokeWeight(1);
            prevVisitCount = currWay.getVisitCount();
            lastColor = currColor;
            processedRoadSize++;
            result.add(currLineMarker);
        }
        return result;
    }

    private List<Marker> roadWayMarkerGen(List<RoadWay> w, int[] color) {
        List<Marker> result = new ArrayList<>();
        for (RoadWay currWay : w) {
            List<Location> locationList = new ArrayList<>();
            for (RoadNode n : currWay.getNodes()) {
                Location pointLocation = new Location(n.lat(), n.lon());
                locationList.add(pointLocation);
            }
            SimpleLinesMarker currLineMarker = new SimpleLinesMarker(locationList);
            currLineMarker.setColor(color(color[0], color[1], color[2]));
            currLineMarker.setStrokeWeight(2);
            result.add(currLineMarker);
        }
        return result;
    }

    private List<Marker> trajMarkerGen(Trajectory traj, int[] color, int weight) {
        List<Marker> result = new ArrayList<>();
        List<Location> locationList = new ArrayList<>();
        for (STPoint n : traj.getSTPoints()) {
            Location pointLocation = new Location(n.y(), n.x());
            locationList.add(pointLocation);
        }
        SimpleLinesMarker currLineMarker = new SimpleLinesMarker(locationList);
        currLineMarker.setColor(color(color[0], color[1], color[2]));
        currLineMarker.setStrokeWeight(weight);
        result.add(currLineMarker);
        return result;
    }

    private List<Marker> matchedTrajMarkerGen(TrajectoryMatchingResult matchedTraj, Map<String, RoadWay> id2RoadWay, int[] color, int
            weight) {
        List<Marker> result = new ArrayList<>();
        for (String s : matchedTraj.getBestMatchWayList()) {
            List<Location> locationList = new ArrayList<>();
            if (id2RoadWay.containsKey(s)) {
                for (RoadNode n : id2RoadWay.get(s).getNodes()) {
                    Location pointLocation = new Location(n.lat(), n.lon());
                    locationList.add(pointLocation);
                }
                SimpleLinesMarker currLineMarker = new SimpleLinesMarker(locationList);
                currLineMarker.setColor(color(color[0], color[1], color[2]));
                currLineMarker.setStrokeWeight(weight);
                result.add(currLineMarker);
            } else
                System.out.println("ERROR! Cannot find roadID:" + s);
        }
        return result;
    }

    private List<Marker> groundTruthMatchedTrajMarkerGen(List<String> groundTruthMatchingResult, Map<String, RoadWay> id2RoadWay, int[]
            color, int weight) {
        List<Marker> result = new ArrayList<>();
        for (String s : groundTruthMatchingResult) {
            List<Location> locationList = new ArrayList<>();
            for (RoadNode n : id2RoadWay.get(s).getNodes()) {
                Location pointLocation = new Location(n.lat(), n.lon());
                locationList.add(pointLocation);
            }
            SimpleLinesMarker currLineMarker = new SimpleLinesMarker(locationList);
            currLineMarker.setColor(color(color[0], color[1], color[2]));
            currLineMarker.setStrokeWeight(weight);
            result.add(currLineMarker);
        }
        return result;
    }

    public void keyPressed() {
        switch (key) {
            case '1': {
                currTrajDisplay = trajDisplay[0];
                break;
            }
            case '2': {
                currTrajDisplay = trajDisplay[1];
                break;
            }
            case '3': {
                currTrajDisplay = trajDisplay[2];
                break;
            }
            case '4': {
                currTrajDisplay = trajDisplay[3];
                break;
            }
            case '5': {
                currTrajDisplay = trajDisplay[4];
                break;
            }
            case '6': {
                currTrajDisplay = trajDisplay[5];
                break;
            }
            case '7': {
                currTrajDisplay = trajDisplay[6];
                break;
            }
//            case '8': {
//                currTrajDisplay = trajDisplay[7];
//                break;
//            }
//            case '9': {
//                currTrajDisplay = trajDisplay[8];
//                break;
//            }
//            case '0': {
//                currTrajDisplay = trajDisplay[9];
//                break;
//            }
            case '-': {
                currTrajDisplay = fullMapDisplay;
                break;
            }
            default:
                break;
        }
    }

    public void display() {
        PApplet.main(new String[]{this.getClass().getName()});
    }

    public static void main(String[] args) {
        PApplet.main(new String[]{UnfoldingBeijingTrajectoryDisplay.class.getName()});
    }
}