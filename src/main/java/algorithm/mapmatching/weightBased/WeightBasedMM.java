package algorithm.mapmatching.weightBased;


import util.dijkstra.RoutingGraph;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.MatchResultWriter;
import util.io.TrajectoryReader;
import util.object.spatialobject.Point;
import util.object.spatialobject.Trajectory;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.object.structure.Triplet;
import util.settings.MapMatchingProperty;

import java.io.IOException;
import java.util.*;

/**
 * Subsequent MM
 * Implementation of initial map-matching in Quddus, M., & Washington, S. (2015).
 */
public class WeightBasedMM {

    private List<String> matchedWaySequence = new ArrayList<>();
    private List<PointMatch> matchedPointSequence = new ArrayList<>();
    private MapMatchingProperty property = new MapMatchingProperty();
    private RoutingGraph routingGraph;
    private double djkstraThreshold;
    private double headingWC;
    private double bearingWC;
    private double pdWC;
    private double shortestPathWC;
    private double radiusM;
    private DistanceFunction distFunc = new GreatCircleDistanceFunction();
    private List<Pair<Integer, List<String>>> outputRouteMatchResult = new ArrayList<>();
    private List<Pair<Integer, List<PointMatch>>> outputPointMatchResult = new ArrayList<>();
    private String routeMatchResultFolder;
    private String pointMatchingResultFolder;
    private String traFolder;

    /**
     * @param mapFolder                 road network folder
     * @param djkstraThreshold
     * @param candidateRadius
     * @param headingWC
     * @param bearingWC
     * @param pdWC
     * @param shortestPathWC
     * @param routeMatchResultFolder
     * @param pointMatchingResultFolder
     * @param traFolder
     */
    public WeightBasedMM(
            String mapFolder,
            double djkstraThreshold, double candidateRadius,
            double headingWC, double bearingWC, double pdWC, double shortestPathWC,
            String routeMatchResultFolder, String pointMatchingResultFolder, String traFolder) {
        routingGraph = new RoutingGraph(Utilities.getRoadNetworkGraph(mapFolder), false, property);
        djkstraThreshold = 1000;
        candidateRadius = 50;
        headingWC = 12;
        bearingWC = 21;
        pdWC = 32;
        shortestPathWC = 35;
        routeMatchResultFolder = "/Users/macbookpro/Desktop/capstone/Beijing-S/outputRouteResult";
        pointMatchingResultFolder = "/Users/macbookpro/Desktop/capstone/Beijing-S/outputPointResult";
        traFolder = "/Users/macbookpro/Desktop/capstone/Beijing-S/TrajFolder";
        ;

    }

    /**
     * Find all shortest paths between each candidate pair
     *
     * @param sources      candidate set of initial point
     * @param destinations candidate set of second point
     * @param maxDistance  searching threshold
     * @return Map<Pair < sourcePM, destinationPM>, Pair<shortestPathLength, PathSequence>>
     */
    private Map<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> getAllShortestPaths(
            List<PointMatch> destinations, List<PointMatch> sources, double maxDistance) {

        Map<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> shortestPaths = new HashMap<>();
        for (PointMatch source : sources) {

            // List<DestinationPM, shortestPathLength, Path>
            List<Triplet<PointMatch, Double, List<String>>> shortestPathToDestPm
                    = Utilities.getShortestPaths(routingGraph, destinations, source, maxDistance);

            for (Triplet<PointMatch, Double, List<String>> triplet : shortestPathToDestPm) {
                if (triplet._2() != Double.POSITIVE_INFINITY) {
                    /* valid path */
                    shortestPaths.put(new Pair<>(source, triplet._1()), new Pair<>(triplet._2(), triplet._3()));
                }
            }
        }
        return shortestPaths;
    }

    /**
     * Initial map-matching
     *
     * @param firstPoint     first GPS point
     * @param secondPoint    second GPS point
     * @param vehicleHeading heading of the second GPS point
     * @return point match of the second GPS point
     * @throws IOException file not found
     */
    private PointMatch initialMM(
            Point firstPoint, Point secondPoint, double vehicleHeading) throws IOException {
        List<PointMatch> firstCandiPMs = Utilities.searchNeighbours(firstPoint, radiusM);
        List<PointMatch> secCandiPMs = Utilities.searchNeighbours(secondPoint, radiusM);

        // double is shortest path length
        Map<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> candiPaths =
                getAllShortestPaths(firstCandiPMs, secCandiPMs, djkstraThreshold);

        // double is tws
        Queue<Pair<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>>> scoredCandiPaths =
                Utilities.rankCandiMatches(
                        candiPaths, firstPoint, secondPoint, djkstraThreshold, vehicleHeading,
                        headingWC, bearingWC, pdWC, shortestPathWC);

        Utilities.updateMatchedWays(matchedWaySequence, scoredCandiPaths);
//        System.out.println("initialMM finds "+scoredCandiPaths.size()+" paths");
//        System.out.println("initial mm");
        matchedPointSequence.add(Utilities.bestCandi(scoredCandiPaths));
        return Utilities.bestCandi(scoredCandiPaths);
    }

    private PointMatch subsqtMM(
            Point prePoint, Point curPoint, PointMatch prevMatchedPM, double vehicleHeading) throws IOException {
        List<PointMatch> secCandiPMs = Utilities.searchNeighbours(curPoint, radiusM);

        // List<DestinationPM, shortestPathLength, Path>
        List<Triplet<PointMatch, Double, List<String>>> candiPaths =
                Utilities.getShortestPaths(routingGraph, secCandiPMs, prevMatchedPM, djkstraThreshold);

        // double is shortest path length
        Map<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> shortestPaths = new HashMap<>();
        for (Triplet<PointMatch, Double, List<String>> triplet : candiPaths) {
            if (triplet._2() != Double.POSITIVE_INFINITY) {
                /* valid path */
                shortestPaths.put(new Pair<>(prevMatchedPM, triplet._1()), new Pair<>(triplet._2(), triplet._3()));
            }
        }

        // double is tws
        Queue<Pair<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>>> scoredCandiPaths =
                Utilities.rankCandiMatches(
                        shortestPaths, prePoint, curPoint, djkstraThreshold, vehicleHeading,
                        headingWC, bearingWC, pdWC, shortestPathWC);

        if (scoredCandiPaths.size() == 0) {
            // subsequent map matcing for this candidate pair failed
            // invoke handleBreakingPoint method
//            System.out.println("No candidate path between" +
//                    prePoint.x() + " " + prePoint.y() + " and " + curPoint.x() + " " + curPoint.y());
//            System.out.println("Number of candidate points of curPoint: " + secCandiPMs.size());
//            System.out.println("Number of candidate paths: "+ candiPaths.size());
//            System.out.println("Number of shortest paths: "+shortestPaths.size());
            return handleBreakingPoint(prePoint, curPoint, vehicleHeading);
        }

        Utilities.updateMatchedWays(matchedWaySequence, scoredCandiPaths);
        matchedPointSequence.add(Utilities.bestCandi(scoredCandiPaths));
        return Utilities.bestCandi(scoredCandiPaths);
    }

    private void resetMatchedResult() {
        matchedWaySequence = new ArrayList<>();
        matchedPointSequence = new ArrayList<>();
    }


    private PointMatch handleBreakingPoint(
            Point firstPoint, Point secondPoint, double vehicleHeading) throws IOException {
        // handle breaking point
        return initialMM(firstPoint, secondPoint, vehicleHeading);
    }

    private void doMatching(final Trajectory trajectory) throws IOException {
        // initialMM
        PointMatch secdPM = initialMM(trajectory.get(0), trajectory.get(1), trajectory.get(1).heading());

        Point prePoint = trajectory.get(1);
        PointMatch prePM = secdPM;

        for (int i = 2; i < trajectory.getSTPoints().size(); i++) {
            // subsqtMM
            prePM = subsqtMM(prePoint, trajectory.get(i), prePM, trajectory.get(i).heading());
            prePoint = trajectory.get(i);
        }
        // Store map-matching result
        outputRouteMatchResult.add(new Pair<>(Integer.parseInt(trajectory.getID()), matchedWaySequence));
        MatchResultWriter.writeRouteMatchResults(outputRouteMatchResult, routeMatchResultFolder);

        outputPointMatchResult.add(new Pair<>(Integer.parseInt(trajectory.getID()), matchedPointSequence));
        MatchResultWriter.writePointMatchResults(outputPointMatchResult, pointMatchingResultFolder);


        // Map matching finish. Now evaluate!
//        String gtFolderPath = "/Users/macbookpro/Desktop/capstone/Beijing-S/RouteMatchFolder";
//        File gtFolder = new File(gtFolderPath);
//        List<String> gtWays = new ArrayList<>();
//
//        for (File fileEntry_ : Objects.requireNonNull(gtFolder.listFiles())) {
//            String trjGtIndex = fileEntry_.getName().split("[^a-zA-Z0-9']+")[1];
//            if (trajectory.getID().equals(trjGtIndex)) {
//                BufferedReader br_ = new BufferedReader(new FileReader(gtFolderPath + "/" + fileEntry_.getName()));
//
//                String gtWay = br_.readLine();
//                while (gtWay != null) {
//                    gtWays.add(gtWay.strip());
//                    gtWay = br_.readLine();
//                }
//            }
//        }
//
//        float correctMatchedWays = 0; // a correct matched way is a way in both estimated set and ground truth
//        for (String gtWay : gtWays) {
//            for (String way : matchedWaySequence) {
//                if (way.equals(gtWay)) {
//                    correctMatchedWays += 1;
//                    break; // avoid a road way been matched twice
//                }
//            }
//        }
//
//        float precision = correctMatchedWays / matchedWaySequence.size();
//        float recall = correctMatchedWays / gtWays.size();
//        float f = 2 * precision * recall / (precision + recall);
//
//        System.out.println("precision: " + precision + ", recall: " + recall + ", f-score: " + f);
//        System.out.println(matchedWaySequence);
        resetMatchedResult();
//        System.out.println("\n");
    }

    public void voidWeightBasedMatching() throws IOException {
        List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(traFolder, distFunc);
        System.out.println("inputTrajList size: " + inputTrajList.size());

        for (Trajectory curTraj : inputTrajList) {
            doMatching(curTraj);
            System.out.println(curTraj.getID());
        }

    }

    public static void main(String[] args) throws IOException {
        /* load beijing trajectories */
//        BeijingTrajectoryLoader bjTrjLoader =
//                new BeijingTrajectoryLoader(500, 500, 120);
////
//        bjTrjLoader.readTrajWithGTRouteMatchResult(
//                Utilities.getRoadNetworkGraph(),
//                "/Users/macbookpro/Desktop/capstone/Beijing-S/raw/trajectory/beijingTrajectory.csv",
//                "/Users/macbookpro/Desktop/capstone/Beijing-S/TrajFolder",
//                "/Users/macbookpro/Desktop/capstone/Beijing-S/RouteMatchFolder");

        /* Map-matching*/
//        List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(traFolder, distFunc);
//        System.out.println("inputTrajList size: " + inputTrajList.size());
//
//        for(Trajectory curTraj:inputTrajList){
//            doMatching(curTraj);
//            System.out.println(curTraj.getID());
//        }
//        float precisions = 0;
//        float recalls = 0;
//        float fs = 0;
//
//        System.out.println("~~~~~~~~~~~~~~~~~~~");
//        System.out.println("average precision: " + precisions / 95);
//        System.out.println("average recall: " + recalls / 95);
//        System.out.println("average fs: " + fs / 95);
    }
}
