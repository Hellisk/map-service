package edu.uq.dke.mapupdate.visualisation;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.utils.MapUtils;
import edu.uq.dke.mapupdate.datatype.MatchingResult;
import edu.uq.dke.mapupdate.io.CSVMapReader;
import edu.uq.dke.mapupdate.io.CSVTrajectoryReader;
import processing.core.PApplet;
import traminer.util.map.matching.PointNodePair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;
import traminer.util.map.roadnetwork.RoadWay;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple Unfolding map app demo.
 *
 * @author uqdalves
 */
//http://unfoldingmaps.org/
public class UnfoldingGraphDisplay extends PApplet {

    private final static String ROOT_PATH = "F:/data/trajectorydata/";
    //    private final static String ROOT_PATH = "C:/data/trajectorydata/";
    private final static int PERCENTAGE = 0;    // remove percentage for map display
    private UnfoldingMap map;
    private int options = 2;    // 0=nothing, 1= points(undone), 2= map, 3= raw trajectories, 4= trajectory matching result, 5= unmatched trajectory pieces
    private String trajID = "46";

    public static void main(String args[]) {
        PApplet.main(new String[]{"edu.uq.dke.mapupdate.visualisation.UnfoldingGraphDisplay"});
    }

    public void settings() {
        size(1440, 900, P2D);
        this.map = new UnfoldingMap(this, new Google.GoogleMapProvider());
        MapUtils.createDefaultEventDispatcher(this, map);
        Location mapCenter = new Location(39.968346f, 116.419598f);  // location in beijing
        map.zoomAndPanTo(15, mapCenter);
//    }
//
//    public void setup() {

        try {
            while (options != 0) {
                int lastOption = this.options % 10;
                this.options = this.options / 10;
                switch (lastOption) {
                    case 1: {
                        break;
                    }
                    case 2: {
                        CSVMapReader csvMapReader = new CSVMapReader(ROOT_PATH + "input/map/");
                        RoadNetworkGraph roadNetworkGraph = csvMapReader.readMap(PERCENTAGE);
                        List<Marker> linesMarkers = new ArrayList<>();
                        for (RoadWay w : roadNetworkGraph.getWays()) {
                            List<Location> locationList = new ArrayList<>();
                            for (RoadNode n : w.getNodes()) {
                                Location pointLocation = new Location(n.lat(), n.lon());
                                locationList.add(pointLocation);
                            }
                            SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                            marker.setColor(color(154, 156, 157));  // color silver
                            marker.setStrokeWeight(2);
                            linesMarkers.add(marker);
                        }
                        map.addMarkers(linesMarkers);
                    }
                    break;
                    case 3: {
                        CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
                        List<Trajectory> rawTrajectoryList = csvTrajectoryReader.readTrajectoryFilesList(ROOT_PATH + "input/trajectory/");
                        List<Marker> linesMarkers = new ArrayList<>();
                        for (Trajectory t : rawTrajectoryList) {
                            if (t.getId().equals(trajID)) {
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
                    case 4: {
                        CSVTrajectoryReader matchingResultReader = new CSVTrajectoryReader();
                        List<MatchingResult> matchedTrajectoryList = matchingResultReader.readMatchingResult(ROOT_PATH + "output/");
                        List<Marker> linesMarkers = new ArrayList<>();
                        for (MatchingResult t : matchedTrajectoryList) {
                            if (t.getTrajID().equals(trajID)) {
                                List<Location> locationList = new ArrayList<>();
                                for (PointNodePair p : t.getMatchingResult()) {
                                    Location pointLocation = new Location(p.getMatchingPoint().lat(), p.getMatchingPoint().lon());
                                    locationList.add(pointLocation);
                                }
                                SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                                marker.setColor(color(59, 130, 79));  // color green
                                marker.setStrokeWeight(4);
                                linesMarkers.add(marker);
                            }
                        }
                        map.addMarkers(linesMarkers);
                        break;
                    }
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
                            marker.setColor(color(238, 137, 40));  // color orange
                            marker.setStrokeWeight(4);
                            map.addMarker(marker);
                            linesMarkers.add(marker);
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