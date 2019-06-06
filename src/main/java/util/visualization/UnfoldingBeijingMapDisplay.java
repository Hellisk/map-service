package util.visualization;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.providers.Microsoft;
import de.fhpotsdam.unfolding.utils.MapUtils;
import processing.core.PApplet;
import util.function.DistanceFunction;
import util.function.EuclideanDistanceFunction;
import util.function.SpatialUtils;
import util.io.MapReader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;

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
	private String inputMapFolder = "C:/data/Beijing-S/output/map/";
	//	private String inputTrajPath = "F:/data/Beijing-S/input/trajectory/L180_I-1_N5000S/";
//	private String gtMatchResultPath = "F:/data/Beijing-S/groundTruth/matchResult/L180_I-1_N5000/";
	private DistanceFunction distFunc = new EuclideanDistanceFunction();
	private UnfoldingMap fullMapDisplay;    // full map visualization
	private UnfoldingMap compareMapDisplay;    // map for comparison
	
	public void setup() {
		size(1440, 990);
//		this.fullMapDisplay = new UnfoldingMap(this, new OpenStreetMap.OpenStreetMapProvider());
//		this.fullMapDisplay = new UnfoldingMap(this, new BlankMap.BlankProvider());
		this.fullMapDisplay = new UnfoldingMap(this, new Microsoft.HybridProvider());
		this.compareMapDisplay = new UnfoldingMap(this, new Microsoft.HybridProvider());
		MapUtils.createMouseEventDispatcher(this, fullMapDisplay);
		MapUtils.createMouseEventDispatcher(this, compareMapDisplay);
		
		int[] blue = {0, 128, 255};
		int[] green = {102, 255, 178};
		int[] red = {255, 0, 0};
		int[] lightPurple = {255, 0, 255};
		int[] pink = {255, 153, 153};
		int[] black = {0, 0, 0};
		
		// read the complete map, fill into fullMapDisplay
		RoadNetworkGraph rawMap = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
//		SpatialUtils.convertMapGCJ2WGS(rawMap);
		SpatialUtils.convertMapUTM2WGS(rawMap, 50, 'S');
		Map<String, RoadWay> id2WayMap = new HashMap<>();
		for (RoadWay way : rawMap.getWays()) {
			if (!id2WayMap.containsKey(way.getID())) {
				id2WayMap.put(way.getID(), way);
			} else
				throw new IllegalArgumentException("Two roads has the same id:" + way.getID());
		}
		List<Marker> mapMarker = roadWayMarkerBeijingGen(rawMap.getWays(), lightPurple, 2);
		fullMapDisplay.addMarkers(mapMarker);
		
		RoadNetworkGraph planarRawMap = rawMap.toPlanarMap();
		List<Marker> compareMapMarker = roadWayMarkerBeijingGen(planarRawMap.getWays(), pink, 2);
		compareMapDisplay.addMarkers(compareMapMarker);
		
		// set map centre
		Location mapCenter = new Location((float) (rawMap.getMaxLat() + rawMap.getMinLat()) / 2, (float) (rawMap
				.getMaxLon() + rawMap.getMinLon()) / 2);
		fullMapDisplay.zoomAndPanTo(14, mapCenter);
		compareMapDisplay.zoomAndPanTo(14, mapCenter);

//		List<Trajectory> trajectoryList = TrajectoryReader.readTrajectoriesToList(inputTrajPath, distFunc);
//		List<Pair<Integer, List<String>>> gtMatchResultList = MatchResultReader.readRouteMatchResults(gtMatchResultPath);
//		Map<Integer, List<String>> id2GTRouteMap = new HashMap<>();
//		for (Pair<Integer, List<String>> routePair : gtMatchResultList) {
//			if (!id2GTRouteMap.containsKey(routePair._1())) {
//				id2GTRouteMap.put(routePair._1(), routePair._2());
//			} else
//				throw new IllegalArgumentException("Two match results have the same id:" + routePair._1());
//		}
//		int count = 0;
//		for (Trajectory traj : trajectoryList) {
//			if (count >= 10)
//				break;
//			List<Marker> trajMarker = trajMarkerBeijingGen(traj, red, 2);
//			if(!id2GTRouteMap.containsKey(Integer.parseInt(traj.getID())))
//				throw new IllegalArgumentException("The trajectory id does not appear in the ground-truth:" + traj.getID());
//			List<RoadWay> gtWayList = new ArrayList<>();
//			for (String wayID : id2GTRouteMap.get(Integer.parseInt(traj.getID()))) {
//				if(id2WayMap.containsKey(wayID)) {
//					gtWayList.add(id2WayMap.get(wayID));
//				}
//			}
//			List<Marker> gtWayMarker = roadWayMarkerBeijingGen(gtWayList,green,2);
//			fullMapDisplay.addMarkers(trajMarker);
//			fullMapDisplay.addMarkers(gtWayMarker);
//			count++;
//		}
		currMapDisplay = fullMapDisplay;
	}
	
	private List<Marker> roadWayMarkerBeijingGen(List<RoadWay> w, int[] color, int strokeWeight) {
		List<Marker> result = new ArrayList<>();
		for (RoadWay currWay : w) {
			List<Location> locationList = new ArrayList<>();
			for (RoadNode n : currWay.getNodes()) {
//				Pair<Double, Double> currPoint = SpatialUtils.convertGCJ2WGS(n.lon(), n.lat());        // for map provider other than Google
//				Pair<Double, Double> currPoint = new Pair<>(n.lon(), n.lat());
				Location pointLocation = new Location(n.lat(), n.lon());
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
//			Pair<Double, Double> currPoint = SpatialUtils.convertGCJ2WGS(n.x(), n.y());
//			Pair<Double, Double> currPoint = new Pair<>(n.x(), n.y());
			Location pointLocation = new Location(n.y(), n.x());
			locationList.add(pointLocation);
		}
		SimpleLinesMarker currLineMarker = new SimpleLinesMarker(locationList);
		currLineMarker.setColor(color(color[0], color[1], color[2]));
		currLineMarker.setStrokeWeight(strokeWeight);
		result.add(currLineMarker);
		return result;
	}
	
	public void keyPressed() {
		switch (key) {
			case '1': {
				currMapDisplay = fullMapDisplay;
				break;
			}
			case '2': {
				currMapDisplay = compareMapDisplay;
				break;
			}
			default:
				break;
		}
	}
	
	public void draw() {
		currMapDisplay.draw();
	}
	
	public void display() {
		PApplet.main(new String[]{this.getClass().getName()});
	}
	
}