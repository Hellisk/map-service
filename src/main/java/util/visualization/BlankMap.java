package util.visualization;

import de.fhpotsdam.unfolding.core.Coordinate;
import de.fhpotsdam.unfolding.providers.OpenStreetMap;
import processing.core.PApplet;

public class BlankMap {

    public static abstract class BlankMapProvider extends OpenStreetMap.OpenStreetMapProvider {
	
		BlankMapProvider() {
            super();
        }

        public String getZoomString(Coordinate coordinate) {
            // Rows are numbered from bottom to top (opposite to OSM)
            float gridSize = PApplet.pow(2, coordinate.zoom);
            float negativeRow = gridSize - coordinate.row - 1;

            return (int) coordinate.zoom + "/" + (int) coordinate.column + "/" + (int) negativeRow;
        }

        public String getPositiveZoomString(Coordinate coordinate) {
            float gridSize = PApplet.pow(2, coordinate.zoom);

            return (int) coordinate.zoom + "/" + (int) coordinate.column + "/" + (int) coordinate.row;
        }
    }

    public static class BlankProvider extends BlankMapProvider {
        public String[] getTileUrls(Coordinate coordinate) {
            String localDir = System.getProperty("user.dir");
            String url = "file:///" + localDir + "/files/blank tile.png";
            return new String[]{url};
        }
    }
}

