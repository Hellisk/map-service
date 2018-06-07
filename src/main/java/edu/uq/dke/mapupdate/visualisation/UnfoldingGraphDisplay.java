package edu.uq.dke.mapupdate.visualisation;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.utils.MapUtils;
import edu.uq.dke.mapupdate.util.io.CSVMapReader;
import edu.uq.dke.mapupdate.util.io.CSVTrajectoryReader;
import edu.uq.dke.mapupdate.util.io.RawFileOperation;
import edu.uq.dke.mapupdate.util.io.RawMapReader;
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
public class UnfoldingGraphDisplay extends PApplet {

    private UnfoldingMap map;
    private int options = 18;    // 0=nothing, 1= removed edges, 2= map, 3= raw trajectories, 4= trajectory matching result, 5=
    // unmatched trajectory pieces, 6= ground truth matching result, 7= map comparison, 8= inferred map, 9= merged map
    private Set<String> trajectoryID = new HashSet<>();

    public void settings() {
        size(1920, 1080, P2D);
        this.map = new UnfoldingMap(this, new Google.GoogleMapProvider());
        MapUtils.createMouseEventDispatcher(this, map);

        Map<String, RoadWay> findWayByID = new HashMap<>();

        try {
            // read the map first
            CSVMapReader csvMapReader = new CSVMapReader(ROOT_PATH + "groundTruth/map/");
            RoadNetworkGraph roadNetworkGraph = csvMapReader.readMap(0);
            for (RoadWay w : roadNetworkGraph.getWays()) {
                findWayByID.put(w.getId(), w);
            }
            // location in beijing
            Location mapCenter = new Location((float) (roadNetworkGraph.getMaxLat() + roadNetworkGraph.getMinLat()) / 2, (float) (roadNetworkGraph
                    .getMaxLon() + roadNetworkGraph.getMinLon()) / 2);
            map.zoomAndPanTo(14, mapCenter);
            map.setPanningRestriction(mapCenter, 50);

            Random random = new Random();
            for (int i = 0; i < TRAJECTORY_COUNT / 100; i++) {
                trajectoryID.add(random.nextInt(TRAJECTORY_COUNT) + "");
            }

            while (options != 0) {
                int lastOption = this.options % 10;
                this.options = this.options / 10;
                switch (lastOption) {
                    // removed edges
                    case 1: {
                        if (PERCENTAGE != 0) {
                            CSVMapReader csvRemovedMapReader = new CSVMapReader(ROOT_PATH + "input/map/");
                            List<RoadWay> removedRoadNetworkGraph = csvRemovedMapReader.readRemovedEdges(PERCENTAGE);
                            List<Marker> linesMarkers = new ArrayList<>();
                            for (RoadWay w : removedRoadNetworkGraph) {
                                List<Location> locationList = new ArrayList<>();
                                for (RoadNode n : w.getNodes()) {
                                    Location pointLocation = new Location(n.lat(), n.lon());
                                    locationList.add(pointLocation);
                                }
                                SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                                marker.setColor(color(255, 178, 102));  // color orange
                                marker.setStrokeWeight(3);
                                linesMarkers.add(marker);
                            }
                            map.addMarkers(linesMarkers);
                        }
                        break;
                    }
                    // raw map
                    case 2: {
                        List<Marker> linesMarkers = new ArrayList<>();
                        for (RoadWay w : roadNetworkGraph.getWays()) {
                            List<Location> locationList = new ArrayList<>();
                            for (RoadNode n : w.getNodes()) {
                                Location pointLocation = new Location(n.lat(), n.lon());
                                locationList.add(pointLocation);
                            }
                            SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                            marker.setColor(color(178, 102, 255));  // color purple
                            marker.setStrokeWeight(3);
                            linesMarkers.add(marker);
                        }
                        map.addMarkers(linesMarkers);
                    }
                    break;
                    // raw trajectory
                    case 3: {
//                        CSVTrajectoryReader csvTrajectoryReader = new CSVTrajectoryReader();
//                        List<Trajectory> rawTrajectoryList = csvTrajectoryReader.readTrajectoryFilesList(ROOT_PATH + "input/trajectory/");
                        RawFileOperation trajFilter = new RawFileOperation(TRAJECTORY_COUNT, MIN_TRAJ_POINT_COUNT, false,
                                MAX_TIME_INTERVAL);
                        List<Trajectory> rawTrajectoryList = trajFilter.RawTrajectoryParser(GT_MAP, RAW_TRAJECTORY, INPUT_TRAJECTORY,
                                GT_MATCHING_RESULT);
                        List<Marker> linesMarkers = new ArrayList<>();
                        for (Trajectory t : rawTrajectoryList) {
                            if (trajectoryID.contains(t.getId())) {
                                List<Location> locationList = new ArrayList<>();
                                for (STPoint p : t.getSTPoints()) {
                                    Location pointLocation = new Location(p.y(), p.x());
                                    locationList.add(pointLocation);
                                }
                                SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                                marker.setColor(color(233, 57, 35));  // color red
                                marker.setStrokeWeight(3);
                                linesMarkers.add(marker);
                            }
                        }
                        map.addMarkers(linesMarkers);
                        break;
                    }
                    // matching result
                    case 4: {
                        CSVTrajectoryReader matchingResultReader = new CSVTrajectoryReader();
                        List<TrajectoryMatchResult> matchedTrajectoryList = matchingResultReader.readMatchedResult(ROOT_PATH + "output/",
                                RANK_LENGTH);
                        List<Marker> linesMarkers = new ArrayList<>();
                        for (TrajectoryMatchResult t : matchedTrajectoryList) {
                            if (trajectoryID.contains(t.getTrajID())) {
                                for (String s : t.getBestMatchWayList()) {
                                    List<Location> locationList = new ArrayList<>();
                                    for (RoadNode n : findWayByID.get(s).getNodes()) {
                                        Location pointLocation = new Location(n.lat(), n.lon());
                                        locationList.add(pointLocation);
                                    }
                                    SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                                    marker.setColor(color(59, 130, 79));  // color green
                                    marker.setStrokeWeight(3);
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
                            marker.setStrokeWeight(3);
                            linesMarkers.add(marker);
                        }
                        map.addMarkers(linesMarkers);
                        break;
                    }
                    // ground truth mapping result
                    case 6: {
                        CSVMapReader csvGroundTruthMapReader = new CSVMapReader(ROOT_PATH + "groundTruth/map/");
                        RoadNetworkGraph groundTruthNetworkGraph = csvGroundTruthMapReader.readMap(0);
                        HashMap<String, RoadWay> idRoadWayMapping = new HashMap<>();
                        for (RoadWay w : groundTruthNetworkGraph.getWays()) {
                            idRoadWayMapping.put(w.getId(), w);
                        }
                        CSVTrajectoryReader groundTruthMatchingResultReader = new CSVTrajectoryReader();
                        List<Pair<Integer, List<String>>> gtMatchingResult = groundTruthMatchingResultReader.readGroundTruthMatchingResult(ROOT_PATH + "groundTruth/matchingResult/");
                        Map<Integer, List<String>> matchingResultList = new HashMap<>();
                        List<Marker> linesMarkers = new ArrayList<>();
                        for (Pair<Integer, List<String>> r : gtMatchingResult) {
                            matchingResultList.put(r._1(), r._2());
                        }
                        for (String s : trajectoryID) {
                            for (String id : matchingResultList.get(Integer.parseInt(s))) {
                                if (idRoadWayMapping.containsKey(id)) {
                                    List<Location> locationList = new ArrayList<>();
                                    for (RoadNode n : idRoadWayMapping.get(id).getNodes()) {
                                        Location pointLocation = new Location(n.lat(), n.lon());
                                        locationList.add(pointLocation);
                                    }
                                    SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                                    marker.setColor(color(0, 255, 255));  // color light blue
                                    marker.setStrokeWeight(3);
                                    linesMarkers.add(marker);
                                }
                            }
                        }
                        map.addMarkers(linesMarkers);
                        break;
                    }
                    // map comparison
                    case 7: {
                        RawMapReader newMapReader = new RawMapReader(RAW_MAP, BOUNDING_BOX);
                        RoadNetworkGraph newMap = newMapReader.readNewBeijingMap();
                        RawMapReader oldMapReader = new RawMapReader(RAW_MAP, BOUNDING_BOX);
                        RoadNetworkGraph oldMap = oldMapReader.readOldBeijingMap();

                        List<Marker> linesMarkers = new ArrayList<>();
                        for (RoadWay w : newMap.getWays()) {
                            List<Location> locationList = new ArrayList<>();
                            for (RoadNode n : w.getNodes()) {
                                Location pointLocation = new Location(n.lat(), n.lon());
                                locationList.add(pointLocation);
                            }
                            SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                            marker.setColor(color(255, 102, 178));  // color pink
                            marker.setStrokeWeight(3);
                            linesMarkers.add(marker);
                        }
                        for (RoadWay w : oldMap.getWays()) {
                            List<Location> locationList = new ArrayList<>();
                            for (RoadNode n : w.getNodes()) {
                                Location pointLocation = new Location(n.lat(), n.lon());
                                locationList.add(pointLocation);
                            }
                            SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                            marker.setColor(color(102, 255, 178));  // color green
                            marker.setStrokeWeight(3);
                            linesMarkers.add(marker);
                        }
                        map.addMarkers(linesMarkers);
                        break;
                    }
                    // inferred map
                    case 8: {
                        CSVMapReader csvInferredMapReader = new CSVMapReader(ROOT_PATH + "mapInference/");
                        List<RoadWay> inferredMap = csvInferredMapReader.readInferredEdges();
                        List<Marker> linesMarkers = new ArrayList<>();
                        for (RoadWay w : inferredMap) {
                            List<Location> locationList = new ArrayList<>();
                            for (RoadNode n : w.getNodes()) {
                                Location pointLocation = new Location(n.lat(), n.lon());
                                locationList.add(pointLocation);
                            }
                            SimpleLinesMarker marker = new SimpleLinesMarker(locationList);
                            marker.setColor(color(255, 255, 255));  // color purple
                            marker.setStrokeWeight(3);
                            linesMarkers.add(marker);
                        }
                        map.addMarkers(linesMarkers);
                        break;
                    }

                    default:
                        System.out.println("Error display option:" + this.options);
                }
            }
        } catch (
                IOException e) {
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