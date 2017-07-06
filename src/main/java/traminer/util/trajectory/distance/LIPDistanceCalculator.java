package traminer.util.trajectory.distance;

import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.Polygon;
import traminer.util.spatial.objects.Segment;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * LIP: trajecotry distance measure.
 *
 * @author uqhsu1, uqdalves
 */
@SuppressWarnings("serial")
public class LIPDistanceCalculator extends TrajectoryDistanceFunction {
    private List<Polygon> polygon;
    private List<Double> weight;

    /**
     * Aux index
     */
    class TwoInt {
        public int x, y;

        public TwoInt(int i, int j) {
            this.x = i;
            this.y = j;
        }
    }

    /**
     * @param dist The points distance measure to use.
     */
    public LIPDistanceCalculator(PointDistanceFunction dist) {
        super(dist);
    }

    @Override
    public double getDistance(final Trajectory t1, final Trajectory t2) {
        // make sure the original trajectories are unchanged
        return LIP(t1.clone(), t2.clone());
    }

    private double LIP(List<STPoint> r, List<STPoint> s) {
        polygon = getPolygon(r, s);
        double result = 0;

        for (int i = 0; i < polygon.size(); i++) {
            result += polygon.get(i).area() * weight.get(i).doubleValue();
        }
        return result;
    }

    private double getLength(List<? extends Point> p) {
        double result = 0.0;
        for (int i = 0; i < p.size() - 1; i++) {
            result += distFunc.distance(p.get(i), p.get(i + 1));
        }
        return result;        
    }

    private List<Polygon> getPolygon(List<STPoint> r, List<STPoint> s) {
        List<Polygon> result = new ArrayList<Polygon>();

        weight = new ArrayList<Double>();
        double lengthR = getLength(r);
        double lengthS = getLength(s);

        List<Segment> rl = getPolyline(r);
        List<Segment> sl = getPolyline(s);
        
        List<Point> intersections = new ArrayList<Point>();
        List<TwoInt> index = new ArrayList<TwoInt>();

        boolean[] used = new boolean[sl.size()];
        for (int i = 0; i < used.length; i++) {
            used[i] = false;
        }

        for (int i = 0; i < rl.size(); i++) {
            for (int j = 0; j < sl.size(); j++) {
                if (used[j]) {
                    continue;
                }

                Point inter = rl.get(i).getIntersection(sl.get(j));
                if (inter == null) {
                    continue;
                }
                double x = inter.x();
                double y = inter.y();

                double r1x = r.get(i).x();
                double r1y = r.get(i).y();


                double r2x = r.get(i + 1).x();
                double r2y = r.get(i + 1).y();

                double temp = 0;
                if (r1x > r2x) {
                    temp = r1x;
                    r1x = r2x;
                    r2x = temp;
                }
                if (r1y > r2y) {
                    temp = r1y;
                    r1y = r2y;
                    r2y = temp;
                }

                double s1x = s.get(j).x();
                double s1y = s.get(j).y();

                double s2x = s.get(j + 1).x();
                double s2y = s.get(j + 1).y();

                if (s1x > s2x) {
                    temp = s1x;
                    s1x = s2x;
                    s2x = temp;
                }

                if (s1y > s2y) {
                    temp = s1y;
                    s1y = s2y;
                    s2y = temp;
                }

                if (x >= r1x && x <= r2x && y >= r1y && y <= r2y &&
                        x >= s1x && x < s2x && y >= s1y && y <= s2y) {
                    Point tempP = new Point(x, y);
                    TwoInt tempI = new TwoInt(i, j);

                    intersections.add(tempP);
                    index.add(tempI);
                    for (int k = 0; k <= j; k++) {
                        used[k] = true;
                    }

                    if (intersections.size() == 1) {
                        List<Point> tempPolyPoints = new ArrayList<Point>();

                        for (int ii = 0; ii <= i; ii++) {
                            tempPolyPoints.add(r.get(ii));
                        }

                        tempPolyPoints.add(tempP);

                        for (int ii = j; ii >= 0; ii--) {
                            tempPolyPoints.add(s.get(ii));
                        }

                        List<Point> tempR = new ArrayList<Point>();
                        for (int ii = 0; ii <= i; ii++) {
                            tempR.add(r.get(ii));
                        }

                        List<Point> tempS = new ArrayList<Point>();
                        for (int ii = 0; ii <= j; ii++) {
                            tempS.add(s.get(ii));
                        }

                        double lengthRSub = getLength(tempR);
                        double lengthSSub = getLength(tempS);

                        double weightV = (lengthRSub + lengthSSub) / (lengthR + lengthS);

                        weight.add(new Double(weightV));
                        Polygon poly = new Polygon();
                        poly.addAll(tempPolyPoints);
                        result.add(poly);
                    } else {
                        ArrayList<Point> tempPolyPoints = new ArrayList<Point>();
                        tempPolyPoints.add(intersections.get(intersections.size() - 2));

                        for (int ii = index.get(index.size() - 2).x + 1; ii <= i; ii++) {
                            tempPolyPoints.add(r.get(ii));
                        }

                        tempPolyPoints.add(tempP);

                        for (int ii = j; ii >= index.get(index.size() - 2).y + 1; ii--) {
                            tempPolyPoints.add(s.get(ii));
                        }

                        ArrayList<Point> tempR = new ArrayList<Point>();
                        for (int ii = index.get(index.size() - 2).x + 1; ii <= i; ii++) {
                            tempR.add(r.get(ii));
                        }

                        ArrayList<Point> tempS = new ArrayList<Point>();
                        for (int ii = index.get(index.size() - 2).y + 1; ii <= j; ii++) {
                            tempS.add(s.get(ii));
                        }

                        double lengthRSub = getLength(tempR);
                        double lengthSSub = getLength(tempS);

                        double weightV = (lengthRSub + lengthSSub) / (lengthR + lengthS);

                        weight.add(new Double(weightV));
                        Polygon poly = new Polygon();
                        poly.addAll(tempPolyPoints);
                        result.add(poly);
                    }
                }
            }
        }

        return result;
    }
}