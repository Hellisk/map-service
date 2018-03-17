package edu.uq.dke.mapupdate.visualisation;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.providers.MapBox;
import de.fhpotsdam.unfolding.utils.MapUtils;
import edu.uq.dke.mapupdate.datatype.TrajectoryMatchResult;
import edu.uq.dke.mapupdate.io.CSVMapReader;
import edu.uq.dke.mapupdate.io.CSVTrajectoryReader;
import processing.core.PApplet;
import traminer.util.Pair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple Unfolding map app demo.
 *
 * @author uqdalves
 */
//http://unfoldingmaps.org/
public class UnfoldingGraphDisplay extends PApplet {

    private String ROOT_PATH = "C:/data/trajectorydata/";
    //    private final static String ROOT_PATH = "F:/data/trajectorydata/";
    private int PERCENTAGE = 0;    // remove percentage for map display
    private UnfoldingMap map;
    // options = 2 should always be the last one if it has
    private int options = 423;    // 0=nothing, 1= removed edges, 2= map, 3= raw trajectories, 4= trajectory matching result, 5= unmatched trajectory pieces
    private String trajID = "25";

    public static void main(String args[]) {
        PApplet.main(new String[]{"edu.uq.dke.mapupdate.visualisation.UnfoldingGraphDisplay"});
    }

    public void settings() {
        size(1440, 900, P2D);
        this.map = new UnfoldingMap(this, new MapBox.BlankProvider());
        MapUtils.createDefaultEventDispatcher(this, map);
        Location mapCenter = new Location(39.968346f, 116.419598f);  // location in beijing
        map.zoomAndPanTo(15, mapCenter);
        Map<String, RoadWay> findWayByID = new HashMap<>();

        try {
            while (options != 0) {
                int lastOption = this.options % 10;
                this.options = this.options / 10;
                switch (lastOption) {
                    // removed edges
                    case 1: {
                        if (PERCENTAGE != 0) {
                            CSVMapReader csvMapReader = new CSVMapReader(ROOT_PATH + "input/map/");
                            List<RoadWay> roadNetworkGraph = csvMapReader.readRemovedEdges(PERCENTAGE);
                            List<Marker> linesMarkers = new ArrayList<>();
                            for (RoadWay w : roadNetworkGraph) {
                                List<Location> locationList = new ArrayList<>();
                                for (RoadNode n : w.getNodes()) {
                                    Location pointLocation = new Location(n.lat(), n.lon());
                                    locationList.add(pointLocation);
                                }
                                SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                                marker.setColor(color(255, 97, 0));  // color orange
                                marker.setStrokeWeight(3);
                                linesMarkers.add(marker);
                            }
                            map.addMarkers(linesMarkers);
                        }
                        break;
                    }
                    // raw map
                    case 2: {
                        CSVMapReader csvMapReader = new CSVMapReader(ROOT_PATH + (PERCENTAGE == 0 ? "groundTruth/map/" : "input/map/"));
                        RoadNetworkGraph roadNetworkGraph = csvMapReader.readMap(PERCENTAGE);
                        List<Marker> linesMarkers = new ArrayList<>();
                        for (RoadWay w : roadNetworkGraph.getWays()) {
                            findWayByID.put(w.getId(), w);
                            List<Location> locationList = new ArrayList<>();
                            for (RoadNode n : w.getNodes()) {
                                Location pointLocation = new Location(n.lat(), n.lon());
                                locationList.add(pointLocation);
                            }
                            SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                            marker.setColor(color(245, 245, 245, 100));  // color white smoky
                            marker.setStrokeWeight(3);
                            linesMarkers.add(marker);
                        }
                        map.addMarkers(linesMarkers);
                    }
                    break;
                    // raw trajectory
                    case 3: {
                        CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
                        List<Trajectory> rawTrajectoryList = csvTrajectoryReader.readTrajectoryFilesList(ROOT_PATH + "input/trajectory/");
                        List<Marker> linesMarkers = new ArrayList<>();
                        for (Trajectory t : rawTrajectoryList) {
                            if (trajID.equals("-1") || t.getId().equals(trajID)) {
                                List<Location> locationList = new ArrayList<>();
                                for (STPoint p : t.getSTPoints()) {
                                    Location pointLocation = new Location(p.y(), p.x());
                                    locationList.add(pointLocation);
                                }
                                SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                                marker.setColor(color(233, 57, 35));  // color red
                                marker.setStrokeWeight(4);
                                linesMarkers.add(marker);
                            }
                        }
                        map.addMarkers(linesMarkers);
                        break;
                    }
                    // matching result
                    case 4: {
                        CSVTrajectoryReader matchingResultReader = new CSVTrajectoryReader();
                        List<TrajectoryMatchResult> matchedTrajectoryList = matchingResultReader.readMatchedResult(ROOT_PATH + "output/");
                        List<Marker> linesMarkers = new ArrayList<>();
                        for (TrajectoryMatchResult t : matchedTrajectoryList) {
                            if (trajID.equals("-1") || t.getTrajID().equals(trajID)) {
                                for (String s : t.getMatchWayList()) {
                                    List<Location> locationList = new ArrayList<>();
                                    for (RoadNode n : findWayByID.get(s).getNodes()) {
                                        Location pointLocation = new Location(n.lat(), n.lon());
                                        locationList.add(pointLocation);
                                    }
                                    SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                                    marker.setColor(color(59, 130, 79));  // color green
                                    marker.setStrokeWeight(4);
                                    linesMarkers.add(marker);
                                }
                            }
                        }
                        map.addMarkers(linesMarkers);
                        break;
                    }
                    // unmatched trajectory
                    case 5: {
                        CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
                        List<Trajectory> rawTrajectoryList = csvTrajectoryReader.readTrajectoryFilesList(ROOT_PATH + "output/unmatchedTraj/");
                        List<Marker> linesMarkers = new ArrayList<>();
                        for (Trajectory t : rawTrajectoryList) {
                            List<Location> locationList = new ArrayList<>();
                            for (STPoint p : t.getSTPoints()) {
                                Location pointLocation = new Location(p.y(), p.x());
                                locationList.add(pointLocation);
                            }
                            SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                            marker.setColor(color(245, 222, 179));  // color light yellow
                            marker.setStrokeWeight(4);
                            map.addMarker(marker);
                            linesMarkers.add(marker);
                        }
                        map.addMarkers(linesMarkers);
                        break;
                    }
                    // ground truth mapping result
                    case 6: {
                        CSVMapReader csvMapReader = new CSVMapReader(ROOT_PATH + "groundTruth/map/");
                        RoadNetworkGraph roadNetworkGraph = csvMapReader.readMap(0);
                        HashMap<String, RoadWay> idRoadWayMapping = new HashMap<>();
                        for (RoadWay w : roadNetworkGraph.getWays()) {
                            idRoadWayMapping.put(w.getId(), w);
                        }
                        CSVTrajectoryReader groundTruthMatchingResultReader = new CSVTrajectoryReader();
                        List<Pair<Integer, List<String>>> gtMatchingResult = groundTruthMatchingResultReader.readGroundTruthMatchingResult(ROOT_PATH + "groundTruth/matchingResult/");
                        List<Marker> linesMarkers = new ArrayList<>();
                        for (Pair<Integer, List<String>> r : gtMatchingResult) {
                            if (r._1() == Integer.parseInt(trajID)) {
                                for (String s : r._2()) {
                                    if (idRoadWayMapping.containsKey(s)) {
                                        List<Location> locationList = new ArrayList<>();
                                        for (RoadNode n : idRoadWayMapping.get(s).getNodes()) {
                                            Location pointLocation = new Location(n.lat(), n.lon());
                                            locationList.add(pointLocation);
                                        }
                                        SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                                        marker.setColor(color(0, 255, 255));  // color light blue
                                        marker.setStrokeWeight(4);
                                        linesMarkers.add(marker);
                                    }
                                }
                            }
                        }
                        map.addMarkers(linesMarkers);
                        break;
                    }
                    default:
                        System.out.println("Error display option:" + this.options);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void draw() {
        map.draw();
    }

    public void display() {
        PApplet.main(new String[]{this.getClass().getName()});
    }

}