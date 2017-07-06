package traminer.util.spatial.clustering;

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansLloyd;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMedoidsPAM;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansPlusPlusInitialMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMedoidsInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.PAMInitialMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.parallel.ParallelLloydKMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.DeLiClu;
import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.OPTICSXi;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.model.OPTICSModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.Point;

import java.util.List;

/**
 * Service to provide clustering methods for spatial Points.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class SpatialClusteringBuilder implements ClusteringInterface {

    /**
     * Run DBSCAN clustering on the given list of points.
     *
     * @param dataPts  Data to cluster (list of points).
     * @param eps      DBSCAN parameter epsilon (distance threshold).
     * @param minPts   Minimum number of point in each DBSCAN cluster.
     * @param distFunc The points distance function to use.
     * @return List of DBSCAN clusters.
     */
    public List<SpatialCluster> runDBSCAN(
            List<Point> dataPts, double eps, int minPts,
            PointDistanceFunction distFunc) {
        if (dataPts == null || dataPts.isEmpty()) {
            throw new IllegalArgumentException("List of spatial points "
                    + "for spatial clustering cannot be empty.");
        }
        if (distFunc == null) {
            throw new NullPointerException("Distance function "
                    + "for spatial clustering cannot be null.");
        }
        if (eps < 0) {
            throw new IllegalArgumentException("Parameter epsilon 'eps' "
                    + "for DBSCAN clustering must be positive.");
        }
        if (minPts <= 0) {
            throw new IllegalArgumentException("Minimum number of points "
                    + "for DBSCAN clustering must be greater than zero.");
        }
        Database db = ElkiAdapter.getDatabase(dataPts);
        DistanceFunction<NumberVector> elkiDist =
                ElkiAdapter.getDistanceFunction(distFunc);

        // DBSCAN algorithm setup
        ListParameterization dbscanParams = new ListParameterization();
        dbscanParams.addParameter(DBSCAN.Parameterizer.EPSILON_ID, eps);
        dbscanParams.addParameter(DBSCAN.Parameterizer.MINPTS_ID, minPts);
        dbscanParams.addParameter(DBSCAN.DISTANCE_FUNCTION_ID, elkiDist);

        DBSCAN<DoubleVector> dbscan = ClassGenericsUtil.parameterizeOrAbort(
                DBSCAN.class, dbscanParams);

        // run DBSCAN on database and get clusters
        Clustering<Model> dbscanModel = dbscan.run(db);
        // get the clusters
        List<SpatialCluster> clusters = ElkiAdapter.getClusters(db, dbscanModel);

        return clusters;
    }

    /**
     * Run kMeans clustering on the given set of points.
     *
     * @param dataPts  Data to cluster (list of points).
     * @param k        Number of clusters.
     * @param maxIter  Maximum number of iterations to allow.
     * @param distFunc The points distance function to use.
     * @return List of kMeans clusters.
     */
    public List<SpatialCluster> runKMeans(
            List<Point> dataPts, int k, int maxIter,
            PointDistanceFunction distFunc) {
        if (dataPts == null || dataPts.isEmpty()) {
            throw new IllegalArgumentException("List of spatial points "
                    + "for spatial clustering cannot be empty.");
        }
        if (distFunc == null) {
            throw new NullPointerException("Distance function "
                    + "for spatial clustering cannot be null.");
        }
        if (k <= 0) {
            throw new IllegalArgumentException("Number of clusters (k) "
                    + "for KMeans clustering must be greater than zero.");
        }
        if (maxIter < 0) {
            throw new IllegalArgumentException("Maximum number of iterations "
                    + "for KMeans clustering must be positive.");
        }
        Database db = ElkiAdapter.getDatabase(dataPts);
        DistanceFunction<NumberVector> elkiDist =
                ElkiAdapter.getDistanceFunction(distFunc);

        // kMeans algorithm setup
        ListParameterization kmeansParams = new ListParameterization();
        kmeansParams.addParameter(KMeans.K_ID, k);
        kmeansParams.addParameter(KMeans.MAXITER_ID, maxIter);
        kmeansParams.addParameter(KMeans.DISTANCE_FUNCTION_ID, elkiDist);

        KMeansLloyd<DoubleVector> kmeans = ClassGenericsUtil.parameterizeOrAbort(
                KMeansLloyd.class, kmeansParams);

        // run kMeans on database
        Clustering<KMeansModel> kmeansModel = kmeans.run(db);

        // get the clusters and their means
        List<SpatialCluster> clusters = ElkiAdapter.getClustersMeans(db, kmeansModel);

        return clusters;
    }

    /**
     * Run kMeans++ clustering (parallel kMeans) on the given set of points.
     *
     * @param dataPts  Data to cluster (list of points).
     * @param k        Number of clusters.
     * @param maxIter  Maximum number of iterations to allow.
     * @param distFunc The points distance function to use.
     * @return List of kMeans clusters.
     */
    public List<SpatialCluster> runKMeansPlusPlus(
            List<Point> dataPts, int k, int maxIter,
            PointDistanceFunction distFunc) {
        if (dataPts == null || dataPts.isEmpty()) {
            throw new IllegalArgumentException("List of spatial points "
                    + "for spatial clustering cannot be empty.");
        }
        if (distFunc == null) {
            throw new NullPointerException("Distance function "
                    + "for spatial clustering cannot be null.");
        }
        if (k <= 0) {
            throw new IllegalArgumentException("Number of clusters (k) "
                    + "for KMeans clustering must be greater than zero.");
        }
        if (maxIter < 0) {
            throw new IllegalArgumentException("Maximum number of iterations "
                    + "for KMeans clustering must be positive.");
        }
        Database db = ElkiAdapter.getDatabase(dataPts);
        DistanceFunction<NumberVector> elkiDist =
                ElkiAdapter.getDistanceFunction(distFunc);

        // kMeans algorithm setup
        ListParameterization kmeansParams = new ListParameterization();
        kmeansParams.addParameter(ParallelLloydKMeans.K_ID, k);
        kmeansParams.addParameter(ParallelLloydKMeans.MAXITER_ID, maxIter);
        kmeansParams.addParameter(ParallelLloydKMeans.DISTANCE_FUNCTION_ID, elkiDist);
        kmeansParams.addParameter(ParallelLloydKMeans.INIT_ID, KMeansPlusPlusInitialMeans.class);

        ParallelLloydKMeans<DoubleVector> kmeanspp = ClassGenericsUtil
                .parameterizeOrAbort(ParallelLloydKMeans.class, kmeansParams);

        // run kMeans on database
        Clustering<KMeansModel> kmeansppModel = kmeanspp.run(db);

        // get the clusters and their means
        List<SpatialCluster> clusters = ElkiAdapter.getClustersMeans(db, kmeansppModel);

        return clusters;
    }

    /**
     * Run kMedoids using PAM algorithm on the given set of points.
     *
     * @param dataPts  Data to cluster (list of points).
     * @param k        Number of clusters.
     * @param maxIter  Maximum number of iterations to allow.
     * @param distFunc The points distance function to use.
     * @return List of kMedoids clusters.
     */
    public List<SpatialCluster> runKMedoids(
            List<Point> dataPts, int k, int maxIter,
            PointDistanceFunction distFunc) {
        if (dataPts == null || dataPts.isEmpty()) {
            throw new IllegalArgumentException("List of spatial points "
                    + "for spatial clustering cannot be empty.");
        }
        if (distFunc == null) {
            throw new NullPointerException("Distance function "
                    + "for spatial clustering cannot be null.");
        }
        if (k <= 0) {
            throw new IllegalArgumentException("Number of clusters (k) "
                    + "for KMedoids clustering must be greater than zero.");
        }
        if (maxIter < 0) {
            throw new IllegalArgumentException("Maximum number of iterations "
                    + "for KMedoids clustering must be positive.");
        }
        Database db = ElkiAdapter.getDatabase(dataPts);
        DistanceFunction<NumberVector> elkiDist =
                ElkiAdapter.getDistanceFunction(distFunc);

        // kMedoids algorithm setup
        KMedoidsInitialization<DoubleVector> init =
                new PAMInitialMeans<DoubleVector>();
        ListParameterization kmedoidsParams = new ListParameterization();
        kmedoidsParams.addParameter(KMeans.K_ID, k);
        kmedoidsParams.addParameter(KMeans.MAXITER_ID, maxIter);
        kmedoidsParams.addParameter(KMeans.INIT_ID, init);
        kmedoidsParams.addParameter(KMedoidsPAM.DISTANCE_FUNCTION_ID, elkiDist);

        KMedoidsPAM<DoubleVector> kmedoids = ClassGenericsUtil
                .parameterizeOrAbort(KMedoidsPAM.class, kmedoidsParams);

        // run kMedoids on database
        Clustering<MedoidModel> kmedoidsModel = kmedoids.run(db);

        // get the clusters and their medoids
        List<SpatialCluster> clusters = ElkiAdapter.getClustersMedoids(db, kmedoidsModel);

        return clusters;
    }

    /**
     * Run OPTICS-based clustering DeLiClu (Density-Link-Clustering)
     * on the given set of points.
     *
     * @param dataPts  Data to cluster (list of points).
     * @param xi       OPTICS Parameter to specify the steepness threshold.
     *                 A contrast parameter, the relative decrease in density
     *                 (e.g. 0.1 = 10% drop in density).
     * @param minPts   Minimum number of point in each cluster.
     * @param distFunc The points distance function to use.
     * @return List of OPTICS clusters.
     */
    public List<SpatialCluster> runOPTICS(
            List<Point> dataPts, double xi, int minPts,
            PointDistanceFunction distFunc) {
        if (dataPts == null || dataPts.isEmpty()) {
            throw new IllegalArgumentException("List of spatial points "
                    + "for spatial clustering cannot be empty.");
        }
        if (distFunc == null) {
            throw new NullPointerException("Distance function "
                    + "for spatial clustering cannot be null.");
        }
        if (xi < 0 || xi > 1) {
            throw new IllegalArgumentException("Parameter 'xi' for OPTICS "
                    + "clustering must be a number between 0 and 1.");
        }
        if (minPts <= 0) {
            throw new IllegalArgumentException("Minimum number of points "
                    + "for OPTICS clustering must be greater than zero.");
        }
        Database db = ElkiAdapter.getDatabase(dataPts);
        DistanceFunction<NumberVector> elkiDist =
                ElkiAdapter.getDistanceFunction(distFunc);

        // DeLiClu algorithm setup
        ListParameterization delicluParams = new ListParameterization();
        delicluParams.addParameter(DeLiClu.Parameterizer.MINPTS_ID, minPts);
        delicluParams.addParameter(DeLiClu.DISTANCE_FUNCTION_ID, elkiDist);

        DeLiClu<DoubleVector> deliclu = ClassGenericsUtil
                .parameterizeOrAbort(DeLiClu.class, delicluParams);

        // run OPTICS and get clusters using DeLiClu
        OPTICSXi optics = new OPTICSXi(deliclu, xi);
        Clustering<OPTICSModel> opticsModel = optics.run(db);

        // get the clusters and their medoids
        List<SpatialCluster> clusters = ElkiAdapter.getClusters(db, opticsModel);

        return clusters;
    }

    // TODO
    /**
     * Expectation Maximisation (EM)
     * clustering using Elki Java framework.
     *
     * @param k Number of clusters.
     * @param maxIter Maximum number of iterations to allow.
     * @param delta Delta parameter.
     * @param soft Retain soft assignments.
     * @param distFunc The points distance function to use.
     *
     */
    /*
    public static Clustering<EMModel> runEM(
			Database db, int k, int maxIter, double delta, boolean soft){
		
		AbstractEMModelFactory<NumberVector, EMModel> init = new MultivariateG
		EM<NumberVector, EMModel> em = new EM<NumberVector, MeanModel>(k, delta,  {
		}, maxIter, soft);
		
		// run kMeans on database
	    Clustering<KMeansModel> result = kmeans.run(db);
		
	    return result;
	}
	 */
}
