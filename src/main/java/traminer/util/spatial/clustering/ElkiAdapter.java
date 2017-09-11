package traminer.util.spatial.clustering;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.model.MedoidModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
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
import traminer.util.exceptions.DistanceFunctionException;
import traminer.util.spatial.distance.HaversineDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Elki library adapter. Bridge the compatibility between the methods
 * and objects of this library, and the Elki library functionalities.
 *
 * @see http://elki.dbs.ifi.lmu.de/
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
final class ElkiAdapter implements ClusteringInterface {

    /**
     * Process the result from a Elki clustering algorithm (model).
     * Collect the clusters from the model, and return a list
     * of spatial clusters.
     *
     * @param db              The Elki database containing the clusterring data.
     * @param clusteringModel The Elki clustering model.
     * @return A list of spatial clusters.
     * @see SpatialClusteringBuilder
     */
    public static List<SpatialCluster> getClusters(Database db, Clustering<? extends Model> clusteringModel) {
        // Relation containing the double vectors:
        Relation<DoubleVector> rel = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);//NUMBER_VECTOR_FIELD);

        int i = 0;
        List<SpatialCluster> clusterList = new ArrayList<SpatialCluster>();
        for (Cluster<? extends Model> clu : clusteringModel.getAllClusters()) {
            SpatialCluster cluster = new SpatialCluster("" + (i++));
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

    /**
     * Process the result from a Elki clustering algorithm (KMeans model).
     * Collect the clusters with their means from the model, and return a
     * list of spatial clusters.
     *
     * @param db              The Elki database containing the clusterring data.
     * @param clusteringModel The Elki KMeans clustering model.
     * @return A list of spatial clusters with means.
     * @see SpatialClusteringBuilder
     */
    public static List<SpatialCluster> getClustersMeans(Database db, Clustering<KMeansModel> clusteringModel) {
        // Relation containing the double vectors:
        Relation<DoubleVector> rel = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);

        // Process clusters
        List<SpatialCluster> clusterList = new ArrayList<SpatialCluster>();
        int i = 0;
        for (Cluster<KMeansModel> model : clusteringModel.getAllClusters()) {
            SpatialCluster cluster = new SpatialCluster("" + (i++));
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
     * Process the result from a Elki clustering algorithm (KMedoids model).
     * Collect the clusters with their medoids from the model, and return a
     * list of spatial clusters.
     *
     * @param db              The Elki database containing the clusterring data.
     * @param clusteringModel The Elki KMedoids clustering model.
     * @return A list of spatial clusters with medoids.
     * @see SpatialClusteringBuilder
     */
    public static List<SpatialCluster> getClustersMedoids(Database db, Clustering<MedoidModel> clusteringModel) {
        // Relation containing the double vectors:
        Relation<DoubleVector> rel = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);

        // Process clusters
        List<SpatialCluster> clusterList = new ArrayList<SpatialCluster>();
        int i = 0;
        for (Cluster<MedoidModel> model : clusteringModel.getAllClusters()) {
            SpatialCluster cluster = new SpatialCluster("" + (i++));
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
     * Convert the input data (list of points) to a Elki Database format.
     *
     * @param data The list of point to convert.
     * @return A Elki Database from the input list of points.
     * @see SpatialClusteringBuilder
     */
    public static Database getDatabase(List<Point> data) {
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
        return getDatabase(db);
    }

    /**
     * Convert the input data (double matrix) to a Elki Database format.
     *
     * @param data The double matrix to convert.
     * @return A Elki Database from the input double array
     * @see SpatialClusteringBuilder
     */
    public static Database getDatabase(double[][] data) {
        DatabaseConnection dbc = new ArrayAdapterDatabaseConnection(data);
        Database db = new StaticArrayDatabase(dbc, null);
        db.initialize();

        return db;
    }

    /**
     * Get the correspondent Elki distance function for the given point
     * distance function.
     *
     * @param distFunc Point distance function to parse.
     * @return A Elki distance function.
     * @throws DistanceFunctionException If the distance function is not
     *                                   supported by the Elki library.
     */
    public static DistanceFunction<NumberVector> getDistanceFunction(PointDistanceFunction distFunc) {
        if (distFunc instanceof HaversineDistanceFunction) {
            return new LatLngDistanceFunction(SphericalHaversineEarthModel.STATIC);
        } else if (distFunc instanceof EuclideanDistanceFunction) {
            return EuclideanDistanceFunction.STATIC;
        } else {
            throw new DistanceFunctionException("Distance function is not supported.");
        }
    }

    /**
     * Auxiliary print function. Prints the given clustering to the
     * output console.
     *
     * @param db              The Elki database containing the clustering data.
     * @param clusteringModel The Elki clustering model.
     * @see SpatialClusteringBuilder
     */
    public static void printResult(Database db, Clustering<? extends Model> clusteringModel) {
        // Relation containing the number vectors:
        Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
        // We know that the ids must be a continuous range:
        DBIDRange ids = (DBIDRange) rel.getDBIDs();

        int i = 0;
        for (Cluster<? extends Model> clu : clusteringModel.getAllClusters()) {
            // will name all clusters "Cluster" in lack of noise support:
            println("#" + i + ": " + clu.getNameAutomatic());
            println("Size: " + clu.size());
            println("Center: " + clu.getModel().toString());
            // Iterate over objects:
            print("Objects: ");
            for (DBIDIter it = clu.getIDs().iter(); it.valid(); it.advance()) {
                // To get the vector use:
                // NumberVector v = rel.get(it);

                // Offset within our DBID range: "line number"
                final int offset = ids.getOffset(it);
                print(" " + offset);
                // Do NOT rely on using "internalGetIndex()" directly!
            }
            println("");
            ++i;
        }
    }

    /**
     * Print object to system output.
     *
     * @param obj Object to print.
     */
    private static void println(Object obj) {
        System.out.println(obj.toString());
    }

    /**
     * Print object to system output.
     *
     * @param obj Object to print.
     */
    private static void print(Object obj) {
        System.out.print(obj.toString());
    }
}
