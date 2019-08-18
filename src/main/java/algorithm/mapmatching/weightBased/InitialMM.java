//package algorithm.mapmatching.weightBased;
//
//import util.dijkstra.RoutingGraph;
//import util.function.DistanceFunction;
//import util.function.GreatCircleDistanceFunction;
//import util.object.spatialobject.Point;
//import util.object.spatialobject.Segment;
//import util.object.structure.Pair;
//import util.object.structure.PointMatch;
//import util.object.structure.Triplet;
//import util.settings.MapMatchingProperty;
//
//import java.io.IOException;
//import java.util.*;
//
///**
// * Initial MM
// * Implementation of initial map-matching in Quddus, M., & Washington, S. (2015).
// */
//public class InitialMM {
//    private static MapMatchingProperty property = new MapMatchingProperty();
//    private static RoutingGraph routingGraph = new RoutingGraph(Utilities.getRoadNetworkGraph(), false, property);
//
//
//    /**
//     * Find all shortest paths between each candidate pair
//     *
//     * @param sources      candidate set of initial point
//     * @param destinations candidate set of second point
//     * @param maxDistance  searching threshold
//     * @return Map<Pair <sourcePM, destinationPM>, Pair<shortestPathLength, PathSequence>>
//     */
//    private Map<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> getAllShortestPaths(
//            List<PointMatch> destinations, List<PointMatch> sources, double maxDistance) {
//
//        int count = 0;
//        Map<Pair<PointMatch, PointMatch>, Pair<Double,List<String>>> shortestPaths = new HashMap<>();
//        for (PointMatch source : sources) {
//
//            // List<DestinationPM, shortestPathLength, Path>
//            List<Triplet<PointMatch, Double, List<String>>> shortestPathToDestPm
//                    = Utilities.getShortestPaths(routingGraph, destinations, source, maxDistance);
//
//            for (Triplet<PointMatch, Double, List<String>> triplet : shortestPathToDestPm) {
//                if (triplet._2() != Double.POSITIVE_INFINITY) {
//                    /* valid path */
//                    count += 1;
//                    shortestPaths.put(new Pair<>(source, triplet._1()), new Pair<>(triplet._2(), triplet._3()));
//                }
//            }
//        }
//        return shortestPaths;
//    }
//
//
//
//    public static void main(String[] args) throws IOException {
//        InitialMM inimm = new InitialMM();
//
//        DistanceFunction df = new GreatCircleDistanceFunction();
//        Point p1 = new Point(116.42743, 39.95933, df);
//        Point p2 = new Point(116.42804, 39.95936, df);
//
//        double heading2 = 98.0;
//
//        List<PointMatch> neighboursFirst = Utilities.searchNeighbours(p1, 50);
//        List<PointMatch> neighboursSec = Utilities.searchNeighbours(p2, 50);
//
//        Map<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>> candiPaths =
//                inimm.getAllShortestPaths(neighboursFirst, neighboursSec, 1000);
//
//        for (Map.Entry<Pair<PointMatch, PointMatch>, Pair<Double, List<String>>>
//                candipath: candiPaths.entrySet()) {
//            List<String> pathSequence = candipath.getValue()._2();
//
//            StringBuilder sb = new StringBuilder(pathSequence.get(0));
//            for (int i = 1; i < pathSequence.size(); i++) {
//                sb.append(" > ");
//                sb.append(pathSequence.get(i));
//            }
//            String path = sb.toString();
//
//            System.out.println(candipath.getKey()._1().getRoadID()+" > "+candipath.getKey()._1().getRoadID()
//            +": "+candipath.getValue()._1()+" m ||"+path);
//        }
//        System.out.println(candiPaths.size());
//
//
////        Queue<Pair<Pair<PointMatch, PointMatch>, Double>> scores = Utilities.rankCandiMatches(
////                candiPaths, p1, p2, 1000, heading2,12,21, 32,35);
////
////        while (!scores.isEmpty()) {
////            Pair<Pair<PointMatch, PointMatch>, Double> ele = scores.poll();
////            System.out.println(ele._1()._1().getRoadID()+"|"+ele._1()._2().getRoadID()+"|"+ ele._2());
////        }
//    }
//}
