package edu.uq.dke.mapupdate.visualisation;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.utils.MapUtils;
import edu.uq.dke.mapupdate.util.io.CSVMapReader;
import edu.uq.dke.mapupdate.util.io.CSVTrajectoryReader;
import edu.uq.dke.mapupdate.util.object.datastructure.Pair;
import edu.uq.dke.mapupdate.util.object.datastructure.TrajectoryMatchResult;
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
public class UnfoldingTrajectoryDisplay extends PApplet {

    private UnfoldingMap currTrajDisplay;
    private UnfoldingMap fullMapDisplay;    // full map visualization
    private UnfoldingMap[] trajDisplay = new UnfoldingMap[6];   // 3= removed edges and unmatched trajectories

    public void setup() {
        size(1440, 900, JAVA2D);
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

        try {
            // read the input map
            CSVMapReader csvInputMapReader = new CSVMapReader(INPUT_MAP);
            RoadNetworkGraph roadNetworkGraph = csvInputMapReader.readMap(PERCENTAGE);

            // set the map center
            Location mapCenter = new Location((float) (roadNetworkGraph.getMaxLat() + roadNetworkGraph.getMinLat()) / 2, (float) (roadNetworkGraph
                    .getMaxLon() + roadNetworkGraph.getMinLon()) / 2);
            fullMapDisplay.zoomAndPanTo(14, mapCenter);
            for (UnfoldingMap mapDisplay : trajDisplay)
                mapDisplay.zoomAndPanTo(14, mapCenter);

            List<RoadWay> removedRoadList = csvInputMapReader.readRemovedEdges(PERCENTAGE);
            List<Marker> baseMapMarker = roadWayMarkerGen(roadNetworkGraph.getWays(), blue);
            List<Marker> removedMapMarker = roadWayMarkerGen(removedRoadList, green);
            List<Marker> removedWeightedMapMarker = weightedRoadWayMarkerGen(removedRoadList);
            fullMapDisplay.addMarkers(baseMapMarker);
            fullMapDisplay.addMarkers(removedMapMarker);
            trajDisplay[3].addMarkers(removedWeightedMapMarker);
            trajDisplay[4].addMarkers(removedWeightedMapMarker);

            // read the raw map for trajectory comparison
            Map<String, RoadWay> id2RoadWay = new HashMap<>();
            CSVMapReader csvRawMapReader = new CSVMapReader(GT_MAP);
            RoadNetworkGraph rawRoadGraph = csvRawMapReader.readMap(0);
            List<Marker> rawMapMarker = roadWayMarkerGen(rawRoadGraph.getWays(), blue);
            // update the road way mapping for the look up of matched result
            for (RoadWay w : rawRoadGraph.getWays())
                id2RoadWay.put(w.getId(), w);
//            trajDisplay[0].addMarkers(rawMapMarker);
//            trajDisplay[1].addMarkers(rawMapMarker);
//            trajDisplay[2].addMarkers(rawMapMarker);

            // read unmatched trajectory for break point evaluation
            CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
            List<Trajectory> unmatchedTraj = csvTrajectoryReader.readTrajectoryFilesList(OUTPUT_FOLDER + "unmatchedTraj/TP" +
                    MIN_TRAJ_POINT_COUNT + "_TI" + MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/");
            List<Marker> unmatchedMarker = trajMarkerGen(unmatchedTraj, blue, 2);
            trajDisplay[3].addMarkers(unmatchedMarker);

            // randomly select and visualize a trajectory and the corresponding matching result on the map
            Random random = new Random();
            List<Trajectory> rawTraj = csvTrajectoryReader.readTrajectoryFilesList(INPUT_TRAJECTORY);
            List<Marker> rawTrajMarker = trajMarkerGen(rawTraj, red, 3);
            List<TrajectoryMatchResult> matchedTraj = csvTrajectoryReader.readMatchedResult(OUTPUT_FOLDER);
            List<Marker> matchedTrajMarker = matchedTrajMarkerGen(matchedTraj, id2RoadWay, lightPurple);
            List<Pair<Integer, List<String>>> groundTruthMatchingResult = csvTrajectoryReader.readGroundTruthMatchingResult(GT_MATCHING_RESULT);
            List<Marker> groundTruthMatchedTrajMarker = groundTruthMatchedTrajMarkerGen(groundTruthMatchingResult, id2RoadWay, green);
            for (int i = 0; i < 3; i++) {
                int currIndex = random.nextInt(rawTraj.size());
                trajDisplay[i].addMarkers(rawTrajMarker.get(currIndex));
                trajDisplay[i].addMarkers(matchedTrajMarker.get(currIndex));
                trajDisplay[i].addMarkers(groundTruthMatchedTrajMarker.get(currIndex));
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
        int colorGradeSize = w.size() / 256;   // calculate the rough size of each color grade
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

    private List<Marker> trajMarkerGen(List<Trajectory> traj, int[] color, int weight) {
        List<Marker> result = new ArrayList<>();
        for (Trajectory currTraj : traj) {
            List<Location> locationList = new ArrayList<>();
            for (STPoint n : currTraj.getSTPoints()) {
                Location pointLocation = new Location(n.y(), n.x());
                locationList.add(pointLocation);
            }
            SimpleLinesMarker currLineMarker = new SimpleLinesMarker(locationList);
            currLineMarker.setColor(color(color[0], color[1], color[2]));
            currLineMarker.setStrokeWeight(weight);
            result.add(currLineMarker);
        }
        return result;
    }

    private List<Marker> matchedTrajMarkerGen(List<TrajectoryMatchResult> matchedTraj, Map<String, RoadWay> id2RoadWay, int[] color) {
        List<Marker> result = new ArrayList<>();
        for (TrajectoryMatchResult matchResult : matchedTraj) {
            for (String s : matchResult.getBestMatchWayList()) {
                List<Location> locationList = new ArrayList<>();
                if (id2RoadWay.containsKey(s)) {
                    for (RoadNode n : id2RoadWay.get(s).getNodes()) {
                        Location pointLocation = new Location(n.lat(), n.lon());
                        locationList.add(pointLocation);
                    }
                    SimpleLinesMarker currLineMarker = new SimpleLinesMarker(locationList);
                    currLineMarker.setColor(color(color[0], color[1], color[2]));
                    currLineMarker.setStrokeWeight(3);
                    result.add(currLineMarker);
                } else
                    System.out.println("ERROR! Cannot find roadID:" + s);
            }
        }
        return result;
    }

    private List<Marker> groundTruthMatchedTrajMarkerGen(List<Pair<Integer, List<String>>> groundTruthMatchingResult, Map<String, RoadWay> id2RoadWay, int[] color) {
        List<Marker> result = new ArrayList<>();
        for (Pair<Integer, List<String>> gtMatchResult : groundTruthMatchingResult) {
            for (String s : gtMatchResult._2()) {
                List<Location> locationList = new ArrayList<>();
                for (RoadNode n : id2RoadWay.get(s).getNodes()) {
                    Location pointLocation = new Location(n.lat(), n.lon());
                    locationList.add(pointLocation);
                }
                SimpleLinesMarker currLineMarker = new SimpleLinesMarker(locationList);
                currLineMarker.setColor(color(color[0], color[1], color[2]));
                currLineMarker.setStrokeWeight(3);
                result.add(currLineMarker);
            }
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
//            case '7': {
//                currTrajDisplay = trajDisplay[6];
//                break;
//            }
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
        PApplet.main(new String[]{UnfoldingTrajectoryDisplay.class.getName()});
    }
}