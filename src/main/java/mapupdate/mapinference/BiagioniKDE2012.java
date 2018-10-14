package mapupdate.mapinference;

import java.io.IOException;

public class BiagioniKDE2012 {
    private KDEMapInference mapInference;

    public BiagioniKDE2012(int cellSize, int blur) {
        this.mapInference = new KDEMapInference(cellSize, blur);
    }

    public void KDEMapInferenceProcess(String codeRootPath, String inputTrajPath) throws IOException {
        mapInference.startMapInference(codeRootPath, inputTrajPath);
    }
}
