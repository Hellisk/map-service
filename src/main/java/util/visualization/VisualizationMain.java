package util.visualization;

public class VisualizationMain {
	public static void main(String[] args) {
		System.setProperty("logfile.name", "display.log");
		UnfoldingBeijingMapDisplay mapDisplay = new UnfoldingBeijingMapDisplay();
		mapDisplay.display();
	}
}