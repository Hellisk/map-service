package mapupdate.mapinference.trajectoryclustering.pcurves;

import mapupdate.mapinference.trajectoryclustering.Cluster;
import mapupdate.mapinference.trajectoryclustering.pcurves.LinearAlgebra.Sample2D;
import mapupdate.mapinference.trajectoryclustering.pcurves.LinearAlgebra.Vektor;
import mapupdate.mapinference.trajectoryclustering.pcurves.LinearAlgebra.Vektor2D;
import mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurve.PrincipalCurveAlgorithm;
import mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurve.PrincipalCurveClass;
import mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurve.PrincipalCurveParameters;
import mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurve.SetOfCurves;
import mapupdate.util.object.roadnetwork.RoadNode;
import mapupdate.util.object.roadnetwork.RoadWay;
import mapupdate.util.object.spatialobject.Point;
import mapupdate.util.object.spatialobject.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * PrincipalCurveGenerator
 * \brief Provides input/output interface for Kegl's principal curves code
 * <p>
 * Usage: ./backboneFinder inputTxt.txt outputBackboneCoord.txt outputBackboneIdx.txt
 * \param inputTxt.txt Input tab delimited text file containing seed coordinates.
 * \param outputBackboneCoord.txt Output tab delimited text file containing the principal curves backbone
 * \param outputBackboneIdx.txt Output tab delimited text file containing seed indices along the backbone (i.e., the
 * distance of each seed along the principal curves backbone)
 * <p>
 * This class provides an input/output interface for Kegl's principal curves code. For more information, see his website
 * at: http://www.iro.umontreal.ca/~kegl/research/pcurves/
 * This class reads a list of points containing x,y,z seed coordinates. Generate this list of points by dumping a
 * protobuf file with "proto2txt" using Michael's plugin
 * This class writes out a list of points containing x,y,z coordinates of the principal curves backbone.
 * This class writes out a second list of points containing the distance along the backbone for each seed.
 */

public class PrincipalCurveGenerator {
    public RoadWay startPrincipalCurveGen(Cluster inputCluster) throws InterruptedException, IllegalStateException {

        // load seed coordinates
        Sample2D sample2 = new Sample2D();
        for (Trajectory traj : inputCluster.getTrajectoryList()) {
            for (Point p : traj.getCoordinates()) {
                Vektor2D tempVektor = new Vektor2D(p.x(), p.y());
                sample2.AddPoint(tempVektor);
            }
        }

        // run principal curves
        PrincipalCurveParameters principalCurveParameters = new PrincipalCurveParameters();
        PrincipalCurveClass principalCurve = new PrincipalCurveClass(sample2, principalCurveParameters);
        PrincipalCurveAlgorithm algorithmThread = new PrincipalCurveAlgorithm(principalCurve, principalCurveParameters);
//        if (true)
//            throw new RuntimeException("This interface should not be used because it does not deal with random seeds");
        algorithmThread.start(0);

        // output principal curve coordinates
        SetOfCurves savePrincipalCurve = principalCurve.ConvertToCurves();
        assert savePrincipalCurve != null;
        List<RoadNode> roadNodeList = new ArrayList<>();
        for (int i = 0; i < savePrincipalCurve.GetNumOfCurves(); i++) {
            for (int j = 0; j < savePrincipalCurve.GetCurveAt(i).getSize(); j++) {
                Vektor temp = savePrincipalCurve.GetCurveAt(i).GetPointAt(j);
                RoadNode currNode = new RoadNode("", temp.GetCoords(0), temp.GetCoords(1));
                roadNodeList.add(currNode);
            }
        }
        RoadWay newWay = new RoadWay("", roadNodeList);
        newWay.setConfidenceScore(inputCluster.getTrajectoryList().size());
        newWay.setNewRoad(true);
        return newWay;
    }
}
