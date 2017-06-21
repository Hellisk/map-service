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
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.model.OPTICSModel;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.geo.LatLngDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.geodesy.SphericalHaversineEarthModel;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import traminer.util.spatial.distance.HaversineDistanceFunction;
import traminer.util.spatial.objects.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to provide clustering methods for spatial
 * Points, using the Elki Java framework.
 * <p>
 * <p> http://elki.dbs.ifi.lmu.de/
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class SpatialClusteringService implements ClusteringInterface {
    private static DistanceFunction<NumberVector> elkiDist =
            EuclideanDistanceFunction.STATIC;

    /**
     * The distance measure to use.
     *
     * @param distCalc
     */
    public SpatialClusteringService(DistanceFunction distCalc) {
        if (distCalc instanceof HaversineDistanceFunction) {
            elkiDist = new LatLngDistanceFunction(
                    SphericalHaversineEarthModel.STATIC);
        } else {
            elkiDist = EuclideanDistanceFunction.STATIC;
        }
    }

    /**
     * Run DBSCAN clustering on the given set of points.
     *
     * @param dataPts Data to cluster (list of points).
     * @param eps     DBSCAN parameter epsilon (distance threshold)
     * @param minPts  Minimum number of point in each DBSCAN cluster.
     * @return List of DBSCAN clusters.
     */
    public List<SpatialCluster> getDBSCAN(List<Point> dataPts, double eps, int minPts) {
        Database db = getElkiDatabase(dataPts);
        Clustering<Model> dbscan = runDBSCAN(db, eps, minPts);
        return getClusters(db, dbscan);
    }

    /**
     * Run DBSCAN clustering on the given database, using Elki framework.
     *
     * @param eps    DBSCAN parameter epsilon (distance threshold)
     * @param minPts Minimum number of point in each DBSCAN cluster.
     */
    private static Clustering<Model> runDBSCAN(Database db, double eps, int minPts) {
        // DBSCAN algorithm setup
        ListParameterization dbscanParams = new ListParameterization();
        dbscanParams.addParameter(DBSCAN.Parameterizer.EPSILON_ID, eps);
        dbscanParams.addParameter(DBSCAN.Parameterizer.MINPTS_ID, minPts);
        dbscanParams.addParameter(DBSCAN.DISTANCE_FUNCTION_ID, elkiDist);

        DBSCAN<DoubleVector> dbscan = ClassGenericsUtil.parameterizeOrAbort(
                DBSCAN.class, dbscanParams);

        // run DBSCAN on database
        Clustering<Model> result = dbscan.run(db);

        return result;
    }

    /**
     * Run kMeans clustering on the given set of points.
     *
     * @param dataPts Data to cluster (list of points).
     * @param k       Number of clusters.
     * @param maxIter Maximum number of iterations to allow.
     * @return List of kMeans clusters.
     */
    public List<SpatialCluster> getKMeans(List<Point> dataPts, int k, int maxIter) {
        Database db = getElkiDatabase(dataPts);
        Clustering<KMeansModel> kmeans = runKMeans(db, k, maxIter);

        // Relation containing the double vectors:
        Relation<DoubleVector> rel = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
        // Process clusters
        List<SpatialCluster> clusterList = new ArrayList<SpatialCluster>();
        int i = 0;
        for (Cluster<KMeansModel> model : kmeans.getAllClusters()) {
            SpatialCluster cluster = new SpatialCluster("" + (i++), model.size());
            cluster.setNoise(model.isNoise());

            // get the cluster center (Mean)
            double[] mean = model.getModel().getMean().getArrayCopy();
            cluster.setCenter(mean[0], mean[1]);

            // Iterate over cluster objects
            for (DBIDIter it = model.getIDs().iter(); it.valid(); it.advance()) {
                // get the vector values
                double[] obj = rel.get(it).getValues();
                cluster.add(new Point(obj[0], obj[1]));
            }
            clusterList.add(cluster);
        }

        return clusterList;
    }

    /**
     * Run kMeans clustering on the given database, using Elki framework.
     *
     * @param k       Number of clusters.
     * @param maxIter Maximum number of iterations to allow.
     */
    private static Clustering<KMeansModel> runKMeans(Database db, int k, int maxIter) {
        // kMeans algorithm setup
        ListParameterization kmeansParams = new ListParameterization();
        kmeansParams.addParameter(KMeans.K_ID, k);
        kmeansParams.addParameter(KMeans.MAXITER_ID, maxIter);
        kmeansParams.addParameter(KMeans.DISTANCE_FUNCTION_ID, elkiDist);

        KMeansLloyd<DoubleVector> kmeans = ClassGenericsUtil.parameterizeOrAbort(
                KMeansLloyd.class, kmeansParams);

        // run kMeans on database
        Clustering<KMeansModel> result = kmeans.run(db);

        return result;
    }

    /**
     * Run kMeans++ clustering (parallel kMeans) on the given set of points.
     *
     * @param dataPts Data to cluster (list of points).
     * @param k       Number of clusters.
     * @param maxIter Maximum number of iterations to allow.
     * @return List of kMeans clusters.
     */
    public List<SpatialCluster> getKMeansPlusPlus(List<Point> dataPts, int k, int maxIter) {
        Database db = getElkiDatabase(dataPts);
        Clustering<KMeansModel> kmeans = runKMeansPlusPlus(db, k, maxIter);

        // Relation containing the double vectors:
        Relation<DoubleVector> rel = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
        // Process clusters
        List<SpatialCluster> clusterList = new ArrayList<SpatialCluster>();
        int i = 0;
        for (Cluster<KMeansModel> model : kmeans.getAllClusters()) {
            SpatialCluster cluster = new SpatialCluster("" + (i++), model.size());
            cluster.setNoise(model.isNoise());

            // get the cluster center (Mean)
            double[] mean = model.getModel().getMean().getArrayCopy();
            cluster.setCenter(mean[0], mean[1]);

            // Iterate over cluster objects
            for (DBIDIter it = model.getIDs().iter(); it.valid(); it.advance()) {
                // get the vector values
                double[] obj = rel.get(it).getValues();
                cluster.add(new Point(obj[0], obj[1]));
            }
            clusterList.add(cluster);
        }

        return clusterList;
    }

    /**
     * Run kMeans++ clustering (parallel kMeans) on the given database,
     * using Elki framework.
     *
     * @param k       Number of clusters.
     * @param maxIter Maximun number of iterations.
     */
    private static Clustering<KMeansModel> runKMeansPlusPlus(Database db, int k, int maxIter) {
        // kMeans algorithm setup
        ListParameterization kmeansParams = new ListParameterization();
        kmeansParams.addParameter(ParallelLloydKMeans.K_ID, k);
        kmeansParams.addParameter(ParallelLloydKMeans.MAXITER_ID, maxIter);
        kmeansParams.addParameter(ParallelLloydKMeans.DISTANCE_FUNCTION_ID, elkiDist);
        kmeansParams.addParameter(ParallelLloydKMeans.INIT_ID, KMeansPlusPlusInitialMeans.class);

        ParallelLloydKMeans<DoubleVector> kmeanspp = ClassGenericsUtil.parameterizeOrAbort(
                ParallelLloydKMeans.class, kmeansParams);

        // run kMeans on database
        Clustering<KMeansModel> result = kmeanspp.run(db);

        return result;
    }

    /**
     * Run kMedoids using PAM algorithm on the given set of points.
     *
     * @param dataPts Data to cluster (list of points).
     * @param k       Number of clusters.
     * @param maxIter Maximum number of iterations to allow.
     * @return List of kMedoids clusters.
     */
    public List<SpatialCluster> getKMedoids(List<Point> dataPts, int k, int maxIter) {
        Database db = getElkiDatabase(dataPts);
        Clustering<MedoidModel> kmedoids = runKMedoids(db, k, maxIter);

        // Relation containing the double vectors:
        Relation<DoubleVector> rel = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);//NUMBER_VECTOR_FIELD);
        // Process clusters
        List<SpatialCluster> clusterList = new ArrayList<SpatialCluster>();
        int i = 0;
        for (Cluster<MedoidModel> model : kmedoids.getAllClusters()) {
            SpatialCluster cluster = new SpatialCluster("" + (i++), model.size());
            cluster.setNoise(model.isNoise());

            // get the cluster center (Medoid)
            DBID medoidId = model.getModel().getMedoid();
            double[] medoid = rel.get(medoidId).getValues();
            cluster.setCenter(medoid[0], medoid[1]);

            // Iterate over cluster objects
            for (DBIDIter it = model.getIDs().iter(); it.valid(); it.advance()) {
                // get the vector values
                double[] obj = rel.get(it).getValues();
                cluster.add(new Point(obj[0], obj[1]));
            }
            clusterList.add(cluster);
        }

        return clusterList;
    }

    /**
     * Run kMedoids using PAM algorithm on the given database,
     * using Elki framework.
     *
     * @param k       Number of clusters.
     * @param maxIter Maximum number of iterations to allow.
     */
    private static Clustering<MedoidModel> runKMedoids(Database db, int k, int maxIter) {
        // kMedoids algorithm setup
        KMedoidsInitialization<DoubleVector> init =
                new PAMInitialMeans<DoubleVector>();
        ListParameterization kmedoidsParams = new ListParameterization();
        kmedoidsParams.addParameter(KMeans.K_ID, k);
        kmedoidsParams.addParameter(KMeans.MAXITER_ID, maxIter);
        kmedoidsParams.addParameter(KMeans.INIT_ID, init);
        kmedoidsParams.addParameter(KMedoidsPAM.DISTANCE_FUNCTION_ID, elkiDist);

        KMedoidsPAM<DoubleVector> kmedoids = ClassGenericsUtil.parameterizeOrAbort(
                KMedoidsPAM.class, kmedoidsParams);

        // run kMedoids on database
        Clustering<MedoidModel> result = kmedoids.run(db);

        return result;
    }

    /**
     * Run OPTICS-based clustering DeLiClu (Density-Link-Clustering)
     * on the given set of points.
     *
     * @param dataPts Data to cluster (list of points).
     * @param xi      OPTICS Parameter to specify the steepness threshold.
     *                A contrast parameter, the relative decrease in density
     *                (e.g. 0.1 = 10% drop in density).
     * @param minPts  Minimum number of point in each cluster.
     * @return List of OPTICS clusters.
     */
    public List<SpatialCluster> getOPTICS(List<Point> dataPts, double xi, int minPts) {
        Database db = getElkiDatabase(dataPts);
        Clustering<OPTICSModel> optics = runOPTICS(db, xi, minPts);
        return getClusters(db, optics);
    }

    /**
     * Run OPTICS-based clustering DeLiClu (Density-Link-Clustering) on
     * the given database, using Elki framework.
     *
     * @param xi     OPTICS Parameter to specify the steepness threshold.
     *               A contrast parameter, the relative decrease in density
     *               (e.g. 0.1 = 10% drop in density).
     * @param minPts Minimum number of point in each cluster.
     */
    private static Clustering<OPTICSModel> runOPTICS(Database db, double xi, int minPts) {
        // DeLiClu algorithm setup
        ListParameterization delicluParams = new ListParameterization();
        delicluParams.addParameter(DeLiClu.Parameterizer.MINPTS_ID, minPts);
        delicluParams.addParameter(DeLiClu.DISTANCE_FUNCTION_ID, elkiDist);

        DeLiClu<DoubleVector> deliclu = ClassGenericsUtil.parameterizeOrAbort(
                DeLiClu.class, delicluParams);

        // run OPTICS and get clusters using DeLiClu
        OPTICSXi optics = new OPTICSXi(deliclu, xi);
        Clustering<OPTICSModel> result = optics.run(db);

        return result;
    }

    /**
     * Simple example of Expectation Maximisation (EM)
     * clustering using Elki Java framework.
     *
     * @param k Number of clusters.
     * @param maxIter Maximum number of iterations to allow.
     * @param delta Delta parameter.
     * @param soft Retain soft assignments.
     */
/*	public static Clustering<EMModel> runEM(
            Database db, int k, int maxIter, double delta, boolean soft){
		
		AbstractEMModelFactory<NumberVector, EMModel> init = new MultivariateG
		EM<NumberVector, EMModel> em = new EM<NumberVector, MeanModel>(k, delta,  {
		}, maxIter, soft);
		
		// run kMeans on database
	    Clustering<KMeansModel> result = kmeans.run(db);
		
	    return result;
	}
*/

    /**
     * Convert the input data (double array)
     * to Elki Database format.
     */
    private static Database getElkiDatabase(double[][] data) {
        DatabaseConnection dbc = new ArrayAdapterDatabaseConnection(data);
        Database db = new StaticArrayDatabase(dbc, null);
        db.initialize();

        return db;
    }

    /**
     * Convert the input data (list of points)
     * to Elki Database format.
     */
    private static Database getElkiDatabase(List<Point> data) {
        // Use 2D array of doubles, where each row represents a point
        // and you have as much columns as your dimensions
        int size = data.size();
        int dim = 3;//data.get(0).dimension;
        double[][] db = new double[size][dim];
        Point p;
        for (int i = 0; i < size; i++) {
            p = data.get(i);
            db[i][0] = p.x();
            db[i][1] = p.y();
        }
        return getElkiDatabase(db);
    }

    /**
     * Process the result from a cluster algorithm (model).
     * collect the clusters form the model and return a list
     * of spatial clusters.
     */
    private static List<SpatialCluster> getClusters(Database db, Clustering<? extends Model> modelResult) {
        // Relation containing the double vectors:
        Relation<DoubleVector> rel = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);//NUMBER_VECTOR_FIELD);

        int i = 0;
        List<SpatialCluster> clusterList = new ArrayList<SpatialCluster>();
        for (Cluster<? extends Model> clu : modelResult.getAllClusters()) {
            SpatialCluster cluster = new SpatialCluster("" + (i++), clu.size());
            cluster.setNoise(clu.isNoise());

            // Iterate over cluster objects
            for (DBIDIter it = clu.getIDs().iter(); it.valid(); it.advance()) {
                // get the vector values
                double[] obj = rel.get(it).getValues();
                cluster.add(new Point(obj[0], obj[1]));
            }
            clusterList.add(cluster);
        }

        return clusterList;
    }

    private static void printResult(Database db, Clustering<? extends Model> result) {
        // Relation containing the number vectors:
        Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
        // We know that the ids must be a continuous range:
        DBIDRange ids = (DBIDRange) rel.getDBIDs();

        int i = 0;
        for (Cluster<? extends Model> clu : result.getAllClusters()) {
            // will name all clusters "Cluster" in lack of noise support:
            System.out.println("#" + i + ": " + clu.getNameAutomatic());
            System.out.println("Size: " + clu.size());
            System.out.println("Center: " + clu.getModel().toString());
            // Iterate over objects:
            System.out.print("Objects: ");
            for (DBIDIter it = clu.getIDs().iter(); it.valid(); it.advance()) {
                // To get the vector use:
                // NumberVector v = rel.get(it);

                // Offset within our DBID range: "line number"
                final int offset = ids.getOffset(it);
                System.out.print(" " + offset);
                // Do NOT rely on using "internalGetIndex()" directly!
            }
            System.out.println();
            ++i;
        }
    }

    /**
     * Test
     */
    public static void main(String[] arg) {
        // Use 2D array of doubles, where each row represents a point
        // and you have as much columns as your dimensions
        int NUM_OF_POINTS = 1000;
        int NUM_OF_DIMENSIONS = 2;
        double[][] data = new double[NUM_OF_POINTS][NUM_OF_DIMENSIONS];

        // populate array according to your data
        //.. TODO

        // parse double database
        Database db = getElkiDatabase(data);

        // DBSCAN parameters
        double eps = 0.1;
        int minPts = 10;
        // run DBSCAN
        Clustering<Model> dbscanResult =
                runDBSCAN(db, eps, minPts);
        // print result
        printResult(db, dbscanResult);

        // kMeans parameters
        int k = 10;
        int maxIter = 10;
        // run kMeans
        Clustering<KMeansModel> kmeansResult =
                runKMeans(db, k, maxIter);
        // print result
        printResult(db, kmeansResult);
    }
}
