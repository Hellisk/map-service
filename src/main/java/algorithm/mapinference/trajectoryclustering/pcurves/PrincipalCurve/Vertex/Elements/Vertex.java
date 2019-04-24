package algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.Elements;

import algorithm.mapinference.trajectoryclustering.pcurves.LinearAlgebra.*;
import algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.PrincipalCurveSampleVektor;
import algorithm.mapinference.trajectoryclustering.pcurves.Utilities.MyMath;

abstract public class Vertex extends Vektor2D {
    static public Vektor prototypeVektor;
    public int label; // for marking checked vertices in creating paths, for filtering vertices
    private VertexCluster set;
    private double n = ElementVertex.principalCurve.GetSampleWeight();

    protected Vertex(Vektor vektor) {
        super(vektor);
        set = new VertexCluster(PrincipalCurveSampleVektor.prototypeVektor);
    }

    protected Vertex(Vertex vertex) {
        super(vertex);
        set = vertex.set;
    }

    static protected boolean CornerAngle(double angle) {
        return angle < 100;
    }

    static protected boolean RectAngle(double angle) {
        return CornerAngle(angle) && CornerAngle(180 - angle);
    }

    static protected boolean LineAngle(double angle) {
        return CornerAngle(180 - angle / 2);
    }

    abstract public int GetDegree();

    abstract public Edge[] GetEdges();

    abstract public Vertex[] GetNeighbors();

    abstract public int[] GetEdgeIndexes();

    abstract public int[] GetNeighborIndexes();

    public abstract ElementVertex[] GetElementVertices();

    abstract public void Maintain(Vertex neighbor);

    abstract public Vertex Degrade(Vertex neighbor);

    protected abstract ElementVertex GetMainElementVertex();

    abstract public double GetPenalty();

    protected abstract NumeratorAndDenominator GetNumeratorAndDenominatorForPenalty();

    // abstract public Vektor GetTangent(Vertex neighbor);
    abstract public Vertex Restructure();

    // Final functions
    final public void AddPointToSet(PrincipalCurveSampleVektor samplePoint) {
        if (samplePoint.cluster != this) {
            set.AddPoint(samplePoint);
            samplePoint.cluster = this;
        }
    }

    final public void DeleteMovedPointsFromSet() {
        set.DeleteMovedPoints(this);
    }

    final public void ResetSet() {
        set.Reset();
    }

    final public int GetSetSize() {
        return set.GetSize();
    }

    final public PrincipalCurveSampleVektor GetSetPointAt(int index) {
        return (PrincipalCurveSampleVektor) set.GetSetPointAt(index);
    }

    final public double GetSetWeight() {
        return set.GetWeight();
    }

    final public double GetMSETimesWeight() {
        return set.GetMSETimesWeight(this);
    }

    final public NumeratorAndDenominator GetNumeratorAndDenominatorForMSETimesWeight() {
        NumeratorAndDenominator nd = new NumeratorAndDenominator(PrincipalCurveSampleVektor.prototypeVektor);
        nd.numerator.AddEqual(set.GetSum());
        nd.denominator += set.GetWeight();
        return nd;
    }

    final public Vektor NewVertex() {
        NumeratorAndDenominator nd = GetNumeratorAndDenominatorForMSE();
        nd.AddEqual(GetNumeratorAndDenominatorForPenalty());
        if (MyMath.equals(nd.denominator, 0))
            return this;
        else
            return nd.Resolve();
    }

    final protected EndVertexOfTwo MaintainEndVertex(Vertex neighbor) {
        try {
            HasTwoSymmetricEdges realNeighbor = (HasTwoSymmetricEdges) neighbor;
            return new EndVertexOfThree(this, realNeighbor.GetEdgeIndex(this).edgeIndex, realNeighbor
                    .GetOppositeEdgeIndex(this).edgeIndex);
        } catch (ClassCastException e1) {
            try {
                HasOneEdge realNeighbor = (HasOneEdge) neighbor;
                return new EndVertexOfTwo(this, realNeighbor.GetEdgeIndex(this).edgeIndex);
            } catch (ClassCastException e2) {
                throw (new ArithmeticException("NO EDGES!!" + "\nneighbor:\n" + neighbor));
            }
		} catch (algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.TwoOppositeEdgeIndexesException e1) {
            try {
				algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.YVertex realNeighbor =
						(algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.YVertex) neighbor;
                return new EndVertexOfY(this, realNeighbor.GetEdgeIndex3(), realNeighbor.GetEdgeIndex1(), realNeighbor
                        .GetEdgeIndex2());
            } catch (ClassCastException e2) {
				algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.TVertex realNeighbor =
						(algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.TVertex) neighbor;
                return new EndVertexOfTwo(this, realNeighbor.GetEdgeIndex(this).edgeIndex);
            }
        }
    }

    final public void MaintainNeighbors() {
        Vertex[] neighbors = GetNeighbors();
        for (Vertex neighbor : neighbors)
            neighbor.Maintain(this);
    }

    final public void ReplaceEdgeIndexesForInsertion(int oei, int nei1, int nei2) {
        ElementVertex[] elementVertices = GetElementVertices();
        for (ElementVertex elementVertice : elementVertices)
            elementVertice.ReplaceEdgeIndexesForInsertion(oei, nei1, nei2, this);
    }

    final public void ReplaceEdgeIndexesForDeletion(int oei, int nei) {
        ElementVertex[] elementVertices = GetElementVertices();
        for (ElementVertex elementVertice : elementVertices)
            elementVertice.ReplaceEdgeIndexesForDeletion(oei, nei);
    }

    final public void DecrementEdgeIndexes(int lowerIndex) {
        ElementVertex[] elementVertices = GetElementVertices();
        for (ElementVertex elementVertice : elementVertices)
            elementVertice.DecrementEdgeIndexes(lowerIndex);
    }

    final public void SetVertexIndexOfEdges(int vi) {
        GetMainElementVertex().SetVertexIndexOfEdges(vi);
    }

    final public boolean EmptySet() {
        return GetMainElementVertex().EmptySet();
    }

    final NumeratorAndDenominator GetNumeratorAndDenominatorForMSE() {
        NumeratorAndDenominator nd = GetMainElementVertex().GetNumeratorAndDenominatorForMSE();
        nd.DivEqual(n);
        return nd;
    }

    // For GetTangent() in End, Corner,Star3, Star4 and T-vertex
    // We keep the same angle as the tangent at the neighbor and the incident line segment
    // final protected Vektor GetTangentWithSameAngleAsAtNeighbor(Vertex neighbor) {
    // // for avoiding infinite recursion...
    // if (neighbor instanceof PrincipalCurve.Vertex.EndVertex ||
    // neighbor instanceof PrincipalCurve.Vertex.CornerVertex ||
    // neighbor instanceof PrincipalCurve.Vertex.StarOfThreeVertex ||
    // neighbor instanceof PrincipalCurve.Vertex.StarOfFourVertex ||
    // (neighbor instanceof PrincipalCurve.Vertex.TVertex && this.equals(neighbor.GetNeighbors()[2]))) {
    // Vektor vektor = neighbor.Sub(this);
    // vektor.DivEqual(vektor.Norm2());
    // return vektor;
    // }
    // Vektor neighborTangent = neighbor.GetTangent(this);
    // Vektor midPoint = this.Add(neighbor).Div(2);
    // Vektor difference = this.Sub(neighbor);
    // Line oldalfelezoMeroleges = LineObject.GetNormalAt(midPoint,difference,new Vektor2D(0,0));
    // Vektor vektor = neighbor.Add(neighborTangent);
    // vektor = oldalfelezoMeroleges.Reflect(vektor);
    // vektor.SubEqual(this);
    // return vektor;
    // }

    // final protected Vektor GetTangentOfCenterOfCircleAround(Vektor neighbor1,Vektor neighbor2) {
    // Vektor center = LineObject.GetCenterOfCircleAround(this,neighbor1,neighbor2);
    // if (center == null) {// parallel tangents
    // Vektor normal = neighbor1.Sub(this);
    // normal = normal.Div(normal.Norm2());
    // return normal;
    // }
    // else {
    // Line tangent = LineObject.GetNormalAt(this,center.Sub(this),new Vektor2D(0,0));
    // Vektor normal = tangent.GetDirectionalVektor(); // clockwise, length = 1
    // double angle1 = ((Vektor2D)center).AngleClockwise(this,(Vektor2D)neighbor1); // 0 <= angle < 360
    // double angle2 = ((Vektor2D)center).AngleClockwise(this,(Vektor2D)neighbor2);
    // if (angle1 > angle2) // something is wrong, it should be the opposite
    // return normal;
    // else
    // return normal.Mul(-1);
    // }
    // }

    final class VertexCluster extends SampleWithOnlineStatistics implements Weighted {
        OnlineSampleStatisticsDegreeOne statisticsDegreeOne;

        public VertexCluster(Vektor prototypeVektor) {
            super(1);
            statisticsDegreeOne = new OnlineSampleStatisticsDegreeOne(prototypeVektor);
            statistics[0] = statisticsDegreeOne;
        }

        final public Vektor GetSetPointAt(int index) {
            return sample.GetPointAt(index);
        }

        final public Vektor GetCenter() {
            return statisticsDegreeOne.GetCenter();
        }

        final public double GetSigmaSquaredTimesWeight() {
            return statisticsDegreeOne.GetSigmaSquaredTimesWeight();
        }

        final public double GetMSETimesWeight(Vektor vektor) {
            return statisticsDegreeOne.GetMSETimesWeight(vektor);
        }

        final public Vektor GetSum() {
            return statisticsDegreeOne.GetSum();
        }

        @Override
        final public double GetWeight() {
            return statisticsDegreeOne.GetWeight();
        }

        final public void DeleteMovedPoints(Object cluster) {
            boolean[] toBeDeleted = new boolean[GetSize()];
            PrincipalCurveSampleVektor samplePoint;
            for (int i = 0; i < sample.getSize(); i++) {
                samplePoint = (PrincipalCurveSampleVektor) sample.GetPointAt(i);
                if (samplePoint.cluster != cluster) {
                    statisticsDegreeOne.DeletePoint(samplePoint);
                    toBeDeleted[i] = true;
                } else
                    toBeDeleted[i] = false;
            }
            sample.DeletePoints(toBeDeleted);
        }

    }

}
