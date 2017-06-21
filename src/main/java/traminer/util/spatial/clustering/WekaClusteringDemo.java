package traminer.util.spatial.clustering;

import traminer.util.exceptions.ClusteringException;
import weka.clusterers.Cobweb;
import weka.clusterers.EM;
import weka.clusterers.SimpleKMeans;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.converters.ArffLoader;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.File;

/**
 * Simple example of clustering methods using the Weka library.
 * <p>
 * <p> https://weka.wikispaces.com/
 *
 * @param <T> Type of data to cluster.
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class WekaClusteringDemo implements ClusteringInterface {

    /**
     * Simple example of kMeans++ clustering using Weka Java library.
     *
     * @param k        number of clusters
     * @param dataPath path to data file
     */
    public void simpleKMeansPlusPlus(final int k, final String dataPath) {
        try {
            // data to cluster
            Instances data = DataSource.read(dataPath);

            // create the model
            SimpleKMeans kMeans = new SimpleKMeans();
            // kMeans++ parallel version of kMeans
            kMeans.setInitializationMethod(new SelectedTag(SimpleKMeans.KMEANS_PLUS_PLUS, SimpleKMeans.TAGS_SELECTION));

			/* other important parameters that might be set */
            // kMeans.setMaxIterations(n);
            // kMeans.setDistanceFunction(dfs);
            // kMeans.setSeed(10);

            //important parameter to set: preserver order, number of cluster.
            kMeans.setPreserveInstancesOrder(true);
            kMeans.setNumClusters(k);

            // run kMeans
            kMeans.buildClusterer(data);

            // This array returns the cluster number (starting with 0) for each instance
            // The array has as many elements as the number of instances
            int[] assignments = kMeans.getAssignments();

            // print out clusters
            int i = 0;
            for (int clusterNum : assignments) {
                System.out.printf("Instance %d -> Cluster %d \n", i, clusterNum);
                i++;
            }

            // print out the cluster centroids
            Instances centroids = kMeans.getClusterCentroids();
            for (i = 0; i < centroids.numInstances(); i++) {
                System.out.println("Centroid " + (i + 1) + ": " + centroids.instance(i));
            }

        } catch (Exception e) {
            throw new ClusteringException("kMeans()",
                    "Error running kMeans clustering.\n" + e.getMessage());
        }
    }

    /**
     * Simple example of Expectation Maximisation (EM)
     * clustering using Weka Java library.
     *
     * @param n        number of clusters
     * @param dataPath path to data
     */
    public void simpleEM(final int n, final String dataPath) {
        try {
            // load data
            Instances data = DataSource.read(dataPath);

            EM clusterer = new EM();
            clusterer.setNumClusters(n);
            clusterer.setMaxIterations(10);

            // run the algorithm
            clusterer.buildClusterer(data);

            double[] priors = clusterer.getClusterPriors();

            // print out clusters
            int i = 0;
            for (double prior : priors) {
                System.out.printf("Instance %d -> Prior %d \n", i, prior);
                i++;
            }
        } catch (Exception e) {
            throw new ClusteringException("EM()",
                    "Error running EM clustering.\n" + e.getMessage());
        }
    }

    /**
     * Simple example of Cobweb clustering
     * using Weka Java library.
     *
     * @param dataPath path to data
     */
    public void simpleCobweb(final String dataPath) {
        try {
            // load data
            ArffLoader loader = new ArffLoader();
            loader.setFile(new File(dataPath));

            Instances structure = loader.getStructure();

            // train Cobweb
            Cobweb cw = new Cobweb();
            cw.buildClusterer(structure);
            Instance current;
            while ((current = loader.getNextInstance(structure)) != null)
                cw.updateClusterer(current);
            cw.updateFinished();

        } catch (Exception e) {
            throw new ClusteringException("cobweb()",
                    "Error running Cobweb clustering.\n" + e.getMessage());
        }
    }

}
