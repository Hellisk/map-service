package edu.uq.dke.mapupdate.mapinference;

public class BiagioniKDE2012 {
    private KDEMapInference mapInference;

    public BiagioniKDE2012(double cellSize, int blur) {
        this.mapInference = new KDEMapInference(cellSize, blur);
    }

    public void KDEMapInferenceProcess(String codeRootPath, String inputTrajPath) {
        KDEMapInference mapInference = new KDEMapInference();
        mapInference.startMapInference(codeRootPath, inputTrajPath);
    }
}
