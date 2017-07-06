package traminer.util.map;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.SimplePointMarker;
import de.fhpotsdam.unfolding.utils.MapUtils;
import edu.uq.dke.mapupdate.io.CSVMapReader;
import org.jdom2.JDOMException;
import processing.core.PApplet;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.map.roadnetwork.RoadNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple Unfolding map app demo.
 *
 * @author uqdalves
 */
//http://unfoldingmaps.org/
public class UnfoldingMapDemo extends PApplet {

    private UnfoldingMap map;
    private String cityName = "athens_small";
    private String inputPath = "C:/Users/uqpchao/OneDrive/data/Pfoser/maps/map_athens_small/";
    private int options = 2;    // 0=nothing, 1= points, 2= map, 3= trajectories

    public void settings() {
        size(1440, 900, P2D);
        map = new UnfoldingMap(this);
        MapUtils.createDefaultEventDispatcher(this, map);
//        map.zoomAndPanTo(10, new Location(52.5f, 13.4f));

//        String rssUrl = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.atom";
//        List<Feature> features = GeoRSSReader.loadDataGeoRSS(this, rssUrl);
//        List<Feature> countries = GeoJSONReader.loadData(this, "countries.geo.json");
//        List<Marker> countryMarkers = MapUtils.createSimpleMarkers(countries);
//        map.addMarkers(countryMarkers);
    }

    public void draw() {

        List<Location> locationList = new ArrayList<>();
//        List<SimplePointMarker> newMarkers = new ArrayList<>();

        if (options == 2) try {
            String inputVertexPath = inputPath + cityName + "_vertices_osm.txt";
            String inputEdgePath = inputPath + cityName + "_edges_osm.txt";
            CSVMapReader csvMapReader = new CSVMapReader(inputVertexPath, inputEdgePath);
            RoadNetworkGraph roadNetworkGraph = csvMapReader.readCSV();
            for (RoadNode p : roadNetworkGraph.getNodes()) {
                Location newLocation = new Location(p.lon() / 100000, p.lat() / 100000);
                locationList.add(newLocation);
            }
            for (Location l : locationList) {
                map.addMarker(new SimplePointMarker(l));
            }
        } catch (JDOMException | IOException e) {
            e.printStackTrace();
        }
//        Location berlinLocation = new Location(52.5, 13.4);
//        Location dublinLocation = new Location(53.35, -6.26);
//
//// Create point markers for locations
//        SimplePointMarker berlinMarker = new SimplePointMarker(berlinLocation);
//        SimplePointMarker dublinMarker = new SimplePointMarker(dublinLocation);
//
//// Add markers to the map
//        map.addMarkers(berlinMarker, dublinMarker);
        map.draw();
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"traminer.util.map.UnfoldingMapDemo"});
    }

    public void display(String cityName, String path, String datatype) {
        this.cityName = cityName;
        this.inputPath = path;
        switch (datatype) {
            case "point": {
                options = 1;
                break;
            }
            case "map": {
                options = 2;
                break;
            }
            case "trajectory": {
                options = 3;
                break;
            }
            default:
                System.out.println("Error display type:" + datatype);
        }
        PApplet.main(new String[]{this.getClass().getName()});
    }

}
