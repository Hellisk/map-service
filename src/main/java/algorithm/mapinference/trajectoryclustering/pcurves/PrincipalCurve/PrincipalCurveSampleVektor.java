package algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve;

import algorithm.mapinference.trajectoryclustering.pcurves.LinearAlgebra.LineSegmentAbstract;
import algorithm.mapinference.trajectoryclustering.pcurves.LinearAlgebra.Vektor;
import algorithm.mapinference.trajectoryclustering.pcurves.LinearAlgebra.Vektor2D;
import algorithm.mapinference.trajectoryclustering.pcurves.LinearAlgebra.Weighted;

final public class PrincipalCurveSampleVektor extends Vektor2D implements Weighted {
    static public Vektor prototypeVektor;
    public Object cluster;
    private int indexOfNearestSegment;
    // An UPPER BOUND of the distance from the second nearest segment
    private double distanceFromSecondNearestSegment;
    private double weight;

    public PrincipalCurveSampleVektor(Vektor vektor) {
        super(vektor);
        if (vektor instanceof Weighted)
            weight = ((Weighted) vektor).GetWeight();
        else
            weight = 1;

    }

    @Override
    final public double GetWeight() {
        return weight;
    }

    final public void DecrementEdgeIndexes(int oei) {
        if (indexOfNearestSegment > oei)
            indexOfNearestSegment--;
            // If we delete the nearest segment, set the index to invalid
        else if (indexOfNearestSegment == oei)
            indexOfNearestSegment = -1;
    }

    final public void Initialize() {
        indexOfNearestSegment = 0;
        distanceFromSecondNearestSegment = 0.0;
        cluster = null;
    }

    final public int GetIndexOfNearestSegment() {
        return indexOfNearestSegment;
    }

    final public void SetIndexOfNearestSegment(int index) {
        indexOfNearestSegment = index;
    }

    // Recomputes the distance from the nearest segment over all segments, assuming that
    // the largest change in vertex positions before last update is maxChange, and the
    // maximum possible distance is maxDistance (usually equals to the diameter of the data).
    final public double GetDist2FromNearestSegment(double maxChange, double maxDistance,
                                                   LineSegmentAbstract[] lineSegments) {
        // If the difference between the distance from the nearest line segment and the distance
        // from the second nearest line segment is more than twice the maximum change of position
        // over all vertices of the curve, the nearest neighbor region of the sample point could not
        // have changed.
        double distanceFromNearestSegment;
        try {
            distanceFromNearestSegment = Dist2(lineSegments[indexOfNearestSegment]);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Deletion has set indexOfNearestSegment to -1
            if (indexOfNearestSegment == -1)
                distanceFromNearestSegment = maxDistance;
            else
                throw e;
        }
        distanceFromSecondNearestSegment -= maxChange;
        if (distanceFromSecondNearestSegment - distanceFromNearestSegment <= 0) {
            distanceFromSecondNearestSegment = 2 * maxDistance;
            double d;
            // The following loop is the bottleneck (O(nk))
            // ////////////////////////////////////////////
            distanceFromNearestSegment /= 2;
            do {
                distanceFromNearestSegment *= 2;
                for (int i = 0; i < lineSegments.length; i++) {
                    if ((d = Dist2(lineSegments[i])) < distanceFromNearestSegment) {
                        distanceFromSecondNearestSegment = distanceFromNearestSegment;
                        distanceFromNearestSegment = d;
                        indexOfNearestSegment = i;
                    } else if (d < distanceFromSecondNearestSegment && i != indexOfNearestSegment)
                        distanceFromSecondNearestSegment = d;
                }
            } while (indexOfNearestSegment == -1); // in case maxDistance was too small
            // ////////////////////////////////////////////
        }
        return distanceFromNearestSegment;
    }

    // Recomputes the distance from the nearest segment over the two given segments, assuming that
    // the two segments were created by adding a midpoint to the nearest segment
    final public double GetDist2FromNearestSegment(int edgeIndex1, int edgeIndex2, LineSegmentAbstract[] lineSegments) {
        double d1 = Dist2(lineSegments[edgeIndex1]);
        double d2 = Dist2(lineSegments[edgeIndex2]);
        if (d1 < d2) {
            indexOfNearestSegment = edgeIndex1;
            if (distanceFromSecondNearestSegment > d2 || lineSegments.length == 2)
                distanceFromSecondNearestSegment = d2;
            return d1;
        } else {
            indexOfNearestSegment = edgeIndex2;
            if (distanceFromSecondNearestSegment > d1 || lineSegments.length == 2)
                distanceFromSecondNearestSegment = d1;
            return d2;
        }
    }

    @Override
    public String toString() {
        return super.toString() + " cluster = " + cluster;
    }

    // final public void Paint(Graphics g,DataCanvas canvas,int pixelSize,String type) {
    // if (cluster != null)
    // g.setColor(ColorChoice.colors[Math.abs(cluster.hashCode()) % ColorChoice.NUM_OF_COLORS]);
    // super.Paint(g,canvas,pixelSize,type);
    // }

}
