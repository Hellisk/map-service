package edu.uq.dke.mapupdate.visualisation;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.providers.Microsoft;
import de.fhpotsdam.unfolding.providers.OpenMapSurferProvider;
import de.fhpotsdam.unfolding.providers.OpenStreetMap;
import de.fhpotsdam.unfolding.utils.MapUtils;
import edu.uq.dke.mapupdate.util.io.CSVMapReader;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNode;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadWay;
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
public class UnfoldingMapDisplay extends PApplet {

    private UnfoldingMap currMapDisplay;
    private UnfoldingMap fullMapDisplay;    // full map visualization
    private UnfoldingMap[] mapDisplay = new UnfoldingMap[10];   // specified type or level of map

    public void setup() {
        int OPTION = 1;
        int specifiedTypeSeries = 0;
        size(1760, 990, JAVA2D);
        this.fullMapDisplay = new UnfoldingMap(this, new OpenStreetMap.OpenStreetMapProvider());
        this.fullMapDisplay = new UnfoldingMap(this, new BlankMap.BlankProvider());
//        this.fullMapDisplay = new UnfoldingMap(this, new Google.GoogleMapProvider());
        MapUtils.createMouseEventDispatcher(this, fullMapDisplay);
        for (int i = 0; i < mapDisplay.length; i++) {
            this.mapDisplay[i] = new UnfoldingMap(this, new BlankMap.BlankProvider());
            MapUtils.createMouseEventDispatcher(this, this.mapDisplay[i]);
        }

        try {
            // read the complete map, fill into fullMapDisplay
            CSVMapReader csvRawMapReader = new CSVMapReader(INPUT_MAP);
            RoadNetworkGraph roadNetworkGraph = csvRawMapReader.readMap(0, -1, false);
            List<Marker> weightedMapMarker = weightedRoadWayMarkerGen(roadNetworkGraph.getWays(), true);
            fullMapDisplay.addMarkers(weightedMapMarker);

            // location in beijing
            Location mapCenter = new Location((float) (roadNetworkGraph.getMaxLat() + roadNetworkGraph.getMinLat()) / 2, (float) (roadNetworkGraph
                    .getMaxLon() + roadNetworkGraph.getMinLon()) / 2);
            fullMapDisplay.zoomAndPanTo(14, mapCenter);
            for (UnfoldingMap mapDisplay : mapDisplay)
                mapDisplay.zoomAndPanTo(14, mapCenter);
            // 1 = level comparison, 2 = type comparison
            switch (OPTION) {
                case 1: {
                    // read map by specified level
                    for (int i = 0; i < mapDisplay.length; i++) {
                        CSVMapReader csvMapLevelReader = new CSVMapReader(INPUT_MAP);
                        List<RoadWay> roadWayLevelList = csvMapLevelReader.readMapEdgeByLevel(0, i);
                        List<Marker> weightedLevelLineMarkers = weightedRoadWayMarkerGen(roadWayLevelList, false);
                        mapDisplay[i].addMarkers(weightedLevelLineMarkers);
                    }
                    break;
                }
                case 2: {
                    for (int i = specifiedTypeSeries; i < specifiedTypeSeries + 10; i++) {
                        CSVMapReader csvMapTypeReader = new CSVMapReader(INPUT_MAP);
                        List<RoadWay> roadWayTypeList = csvMapTypeReader.readMapEdgeByType(0, i);
                        List<Marker> weightedTypeLineMarkers = weightedRoadWayMarkerGen(roadWayTypeList, false);
                        mapDisplay[i].addMarkers(weightedTypeLineMarkers);
                    }
                    break;
                }
                default: {
                    System.out.println("ERROR! Incorrect visualization option: " + OPTION);
                    break;
                }
            }
            currMapDisplay = fullMapDisplay;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private List<Marker> weightedRoadWayMarkerGen(List<RoadWay> w, boolean statistic) {
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
                if (statistic && prevVisitCount == 0)
                    System.out.println("The current map contains " + processedRoadSize + " unvisited roads, which accounts for " +
                            processedRoadSize / (double) w.size() * 100 + "% percentage");
                currColor = colorGradeSize == 0 ? 0 : processedRoadSize / colorGradeSize;
            } else if (currWay.getVisitCount() == prevVisitCount) {
                currColor = lastColor;
            } else System.out.println("ERROR! The visit count should not decrease.");
            currLineMarker.setColor(color(255, 255 - currColor, 0));
//            currLineMarker.setColor(color(192, 192, 192));
            currLineMarker.setStrokeWeight(1);
            prevVisitCount = currWay.getVisitCount();
            lastColor = currColor;
            processedRoadSize++;
            result.add(currLineMarker);
        }
        return result;
    }

    public void keyPressed() {
        switch (key) {
            case '1': {
                currMapDisplay = mapDisplay[0];
                break;
            }
            case '2': {
                currMapDisplay = mapDisplay[1];
                break;
            }
            case '3': {
                currMapDisplay = mapDisplay[2];
                break;
            }
            case '4': {
                currMapDisplay = mapDisplay[3];
                break;
            }
            case '5': {
                currMapDisplay = mapDisplay[4];
                break;
            }
            case '6': {
                currMapDisplay = mapDisplay[5];
                break;
            }
            case '7': {
                currMapDisplay = mapDisplay[6];
                break;
            }
            case '8': {
                currMapDisplay = mapDisplay[7];
                break;
            }
            case '9': {
                currMapDisplay = mapDisplay[8];
                break;
            }
            case '0': {
                currMapDisplay = mapDisplay[9];
                break;
            }
            case '-': {
                currMapDisplay = fullMapDisplay;
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