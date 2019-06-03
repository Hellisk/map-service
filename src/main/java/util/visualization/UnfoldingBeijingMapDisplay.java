package util.visualization;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.providers.Microsoft;
import de.fhpotsdam.unfolding.utils.MapUtils;
import processing.core.PApplet;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.function.SpatialUtils;
import util.io.MapReader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;
import util.object.structure.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple Unfolding map app demo.
 *
 * @author uqdalves
 */
//http://unfoldingmaps.org/
public class UnfoldingBeijingMapDisplay extends PApplet {
	
	private UnfoldingMap currMapDisplay;
	private String inputMapFolder = "F:/data/Beijing-S/input/map/";
	private String inputTrajPath = "F:/data/Beijing-S/input/trajectory/L-1_I-1_N-1/";
	private DistanceFunction distFunc = new GreatCircleDistanceFunction();
	private UnfoldingMap fullMapDisplay;    // full map visualization
//	private UnfoldingMap[] mapDisplay = new UnfoldingMap[3];   // specified type or level of map
	
	public void setup() {
		size(1440, 990);
//		this.fullMapDisplay = new UnfoldingMap(this, new OpenStreetMap.OpenStreetMapProvider());
//		this.fullMapDisplay = new UnfoldingMap(this, new BlankMap.BlankProvider());
		this.fullMapDisplay = new UnfoldingMap(this, new Microsoft.HybridProvider());
		MapUtils.createMouseEventDispatcher(this, fullMapDisplay);
//		for (int i = 0; i < mapDisplay.length; i++) {
//			this.mapDisplay[i] = new UnfoldingMap(this, new BlankMap.BlankProvider());
//			MapUtils.createMouseEventDispatcher(this, this.mapDisplay[i]);
//		}
		
		int[] blue = {0, 128, 255};
		int[] green = {102, 255, 178};
		int[] red = {255, 0, 0};
		int[] lightPurple = {255, 0, 255};
		int[] pink = {255, 153, 153};
		int[] black = {0, 0, 0};
		
		// read the complete map, fill into fullMapDisplay
		RoadNetworkGraph roadNetworkGraph = MapReader.readMap(inputMapFolder + "0.txt", false, distFunc);
		List<Marker> mapMarker = roadWayMarkerBeijingGen(roadNetworkGraph.getWays(), lightPurple, 2);
		fullMapDisplay.addMarkers(mapMarker);
		// set map centre
		Location mapCenter = new Location((float) (roadNetworkGraph.getMaxLat() + roadNetworkGraph.getMinLat()) / 2, (float) (roadNetworkGraph
				.getMaxLon() + roadNetworkGraph.getMinLon()) / 2);
		fullMapDisplay.zoomAndPanTo(14, mapCenter);
//		for (UnfoldingMap mapDisplay : mapDisplay)
//			mapDisplay.zoomAndPanTo(14, mapCenter);

//		List<Trajectory> trajectoryList = TrajectoryReader.readTrajectoriesToList(inputTrajPath, distFunc);
//		int count = 0;
//		for (Trajectory traj : trajectoryList) {
//			if (count >= 10)
//				break;
//			List<Marker> trajMarker = trajMarkerBeijingGen(traj, red, 2);
//			fullMapDisplay.addMarkers(trajMarker);
//			count++;
//		}
		currMapDisplay = fullMapDisplay;
		
	}
	
	private List<Marker> roadWayMarkerBeijingGen(List<RoadWay> w, int[] color, int strokeWeight) {
		List<Marker> result = new ArrayList<>();
		for (RoadWay currWay : w) {
			List<Location> locationList = new ArrayList<>();
			for (RoadNode n : currWay.getNodes()) {
				Pair<Double, Double> currPoint = SpatialUtils.convertGCJ2WGS(n.lon(), n.lat());        // for map provider other than Google
//				Pair<Double, Double> currPoint = new Pair<>(n.lon(), n.lat());
				Location pointLocation = new Location(currPoint._2(), currPoint._1());
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
//			Pair<Double, Double> currPoint = new Pair<>(n.x(), n.y());
			Location pointLocation = new Location(currPoint._2(), currPoint._1());
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
//				currMapDisplay = mapDisplay[0];
//				break;
//			}
//			case '2': {
//				currMapDisplay = mapDisplay[1];
//				break;
//			}
//			case '3': {
//				currMapDisplay = mapDisplay[2];
//				break;
//			}
//			default:
//				break;
//		}
//	}
	
	public void draw() {
		currMapDisplay.draw();
//		for (UnfoldingMap unfoldingMap : mapDisplay) {
//			unfoldingMap.draw();
//		}
	}
	
	public void display() {
		PApplet.main(new String[]{this.getClass().getName()});
	}
	
}