package mapupdate.visualisation;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.utils.MapUtils;
import mapupdate.util.io.CSVMapReader;
import mapupdate.util.io.CSVTrajectoryReader;
import mapupdate.util.object.datastructure.Pair;
import mapupdate.util.object.datastructure.TrajectoryMatchingResult;
import mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import mapupdate.util.object.roadnetwork.RoadNode;
import mapupdate.util.object.roadnetwork.RoadWay;
import mapupdate.util.object.spatialobject.STPoint;
import mapupdate.util.object.spatialobject.Trajectory;
import processing.core.PApplet;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static mapupdate.Main.*;

/**
 * Manually display trajectory and matching results.
 *
 * @author uqpchao
 */
//http://unfoldingmaps.org/
public class UnfoldingBeijingTrajectoryManualDisplay extends PApplet {

    private UnfoldingMap currTrajDisplay;
    private UnfoldingMap[] trajDisplay = new UnfoldingMap[3];   // 0~6 = matching result comparison for three different trajectories,
    // 7~10 = unmatched trajectory,initial inference road, merged road, refined road comparison

    public void setup() {
        boolean displayActualMap = false;
        size(1760, 990, JAVA2D);
        for (int i = 0; i < trajDisplay.length; i++) {
            this.trajDisplay[i] = new UnfoldingMap(this, displayActualMap ? new Google.GoogleMapProvider() : new BlankMap.BlankProvider());
            MapUtils.createMouseEventDispatcher(this, this.trajDisplay[i]);
        }
        int[] blue = {0, 128, 255};
        int[] green = {102, 255, 178};
        int[] red = {255, 0, 0};
        int[] lightPurple = {255, 0, 255};
        int[] pink = {255, 153, 153};
        int[] black = {0, 0, 0};

        String trajectoryID = "4";
        String[] matchRoadList1 = {"676968","676966","582316","577721","685410","693360","693361","586449","579168","678747","678748","590837","590831","686502","686503","583710","583711","579167","664575","664576","706887","706889","577700","592348","-592348","-706897","-706895","671763"};
        String[] matchRoadList2 = {"676968","676966","582316","577721","685410","693360","693361","586449","579168","678747","678748","590837","590831","686502","686503","583710","583711","579167","664575","664576","706887","706889","577720","592348","49044058","49044059","568895","686504","686505","606352","687125","687126","-568896","-565577","-708382","-708380","671765","671766","706895","706897","-706897","-706895","671763"};

        try {
            // read the input map
            CSVMapReader csvInputMapReader = new CSVMapReader(INPUT_MAP);
            RoadNetworkGraph roadNetworkGraph = csvInputMapReader.readMap(PERCENTAGE, -1, false);
            Map<String, RoadWay> id2RoadWay = new HashMap<>();
            for (RoadWay w : roadNetworkGraph.getWays())
                id2RoadWay.put(w.getID(), w);
            // read the removed roads
            List<RoadWay> removedRoadList = csvInputMapReader.readRemovedEdges(PERCENTAGE, -1);
            for (RoadWay w : removedRoadList)
                id2RoadWay.put(w.getID(), w);

            // set the background map and map center
            Location mapCenter = new Location((float) (roadNetworkGraph.getMaxLat() + roadNetworkGraph.getMinLat()) / 2, (float) (roadNetworkGraph
                    .getMaxLon() + roadNetworkGraph.getMinLon()) / 2);
            for (UnfoldingMap mapDisplay : trajDisplay)
                mapDisplay.zoomAndPanTo(14, mapCenter);
            List<Marker> baseMapMarker = roadWayMarkerGen(roadNetworkGraph.getWays(), displayActualMap ? black : blue);
            List<Marker> removedWeightedMapMarker = weightedRoadWayMarkerGen(removedRoadList);
            for (UnfoldingMap mapDisplay : trajDisplay) {
                mapDisplay.addMarkers(baseMapMarker);
                mapDisplay.addMarkers(removedWeightedMapMarker);
            }

            // display the particular raw trajectory
            CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
            Trajectory displayTraj = csvTrajectoryReader.readTrajectory(new File(INPUT_TRAJECTORY + "trip_" + trajectoryID + ".txt"));
            List<Marker> trajMarker = trajMarkerGen(displayTraj, red, 2);
            for (UnfoldingMap mapDisplay : trajDisplay) {
                mapDisplay.addMarkers(trajMarker);
            }
            // display the previous matching result and the current one
            List<RoadWay> prevMatchRoadWayList = new ArrayList<>();
            for (String s : matchRoadList1) {
                if (id2RoadWay.containsKey(s))
                    prevMatchRoadWayList.add(id2RoadWay.get(s));
            }
            trajDisplay[1].addMarkers(roadWayMarkerGen(prevMatchRoadWayList, green));
            List<RoadWay> currMatchRoadWayList = new ArrayList<>();
            for (String s : matchRoadList2) {
                if (id2RoadWay.containsKey(s))
                    currMatchRoadWayList.add(id2RoadWay.get(s));
            }
            trajDisplay[2].addMarkers(roadWayMarkerGen(currMatchRoadWayList, lightPurple));
        } catch (IOException e) {
            e.printStackTrace();
        }
        currTrajDisplay = trajDisplay[0];
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
            } else LOGGER.severe("ERROR! The visit count should not decrease.");
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
            default:
                break;
        }
    }

    public void display() {
        PApplet.main(new String[]{this.getClass().getName()});
    }

    public static void main(String[] args) {
        PApplet.main(new String[]{UnfoldingBeijingTrajectoryManualDisplay.class.getName()});
    }
}