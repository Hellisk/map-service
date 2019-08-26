package algorithm.mapmatching.simpleHMM;

import util.function.DistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Point;
import util.object.spatialobject.Segment;
import util.object.structure.PointMatch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Minset {
    /**
     * Removes semantically redundant matching candidates from a set of matching candidates (as
     * PointMatch object) and returns a minimized (reduced) subset.
     * <p>
     * Given a position measurement, a matching candidate is each road in a certain radius of the
     * measured position, and in particular that point on each road that is closest to the measured
     * position. Hence, there are as many state candidates as roads in that area. The idea is to
     * conserve only possible routes through the area and use each route with its closest point to
     * the measured position as a matching candidate. Since roads are split into multiple segments,
     * the number of matching candidates is significantly higher than the respective number of
     * routes. To give an example, assume the following matching candidates as PointMatch
     * objects with a road id and a fraction:
     * (ri, 0.5)
     * (rj, 0.0)
     * (rk, 0.0)
     * where they are connected as ri → rj and ri → rk.
     * Here, matching candidates rj and rk can be removed if we see routes as matching candidates.
     * This is because both, rj and rk, are reachable from ri.
     * Note: Of course, rj and rk may be seen as relevant matching candidates, however, in the present HMM map matching
     * algorithm there is no optimization of matching candidates along the road, instead it only considers the closest
     * point of a road as a matching candidate.
     *
     * @param candidates Set of matching candidates as PointMatch objects.
     * @return Minimized (reduced) set of matching candidates as PointMatch objects.
     */
    public static HashSet<PointMatch> minimize(List<PointMatch> candidates, RoadNetworkGraph roadMap, DistanceFunction distFunc) {

        HashMap<String, PointMatch> candiPMset = new HashMap<>();
        HashMap<String, Integer> misses = new HashMap<>();
        HashSet<String> removes = new HashSet<>();

        for (PointMatch candidate : candidates) {
            candiPMset.put(candidate.getRoadID(), candidate);
            misses.put(candidate.getRoadID(), 0);
        }

        for (PointMatch candidate : candidates) {
            // fake PMs, where the point is source node of the segment successive to candidates
            HashSet<PointMatch> successors = new HashSet<>();

            String wayID = candidate.getRoadID().split("\\|")[0];
            int segmentIdx = Integer.parseInt(candidate.getRoadID().split("\\|")[1]);

            RoadWay way = roadMap.getWayByID(wayID);
            if (segmentIdx < way.getNodes().size() - 2) {
                // this segment is not the last segment in the way
                RoadNode startNode = way.getNode(segmentIdx + 1);
                double[] curCoord = new double[]{startNode.lon(), startNode.lat()};

                RoadNode endNode = way.getNode(segmentIdx + 2);
                double[] nextCoord = new double[]{endNode.lon(), endNode.lat()};

                Segment sg = new Segment(curCoord[0], curCoord[1], nextCoord[0], nextCoord[1], roadMap.getDistanceFunction());

                successors.add(
                        new PointMatch(new Point(curCoord[0], curCoord[1], roadMap.getDistanceFunction()),
                                sg, way.getID() + "|" + segmentIdx + 1));
            } else {
                // this segment is the last segment in the way, get the first segment of the successive way
                Set<RoadWay> sucWays = roadMap.getWayByID(wayID).getToNode().getOutGoingWayList();
                for (RoadWay sucWay : sucWays) {
                    RoadNode startNode = sucWay.getNode(0);
                    double[] curCoord = new double[]{startNode.lon(), startNode.lat()};

                    RoadNode endNode = sucWay.getNode(1);
                    double[] nextCoord = new double[]{endNode.lon(), endNode.lat()};

                    Segment sg = new Segment(curCoord[0], curCoord[1], nextCoord[0], nextCoord[1], roadMap.getDistanceFunction());

                    successors.add(
                            new PointMatch(new Point(curCoord[0], curCoord[1], roadMap.getDistanceFunction()),
                                    sg, sucWay.getID() + "|" + 0));
                }
            }


            String id = candidate.getRoadID(); // wayid + | + segment index

            // for each successor of this candidate
            for (PointMatch successor : successors) {
                // this successor has not been flagged
                if (!candiPMset.containsKey(successor.getID())) {
                    misses.put(id, misses.get(id) + 1);
                }

                // this successor has been flagged as candidate, but it is near to segment source node, remove it
                if (candiPMset.containsKey(successor.getID())
                        && round(fraction(distFunc,
                        candidate.getMatchedSegment().x1(), candidate.getMatchedSegment().y1(),
                        candidate.getMatchedSegment().x2(), candidate.getMatchedSegment().y2(),
                        candidate.lon(), candidate.lat())) == 0) {
                    removes.add(successor.getID());
                    misses.put(id, misses.get(id) + 1);
                }
            }
        }

        for (PointMatch candidate : candidates) {
            String id = candidate.getRoadID();
            if (candiPMset.containsKey(id) && !removes.contains(id)
                    && round(fraction(distFunc,
                    candidate.getMatchedSegment().x1(), candidate.getMatchedSegment().y1(),
                    candidate.getMatchedSegment().x2(), candidate.getMatchedSegment().y2(),
                    candidate.lon(), candidate.lat())) == 1
                    && misses.get(id) == 0) {
                removes.add(id);
            }
        }

        for (String id : removes) {
            candiPMset.remove(id);
        }

        return new HashSet<>(candiPMset.values());
    }

    private static double fraction(DistanceFunction distFunc,
                                   double sourceNodex, double sourceNodey, double targetNodex, double targetNodey, double pointx, double pointy) {
        return distFunc.pointToPointDistance(pointx, pointy, sourceNodex, sourceNodey)
                / distFunc.pointToPointDistance(sourceNodex, sourceNodey, targetNodex, targetNodey);
    }


    private static double round(double value) {
        double precision = 1E-8;
        return Math.round(value / precision) * precision;
    }

}
