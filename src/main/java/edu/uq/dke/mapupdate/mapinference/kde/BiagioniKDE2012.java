package edu.uq.dke.mapupdate.mapinference.kde;

import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.trajectory.Trajectory;

import java.util.List;

public class BiagioniKDE2012 {
    KDEMapInference mapinference;

    public BiagioniKDE2012() {
        this.mapinference = new KDEMapInference();
    }

    public BiagioniKDE2012(double cellSize, int blur) {
        this.mapinference = new KDEMapInference(cellSize, blur);
    }

    public void KDEMapInferenceProcess(List<Trajectory> trajList, RoadNetworkGraph currMap, String outputMapPath) {
        KDEMapInference mapinference = new KDEMapInference();
        mapinference.createKDEWithTrajectories(trajList, currMap, outputMapPath);
    }
}
