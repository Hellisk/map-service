package traminer.util.trajectory.distance;

import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * EDwP: Edit Distance with Projections.
 * <p>
 * </br> Spatial dimension only.
 * </br> Robust to non-uniform sampling rates.
 * </br> Robust to local time shifts.
 * </br> Threshold free.
 * </br> Not a metric.
 * </br> Complexity: O((|T1|+|T2|)^2)
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class EDwPDistanceCalculator extends TrajectoryDistanceFunction {
    /**
     * @param dist The points distance measure to use.
     */
    public EDwPDistanceCalculator(PointDistanceFunction dist) {
        super(dist);
    }

    @Override
    public double getDistance(final Trajectory t1, final Trajectory t2) {
        // make sure the original trajectories are unchanged
        double dist = EDwP(t1.clone(), t2.clone());
        if (Double.isNaN(dist)) {
            return INFINITY;
        }
        return dist;
    }

    protected double EDwP(List<STPoint> t1, List<STPoint> t2) {
        if (t1.size() == 0 && t2.size() == 0) {
            return 0.0;
        }
        if (t1.size() == 0 || t2.size() == 0) {
            return INFINITY;
        }

        // project T2 onto T1
        List<STPoint> t1_with_projections = project(t2, t1);
        // project T1 onto T2
        List<STPoint> t2_with_projections = project(t1, t2);

        // both lists will have same size after projections
        int size = t1_with_projections.size();

        // coverage and replacement
        double coverage, replacement;
        double cost;

        // check which edit was cheaper
        double edwp_cost = 0.0;
        for (int i = 0; i < size - 1; i++) {
            // segment e1
            STPoint e1p1 = t1_with_projections.get(i);
            STPoint e1p2 = t1_with_projections.get(i + 1);
            // segment e2
            STPoint e2p1 = t2_with_projections.get(i);
            STPoint e2p2 = t2_with_projections.get(i + 1);

            // check if the segments are not already aligned
            coverage = coverage(e1p1, e1p2, e2p1, e2p2);
            if (coverage == 0.0) continue;

            // test t1 onto t2
            double min_cost_e1 = INFINITY;
            for (int j = 0; j < t2.size() - 1; j++) {
                STPoint p1 = t2.get(j);
                STPoint p2 = t2.get(j + 1);

                replacement = replacement(e1p1, e1p2, p1, p2);
                coverage = coverage(e1p1, e1p2, p1, p2);

                cost = coverage * replacement;
                if (cost < min_cost_e1 && cost != 0.0) {
                    min_cost_e1 = cost;
                }
            }

            // test t2 onto t1
            double min_cost_e2 = INFINITY;
            for (int j = 0; j < t1.size() - 1; j++) {
                STPoint p1 = t1.get(j);
                STPoint p2 = t1.get(j + 1);

                replacement = replacement(e2p1, e2p2, p1, p2);
                coverage = coverage(e2p1, e2p2, p1, p2);

                cost = coverage * replacement;
                if (cost < min_cost_e2) {
                    min_cost_e2 = cost;
                }
            }

            // get the cheapest edit
            edwp_cost += Math.min(min_cost_e1, min_cost_e2);
        }

        return edwp_cost;
    }

    /**
     * Project all points from t1 onto t2.
     * <p>
     * Find the cheapest projection of each point (shortest distance).
     *
     * @return Return a copy of t2 with the new projections from t1.
     */
    private List<STPoint> project(List<STPoint> t1, List<STPoint> t2) {
        List<STPoint> projList = new ArrayList<STPoint>();
        // find the best projection of every point in t1 onto t2
        double dist;
        STPoint ep1, ep2, proj;
        for (int i = 0; i < t1.size(); i++) {
            STPoint p = t1.get(i);
            double minDist = INFINITY;
            STPoint bestProj = new STPoint();
            for (int j = 0; j < t2.size() - 1; j++) {
                // t2 segment
                ep1 = t2.get(j);
                ep2 = t2.get(j + 1);
                // projection of p onto e
                proj = projection(ep1, ep2, p);
                // find the best pro
                dist = p.distance(proj, distFunc);
                if (dist < minDist) {
                    minDist = dist;
                    bestProj = proj;
                }
            }
            projList.add(bestProj);
        }
        // update and sort t2
        for (STPoint p : t2) {
            projList.add(p);
        }
        Collections.sort(projList, STPoint.TIME_COMPARATOR);

        return projList;
    }

    /**
     * Cost of the operation where the segment e1 is matched with e2.
     */
    private double replacement(STPoint e1p1, STPoint e1p2, STPoint e2p1, STPoint e2p2) {
        // distances between segment points
        double dist_p1 = e1p1.distance(e2p1, distFunc);
        double dist_p2 = e1p2.distance(e2p2, distFunc);

        // replacement cost
        double rep_cost = dist_p1 + dist_p2;

        return rep_cost;
    }

    /**
     * Coverage: quantifies how representative the segment being
     * edit are of the overall trajectory. Edges e1 and e2.
     * e1 = [e1.p1, e1.p2], e2 = [e2.p1, e2.p2]
     */
    private double coverage(STPoint e1p1, STPoint e1p2,
                            STPoint e2p1, STPoint e2p2) {
        // segments coverage
        double cover = e1p1.distance(e1p2, distFunc) +
                e2p1.distance(e2p2, distFunc);
        return cover;
    }

    /**
     * Calculate the projection of the point p on to the segment
     * e = [e.p1, e.p2]
     */
    private STPoint projection(STPoint e_p1, STPoint e_p2, STPoint p) {
        // square length of the segment
        double len_2 = dotProduct(e_p1, e_p2, e_p1, e_p2);
        // e.p1 and e.p2 are the same point
        if (len_2 == 0) {
            return e_p1;
        }

        // the projection falls where t = [(p-e.p1) . (e.p2-e.p1)] / |e.p2-e.p1|^2
        double t = dotProduct(e_p1, p, e_p1, e_p2) / len_2;

        // "Before" e.p1 on the line
        if (t < 0.0) {
            return e_p1;
        }
        // after e.p2 on the line
        if (t > 1.0) {
            return e_p2;
        }
        // projection is "in between" e.p1 and e.p2
        // get projection coordinates
        double x = e_p1.x() + t * (e_p2.x() - e_p1.x());
        double y = e_p1.y() + t * (e_p2.y() - e_p1.y());
        long time = (long) (e_p1.time() + t * (e_p2.time() - e_p1.time()));

        return new STPoint(x, y, time);
    }

    /**
     * Calculates the dot product between two segment e1 and e2.
     */
    private double dotProduct(STPoint e1_p1, STPoint e1_p2, STPoint e2_p1, STPoint e2_p2) {
        // shift the points to the origin - vector
        double e1_x = e1_p2.x() - e1_p1.x();
        double e1_y = e1_p2.y() - e1_p1.y();
        double e2_x = e2_p2.x() - e2_p1.x();
        double e2_y = e2_p2.y() - e2_p1.y();

        // calculate the dot product
        double dot_product = (e1_x * e2_x) + (e1_y * e2_y);

        return dot_product;
    }
}
