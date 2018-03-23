package edu.uq.dke.mapupdate.mapinference.kde;

public class BiagioniKDE2012 {
    KDEMapInference mapInference;

    public BiagioniKDE2012() {
        this.mapInference = new KDEMapInference();
    }

    public BiagioniKDE2012(double cellSize, int blur) {
        this.mapInference = new KDEMapInference(cellSize, blur);
    }

    public void KDEMapInferenceProcess(String rootPath) {
        KDEMapInference mapinference = new KDEMapInference();
        mapinference.startMapInference(rootPath);
    }
}
