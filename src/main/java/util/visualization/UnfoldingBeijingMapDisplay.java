package util.visualization;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.marker.SimplePointMarker;
import de.fhpotsdam.unfolding.providers.Microsoft;
import de.fhpotsdam.unfolding.utils.MapUtils;
import processing.core.PApplet;
import util.dijkstra.RoutingGraph;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.function.SpatialUtils;
import util.index.rtree.RTreeIndexing;
import util.io.MapReader;
import util.io.MatchResultReader;
import util.io.TrajectoryReader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.*;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.object.structure.SimpleTrajectoryMatchResult;
import util.settings.MapMatchingProperty;

import java.util.*;

/**
 * Simple Unfolding map app demo.
 *
 * @author uqdalves
 */
//http://unfoldingmaps.org/
public class UnfoldingBeijingMapDisplay extends PApplet {
	private UnfoldingMap currMapDisplay;
	//    private String inputMapFolder = "/Users/macbookpro/Desktop/capstone/Beijing-S/input/map/";
//    private String inputTrajPath = "/Users/macbookpro/Desktop/capstone/Beijing-S/input/trajectory/L180_I120_N-1/";
//    private String outputRouteMatchResultPath =
//            "/Users/macbookpro/Desktop/capstone/Beijing-S/output/matchResult/L180_I120_N-1/";
//    private String gtRouteMatchResultPath = "/Users/macbookpro/Desktop/capstone/Beijing-S/groundTruth/matchResult/route/L180_I120_N-1/";
	private String inputMapFolder = "C:/data/Beijing-S/input/map/";
	private String inputTrajPath = "C:/data/Beijing-S/input/trajectory/L180_I120_N1/";
	private String outputRouteMatchResultPath = "C:/data/Beijing-S/output/matchResult/L180_I120_N-1/";
	private String gtRouteMatchResultPath = "C:/data/Beijing-S/groundTruth/matchResult/route/L180_I120_N-1/";
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
			if (simpleTrajectoryMatchResult.getTrajID().equals("50")) {
				routeMatchResultMap.put(Integer.parseInt(simpleTrajectoryMatchResult.getTrajID()), simpleTrajectoryMatchResult.getRouteMatchResultList());
				break;
			}
		}
		
		for (int index : routeMatchResultMap.keySet()) {
			Trajectory currTraj = trajectoryMap.get(index);
			List<Marker> trajMarker = trajMarkerBeijingGen(currTraj, red, 2);
			fullMapDisplay.addMarkers(trajMarker);
			
			Pair<Integer, List<String>> currOutputRouteMatchResult = new Pair<>(index, routeMatchResultMap.get(index));
			
			Pair<Integer, List<String>> currGTRouteMatchResult = new Pair<>(index, gtRouteMatchResultMap.get(index));
			List<RoadWay> gtWayList = new ArrayList<>();
			for (String wayID : currGTRouteMatchResult._2()) {
				if (rawMap.containsWay(wayID)) {
					gtWayList.add(rawMap.getWayByID(wayID));
				}
			}
			List<RoadWay> outPutList = new ArrayList<>();
			for (String wayID : currOutputRouteMatchResult._2()) {
				if (rawMap.containsWay(wayID)) {
					outPutList.add(rawMap.getWayByID(wayID));
				}
			}
			List<Marker> outputWayMarker = roadWayMarkerBeijingGen(outPutList, blue, 2, false);
			List<Marker> gtWayMarker = roadWayMarkerBeijingGen(gtWayList, green, 2, false);
//			fullMapDisplay.addMarkers(outputWayMarker);
//			fullMapDisplay.addMarkers(gtWayMarker);

//			indexSearchTest(red, green, rawMap);
//			shortestPathSearchTest(rawMap, black, pink, red, green);
		}
		currMapDisplay = fullMapDisplay;
	}
	
	private void shortestPathSearchTest(RoadNetworkGraph roadMap, int[] sourceStartColor, int[] directionStartColor,
										int[] directionEndColor, int[] routeColor) {
		int destCount = 5;
		RoutingGraph routingGraph = new RoutingGraph(roadMap, false, new MapMatchingProperty());
		Random random = new Random();
		List<PointMatch> destPointList = new ArrayList<>();
		RoadWay startWay = roadMap.getWays().get(random.nextInt(roadMap.getWays().size()));
		Point startPoint = new Point((startWay.getNode(0).lon() + startWay.getNode(1).lon()) / 2,
				(startWay.getNode(0).lat() + startWay.getNode(1).lat()) / 2, distFunc);
		Point startDirectionPoint = new Point(startWay.getNode(1).lon(), startWay.getNode(1).lat(), distFunc);
		PointMatch startMatch = new PointMatch(startPoint, startWay.getEdges().get(0), startWay.getID() + "|0");
		
		Pair<Double, Double> startCoordinate = SpatialUtils.convertGCJ2WGS(startPoint.x(), startPoint.y());
		Pair<Double, Double> startDirectionCoordinate = SpatialUtils.convertGCJ2WGS(startDirectionPoint.x(), startDirectionPoint.y());
		Location startLocation = new Location(startCoordinate._2(), startCoordinate._1());
		Location startDirectionLocation = new Location(startDirectionCoordinate._2(), startDirectionCoordinate._1());
//			Location startLocation = new Location(n.y(), n.x());
		SimplePointMarker startPointMarker = new SimplePointMarker(startLocation);
		startPointMarker.setStrokeColor(color(sourceStartColor[0], sourceStartColor[1], sourceStartColor[2]));
		startPointMarker.setColor(color(sourceStartColor[0], sourceStartColor[1], sourceStartColor[2]));
		startPointMarker.setStrokeWeight(5);
		fullMapDisplay.addMarker(startPointMarker);
		SimplePointMarker startDirectionPointMarker = new SimplePointMarker(startDirectionLocation);
		startDirectionPointMarker.setStrokeColor(color(directionEndColor[0], directionEndColor[1], directionEndColor[2]));
		startDirectionPointMarker.setColor(color(directionEndColor[0], directionEndColor[1], directionEndColor[2]));
		startDirectionPointMarker.setStrokeWeight(5);
		fullMapDisplay.addMarker(startDirectionPointMarker);
		
		double maxDistance = 0;
		for (int i = 0; i < destCount; i++) {
			RoadWay endWay = roadMap.getWays().get(random.nextInt(roadMap.getWays().size()));
			Point endPoint = new Point((endWay.getNode(0).lon() + endWay.getNode(1).lon()) / 2,
					(endWay.getNode(0).lat() + endWay.getNode(1).lat()) / 2, distFunc);
			Point endDirectionPoint = new Point(endWay.getNode(0).lon(), endWay.getNode(0).lat(), distFunc);
			Pair<Double, Double> endCoordinate = SpatialUtils.convertGCJ2WGS(endPoint.x(), endPoint.y());
			Pair<Double, Double> endDirectionCoordinate = SpatialUtils.convertGCJ2WGS(endDirectionPoint.x(), endDirectionPoint.y());
			Location endLocation = new Location(endCoordinate._2(), endCoordinate._1());
			Location endDirectionLocation = new Location(endDirectionCoordinate._2(), endDirectionCoordinate._1());
//			Location startLocation = new Location(n.y(), n.x());
			SimplePointMarker endPointMarker = new SimplePointMarker(endLocation);
			endPointMarker.setStrokeColor(color(directionEndColor[0], directionEndColor[1], directionEndColor[2]));
			endPointMarker.setColor(color(directionEndColor[0], directionEndColor[1], directionEndColor[2]));
			endPointMarker.setStrokeWeight(5);
			SimplePointMarker endDirectionPointMarker = new SimplePointMarker(endDirectionLocation);
			endDirectionPointMarker.setStrokeColor(color(directionStartColor[0], directionStartColor[1], directionStartColor[2]));
			endDirectionPointMarker.setColor(color(directionStartColor[0], directionStartColor[1], directionStartColor[2]));
			endDirectionPointMarker.setStrokeWeight(5);
			fullMapDisplay.addMarker(endDirectionPointMarker);
			fullMapDisplay.addMarker(endPointMarker);
			destPointList.add(new PointMatch(endPoint, endWay.getEdges().get(0), endWay.getID() + "|0"));
			maxDistance = Math.max(maxDistance, distFunc.distance(startPoint, endPoint) * 8);
		}
		
		List<Pair<Double, List<String>>> result = routingGraph.calculateOneToNDijkstraSP(startMatch, destPointList, maxDistance);
		List<RoadWay> route = new ArrayList<>();
		for (Pair<Double, List<String>> resultPair : result) {
			if (resultPair._2().size() == 0)
				System.out.println("The current road pair is not reachable.");
			List<String> strings = resultPair._2();
			for (String roadID : strings) {
				route.add(roadMap.getWayByID(roadID.split("\\|")[0]));
			}
			
		}
		fullMapDisplay.addMarkers(roadWayMarkerBeijingGen(route, routeColor, 2, true));
	}
	
	private void indexSearchTest(int[] pointColor, int[] candidateColor, RoadNetworkGraph roadMap) {
		RTreeIndexing rtree = new RTreeIndexing(roadMap);
		Random random = new Random();
		Rect boundingBox = roadMap.getBoundary();
		double radius = 141.4;
		double lonDiff = boundingBox.maxX() - boundingBox.minX();
		double latDiff = boundingBox.maxY() - boundingBox.minY();
//		double pointLon = random.nextDouble() * lonDiff + boundingBox.minX();
//		double pointLat = random.nextDouble() * latDiff + boundingBox.minY();
		double pointLon = 116.40888963156496;
		double pointLat = 39.959436736124815;
		System.out.println("Center location: " + pointLon + "," + pointLat);
		Pair<Double, Double> trajPoint = SpatialUtils.convertGCJ2WGS(pointLon, pointLat);
		SimplePointMarker center = new SimplePointMarker(new Location(trajPoint._2(), trajPoint._1()));
		center.setColor(color(pointColor[0], pointColor[1], pointColor[2]));
		List<Location> marginList = new ArrayList<>();
		marginList.add(new Location(center.getLocation().getLat() - distFunc.getCoordinateOffsetY(radius,
				(boundingBox.minX() + boundingBox.maxX()) / 2), center.getLocation().getLon() - distFunc.getCoordinateOffsetX(radius,
				(boundingBox.minY() + boundingBox.maxY()) / 2)));
		marginList.add(new Location(center.getLocation().getLat() + distFunc.getCoordinateOffsetY(radius,
				(boundingBox.minX() + boundingBox.maxX()) / 2), center.getLocation().getLon() - distFunc.getCoordinateOffsetX(radius,
				(boundingBox.minY() + boundingBox.maxY()) / 2)));
		marginList.add(new Location(center.getLocation().getLat() + distFunc.getCoordinateOffsetY(radius,
				(boundingBox.minX() + boundingBox.maxX()) / 2), center.getLocation().getLon() + distFunc.getCoordinateOffsetX(radius,
				(boundingBox.minY() + boundingBox.maxY()) / 2)));
		marginList.add(new Location(center.getLocation().getLat() - distFunc.getCoordinateOffsetY(radius,
				(boundingBox.minX() + boundingBox.maxX()) / 2), center.getLocation().getLon() + distFunc.getCoordinateOffsetX(radius,
				(boundingBox.minY() + boundingBox.maxY()) / 2)));
		marginList.add(new Location(center.getLocation().getLat() - distFunc.getCoordinateOffsetY(radius,
				(boundingBox.minX() + boundingBox.maxX()) / 2), center.getLocation().getLon() - distFunc.getCoordinateOffsetX(radius,
				(boundingBox.minY() + boundingBox.maxY()) / 2)));
		SimpleLinesMarker polygon = new SimpleLinesMarker(marginList);
		polygon.setColor(color(pointColor[0], pointColor[1], pointColor[2]));
		fullMapDisplay.addMarker(polygon);
		List<PointMatch> searchResult = rtree.searchNeighbours(new Point(pointLon, pointLat, distFunc), radius);
		
		List<Marker> neighborRoads = new ArrayList<>();
		for (PointMatch pointMatch : searchResult) {
			String[] roadIDInfo = pointMatch.getRoadID().split("\\|");
			if (roadMap.containsWay(roadIDInfo[0])) {
				Segment currSegment = roadMap.getWayByID(roadIDInfo[0]).getEdges().get(Integer.parseInt(roadIDInfo[1]));
				List<Location> segLocation = segmentToLocations(currSegment);
				SimpleLinesMarker currSegMarker = new SimpleLinesMarker(segLocation);
				currSegMarker.setColor(color(candidateColor[0], candidateColor[1], candidateColor[2]));
				neighborRoads.add(currSegMarker);
			}
		}
		
		fullMapDisplay.addMarkers(center);
		fullMapDisplay.addMarkers(neighborRoads);
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
					currPointMarker.setStrokeWeight(strokeWeight - 1);
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
//        Location s = new Location(segment.y1(), segment.x1());
//        Location t = new Location(segment.y2(), segment.x2());
		Pair<Double, Double> firstPoint = SpatialUtils.convertGCJ2WGS(segment.x1(), segment.y1());
		Location s = new Location(firstPoint._2(), firstPoint._1());
		Pair<Double, Double> secPoint = SpatialUtils.convertGCJ2WGS(segment.x2(), segment.y2());
		Location t = new Location(secPoint._2(), secPoint._1());
		locations.add(s);
		locations.add(t);
		return locations;
	}
	
	public void display() {
		PApplet.main(new String[]{this.getClass().getName()});
	}
	
}