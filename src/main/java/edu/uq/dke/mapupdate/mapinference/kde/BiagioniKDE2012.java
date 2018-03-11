package edu.uq.dke.mapupdate.mapinference.kde;

public class BiagioniKDE2012 {
    KDEMapInference mapinference;

    public BiagioniKDE2012() {
        this.mapinference = new KDEMapInference();
    }

    public BiagioniKDE2012(double cellSize, int blur) {
        this.mapinference = new KDEMapInference(cellSize, blur);
    }

    public void KDEMapInferenceProcess() {
        KDEMapInference mapinference = new KDEMapInference();
        mapinference.startMapInference();
    }
}
