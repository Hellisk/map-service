package edu.uq.dke.mapupdate.visualisation;

public class VisualizationMain {
    public static void main(String[] args) {
        // visualization
//        UnfoldingMapDisplay mapDisplay = new UnfoldingMapDisplay();
//        mapDisplay.display();
//        UnfoldingBeijingTrajectoryDisplay trajDisplay = new UnfoldingBeijingTrajectoryDisplay();
//        trajDisplay.display();

        UnfoldingGlobalTrajectoryDisplay globalTrajectoryDisplay = new UnfoldingGlobalTrajectoryDisplay();
        globalTrajectoryDisplay.display();
    }
}
