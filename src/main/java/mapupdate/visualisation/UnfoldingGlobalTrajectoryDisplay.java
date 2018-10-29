package mapupdate.visualisation;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.providers.OpenStreetMap;
import de.fhpotsdam.unfolding.utils.MapUtils;
import mapupdate.util.io.CSVRawMapReader;
import mapupdate.util.io.CSVTrajectoryReader;
import mapupdate.util.io.XMLTrajectoryReader;
import mapupdate.util.object.datastructure.TrajectoryMatchingResult;
import mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import mapupdate.util.object.roadnetwork.RoadNode;
import mapupdate.util.object.roadnetwork.RoadWay;
import mapupdate.util.object.spatialobject.Point;
import mapupdate.util.object.spatialobject.TrajectoryPoint;
import mapupdate.util.object.spatialobject.Trajectory;
import processing.core.PApplet;

import java.io.IOException;
import java.util.*;

import static mapupdate.Main.*;

/**
 * Simple Unfolding map app demo.
 *
 * @author uqdalves
 */
//http://unfoldingmaps.org/
public class UnfoldingGlobalTrajectoryDisplay extends PApplet {

    private UnfoldingMap currTrajDisplay;
    private UnfoldingMap[] trajDisplay = new UnfoldingMap[10];   // 3= removed edges and unmatched trajectories

    public void setup() {
        size(1760, 990, JAVA2D);
        int[] blue = {0, 128, 255};
        int[] green = {102, 255, 178};
        int[] red = {255, 0, 0};
        int[] lightPurple = {255, 0, 255};

        try {
            XMLTrajectoryReader trajReader = new XMLTrajectoryReader(ROOT_PATH + "input/");
            CSVTrajectoryReader matchedResultReader = new CSVTrajectoryReader();
            CSVRawMapReader mapReader = new CSVRawMapReader(ROOT_PATH + "input/");
            List<TrajectoryMatchingResult> matchedResult = matchedResultReader.readMatchedResult(OUTPUT_FOLDER, -1);
            Map<String, TrajectoryMatchingResult> id2MatchingResult = new HashMap<>();
            for (TrajectoryMatchingResult mr : matchedResult) {
                id2MatchingResult.put(mr.getTrajID(), mr);
            }
            for (int i = 0; i < trajDisplay.length; i++) {
                int trajNum = 10 + i;
                RoadNetworkGraph currMap = mapReader.readRawMap(trajNum);
                Map<String, RoadWay> id2RoadWay = new HashMap<>();
                for (RoadWay w : currMap.getWays()) {
                    id2RoadWay.put(w.getID(), w);
                }
                Trajectory traj = trajReader.readInputTrajectory(trajNum);
                List<String> groundTruthResult = trajReader.readGroundTruthMatchResult(trajNum);
                Point middlePoint = traj.get(traj.size() / 2);
                // set the map center
                trajDisplay[i] = new UnfoldingMap(this, new OpenStreetMap.OpenStreetMapProvider());
                MapUtils.createMouseEventDispatcher(this, this.trajDisplay[i]);
                Location mapCenter = new Location(middlePoint.y(), middlePoint.x());
                trajDisplay[i].zoomAndPanTo(14, mapCenter);

                List<Marker> rawTrajMarker = trajMarkerGen(traj, red, 2);
                List<Marker> matchedTrajMarker = matchedTrajMarkerGen(id2MatchingResult.get(traj.getId()), id2RoadWay, lightPurple, 4);
                List<Marker> groundTruthMatchedTrajMarker = groundTruthMatchedTrajMarkerGen(groundTruthResult, id2RoadWay, green, 2);
                trajDisplay[i].addMarkers(rawTrajMarker);
                trajDisplay[i].addMarkers(matchedTrajMarker);
                trajDisplay[i].addMarkers(groundTruthMatchedTrajMarker);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        currTrajDisplay = trajDisplay[0];
    }

    public void draw() {
        currTrajDisplay.draw();
    }

    private List<Marker> trajMarkerGen(Trajectory currTraj, int[] color, int weight) {
        List<Marker> result = new ArrayList<>();
        List<Location> locationList = new ArrayList<>();
        for (TrajectoryPoint n : currTraj.getSTPoints()) {
            Location pointLocation = new Location(n.y(), n.x());
            locationList.add(pointLocation);
        }
        SimpleLinesMarker currLineMarker = new SimpleLinesMarker(locationList);
        currLineMarker.setColor(color(color[0], color[1], color[2]));
        currLineMarker.setStrokeWeight(weight);
        result.add(currLineMarker);
        return result;
    }

    private List<Marker> matchedTrajMarkerGen(TrajectoryMatchingResult matchedTraj, Map<String, RoadWay> id2RoadWay, int[] color,
                                              int weight) {
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
                LOGGER.severe("ERROR! Cannot find roadID:" + s);
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
            case '8': {
                currTrajDisplay = trajDisplay[7];
                break;
            }
            case '9': {
                currTrajDisplay = trajDisplay[8];
                break;
            }
            case '0': {
                currTrajDisplay = trajDisplay[9];
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
        PApplet.main(new String[]{UnfoldingGlobalTrajectoryDisplay.class.getName()});
    }
}