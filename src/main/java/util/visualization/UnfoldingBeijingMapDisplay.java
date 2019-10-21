package util.visualization;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.marker.SimplePointMarker;
import de.fhpotsdam.unfolding.providers.Microsoft;
import de.fhpotsdam.unfolding.utils.MapUtils;
import processing.core.PApplet;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.function.SpatialUtils;
import util.io.MapReader;
import util.io.MatchResultReader;
import util.io.TrajectoryReader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Segment;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.Pair;
import util.object.structure.SimpleTrajectoryMatchResult;

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
public class UnfoldingBeijingMapDisplay extends PApplet {
    private UnfoldingMap currMapDisplay;
    private String inputMapFolder = "/Users/macbookpro/Desktop/capstone/Beijing-S/input/map/";
    private String inputTrajPath = "/Users/macbookpro/Desktop/capstone/Beijing-S/input/trajectory/L180_I120_N-1/";
    private String outputRouteMatchResultPath =
            "/Users/macbookpro/Desktop/capstone/Beijing-S/output/matchResult/L180_I120_N-1/";
    private String gtRouteMatchResultPath = "/Users/macbookpro/Desktop/capstone/Beijing-S/groundTruth/matchResult/route/L180_I120_N-1/";
    //	private String inputMapFolder = "C:/data/Beijing-L/input/map/";
//	private String inputTrajPath = "C:/data/db/";
    private DistanceFunction distFunc = new GreatCircleDistanceFunction();
    private UnfoldingMap fullMapDisplay;    // full map visualization
//	private UnfoldingMap compareMapDisplay;    // map for comparison

    public void setup() {
        size(1440, 990);
//		this.fullMapDisplay = new UnfoldingMap(this, new OpenStreetMap.OpenStreetMapProvider());
//        this.fullMapDisplay = new UnfoldingMap(this, new BlankMap.BlankProvider());
        this.fullMapDisplay = new UnfoldingMap(this, new Microsoft.HybridProvider());
//		this.fullMapDisplay = new UnfoldingMap(this, new Google.GoogleMapProvider());
//		this.compareMapDisplay = new UnfoldingMap(this, new Microsoft.HybridProvider());
        MapUtils.createMouseEventDispatcher(this, fullMapDisplay);
//		MapUtils.createMouseEventDispatcher(this, compareMapDisplay);

        int[] blue = {0, 128, 255};
        int[] green = {102, 255, 178};
        int[] red = {255, 0, 0};
        int[] lightPurple = {255, 0, 255};
        int[] pink = {255, 153, 153};
        int[] black = {0, 0, 0};
        int[] grey = {220, 220, 220};

        // read the complete map, fill into fullMapDisplay
        RoadNetworkGraph rawMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
//		SpatialUtils.convertMapGCJ2WGS(rawMap);
//		SpatialUtils.convertMapUTM2WGS(rawMap, 50, 'S');
        Map<String, RoadWay> id2WayMap = new HashMap<>();
        for (RoadWay way : rawMap.getWays()) {
            if (!id2WayMap.containsKey(way.getID())) {
                id2WayMap.put(way.getID(), way);
            } else
                throw new IllegalArgumentException("Two roads has the same id:" + way.getID());
        }
        List<Marker> mapMarker = roadWayMarkerBeijingGen(rawMap.getWays(), grey, 2, false);
        fullMapDisplay.addMarkers(mapMarker);

//		RoadNetworkGraph planarRawMap = rawMap.toPlanarMap();
//		List<Marker> compareMapMarker = roadWayMarkerBeijingGen(planarRawMap.getWays(), pink, 2);
//		compareMapDisplay.addMarkers(compareMapMarker);

        // set map centre
        Location mapCenter = new Location((float) (rawMap.getMaxLat() + rawMap.getMinLat()) / 2, (float) (rawMap
                .getMaxLon() + rawMap.getMinLon()) / 2);
        fullMapDisplay.zoomAndPanTo(14, mapCenter);
//		compareMapDisplay.zoomAndPanTo(14, mapCenter);

        List<Trajectory> trajectoryList = TrajectoryReader.readTrajectoriesToList(inputTrajPath, 1, distFunc);
        List<Pair<Integer, List<String>>> gtRouteMatchResultList = MatchResultReader.readRouteMatchResults(gtRouteMatchResultPath);
        List<SimpleTrajectoryMatchResult> routeMatchResultList = MatchResultReader.readSimpleMatchResultsToList(outputRouteMatchResultPath, distFunc);

        Map<Integer, Trajectory> trajectoryMap = new HashMap<>();
        for (Trajectory trajectory : trajectoryList) {
//			List<Marker> trajMarker = trajMarkerBeijingGen(trajectory, red, 2);
            trajectoryMap.put(Integer.parseInt(trajectory.getID()), trajectory);
        }

        Map<Integer, List<String>> gtRouteMatchResultMap = new HashMap<>();
        for (Pair<Integer, List<String>> gtRoute : gtRouteMatchResultList) {
            gtRouteMatchResultMap.put(gtRoute._1(), gtRoute._2());
        }

        Map<Integer, List<String>> routeMatchResultMap = new HashMap<>();
        for (SimpleTrajectoryMatchResult simpleTrajectoryMatchResult : routeMatchResultList) {
            if (simpleTrajectoryMatchResult.getTrajID().equals("55")) {
                routeMatchResultMap.put(Integer.parseInt(simpleTrajectoryMatchResult.getTrajID()), simpleTrajectoryMatchResult.getRouteMatchResultList());
                break;
            }
        }

        for (int index : routeMatchResultMap.keySet()) {
            Trajectory currTraj = trajectoryMap.get(index);
            List<Marker> trajMarker = trajMarkerBeijingGen(currTraj, red, 2);
//			fullMapDisplay.addMarkers(trajMarker);

            Pair<Integer, List<String>> currOutputRouteMatchResult = new Pair<>(index, routeMatchResultMap.get(index));

            Pair<Integer, List<String>> currGTRouteMatchResult = new Pair<>(index, gtRouteMatchResultMap.get(index));
            List<RoadWay> gtWayList = new ArrayList<>();
            for (String wayID : currGTRouteMatchResult._2()) {
                if (id2WayMap.containsKey(wayID)) {
                    gtWayList.add(id2WayMap.get(wayID));
                }
            }
            List<RoadWay> outPutList = new ArrayList<>();
            for (String wayID : currOutputRouteMatchResult._2()) {
                if (id2WayMap.containsKey(wayID)) {
                    outPutList.add(id2WayMap.get(wayID));
                }
            }
            List<Marker> outputWayMarker = roadWayMarkerBeijingGen(outPutList, blue, 2, false);
            List<Marker> gtWayMarker = roadWayMarkerBeijingGen(gtWayList, green, 2, false);
//			fullMapDisplay.addMarkers(outputWayMarker);
//			fullMapDisplay.addMarkers(gtWayMarker);

            SimplePointMarker center = new SimplePointMarker(new Location(39.96908, 116.40697));
            center.setColor(color(red[0], red[1], red[2]));

            List<Marker> neighborRoads = new ArrayList<>();
            neighborRoads.add(new SimpleLinesMarker(segmentToLocations(id2WayMap.get("685406").getEdges().get(0))));
            neighborRoads.add(new SimpleLinesMarker(segmentToLocations(id2WayMap.get("675348").getEdges().get(1))));
            neighborRoads.add(new SimpleLinesMarker(segmentToLocations(id2WayMap.get("675348").getEdges().get(2))));
            neighborRoads.add(new SimpleLinesMarker(segmentToLocations(id2WayMap.get("675348").getEdges().get(0))));
            neighborRoads.add(new SimpleLinesMarker(segmentToLocations(id2WayMap.get("680261").getEdges().get(1))));
            neighborRoads.add(new SimpleLinesMarker(segmentToLocations(id2WayMap.get("680261").getEdges().get(2))));
            neighborRoads.add(new SimpleLinesMarker(segmentToLocations(id2WayMap.get("13248219").getEdges().get(2))));
            neighborRoads.add(new SimpleLinesMarker(segmentToLocations(id2WayMap.get("13248219").getEdges().get(1))));
            neighborRoads.add(new SimpleLinesMarker(segmentToLocations(id2WayMap.get("13248220").getEdges().get(0))));
            neighborRoads.add(new SimpleLinesMarker(segmentToLocations(id2WayMap.get("685404").getEdges().get(0))));
            neighborRoads.add(new SimpleLinesMarker(segmentToLocations(id2WayMap.get("13248219").getEdges().get(0))));
            neighborRoads.add(new SimpleLinesMarker(segmentToLocations(id2WayMap.get("13248220").getEdges().get(1))));
            for (Marker neighborRoad : neighborRoads) {
                neighborRoad.setColor(color(green[0], green[1], green[2]));
            }
            fullMapDisplay.addMarkers(center);
            fullMapDisplay.addMarkers(neighborRoads);
        }
        currMapDisplay = fullMapDisplay;
    }

    private List<Marker> roadWayMarkerBeijingGen(List<RoadWay> w, int[] color, int strokeWeight, boolean isPointRequired) {
        List<Marker> result = new ArrayList<>();
        for (RoadWay currWay : w) {
            List<Location> locationList = new ArrayList<>();
            for (RoadNode n : currWay.getNodes()) {
                Pair<Double, Double> currPoint = SpatialUtils.convertGCJ2WGS(n.lon(), n.lat());        // for map provider other than Google
                Location pointLocation = new Location(currPoint._2(), currPoint._1());
//				Location pointLocation = new Location(n.lat(), n.lon());
                if (isPointRequired) {
                    SimplePointMarker currPointMarker = new SimplePointMarker(pointLocation);
                    currPointMarker.setStrokeColor(color(color[0], color[1], color[2]));
                    currPointMarker.setColor(color(color[0], color[1], color[2]));
                    currPointMarker.setStrokeWeight(strokeWeight);
                    result.add(currPointMarker);
                }
                locationList.add(pointLocation);
            }
            SimpleLinesMarker currLineMarker = new SimpleLinesMarker(locationList);
            currLineMarker.setColor(color(color[0], color[1], color[2]));
            currLineMarker.setStrokeWeight(strokeWeight);
            result.add(currLineMarker);
        }
        return result;
    }

    private List<Marker> trajMarkerBeijingGen(Trajectory traj, int[] color, int strokeWeight) {
        List<Marker> result = new ArrayList<>();
        List<Location> locationList = new ArrayList<>();
        for (TrajectoryPoint n : traj.getSTPoints()) {
            Pair<Double, Double> currPoint = SpatialUtils.convertGCJ2WGS(n.x(), n.y());
            Location pointLocation = new Location(currPoint._2(), currPoint._1());
//			Location pointLocation = new Location(n.y(), n.x());
            SimplePointMarker currPointMarker = new SimplePointMarker(pointLocation);
            currPointMarker.setStrokeColor(color(color[0], color[1], color[2]));
            currPointMarker.setColor(color(color[0], color[1], color[2]));
            currPointMarker.setStrokeWeight(strokeWeight);
            result.add(currPointMarker);
            locationList.add(pointLocation);
        }
        SimpleLinesMarker currLineMarker = new SimpleLinesMarker(locationList);
        currLineMarker.setColor(color(color[0], color[1], color[2]));
        currLineMarker.setStrokeWeight(strokeWeight);
        result.add(currLineMarker);

        return result;
    }
//
//	public void keyPressed() {
//		switch (key) {
//			case '1': {
//				currMapDisplay = fullMapDisplay;
//				break;
//			}
//			case '2': {
//				currMapDisplay = compareMapDisplay;
//				break;
//			}
//			default:
//				break;
//		}
//	}

    public void draw() {
        currMapDisplay.draw();
    }

    private List<Location> segmentToLocations(Segment segment) {
        List<Location> locations = new ArrayList<>();
        Location s = new Location(segment.y1(), segment.x1());
        Location t = new Location(segment.y2(), segment.x2());
        locations.add(s);
        locations.add(t);
        return locations;
    }

    public void display() {
        PApplet.main(new String[]{this.getClass().getName()});
    }

}