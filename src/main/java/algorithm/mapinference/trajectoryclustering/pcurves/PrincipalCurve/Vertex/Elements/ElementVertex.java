package algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.Elements;

import algorithm.mapinference.trajectoryclustering.pcurves.PrincipalCurve.PrincipalCurveClass;

// if you compile first time, comment out //*, and uncomment //**
// once PrincipalCurve.PrincipalCurveClass exists, restore the file
abstract public class ElementVertex {
    public static PrincipalCurveClass principalCurve;// *

    static protected int GetEdgeVertexIndex1At(int index) {
        return principalCurve.GetEdgeAt(index).GetVertexIndex1();// *
        // ** return 0;
    }

    static protected int GetEdgeVertexIndex2At(int index) {
        return principalCurve.GetEdgeAt(index).GetVertexIndex2();// *
        // ** return 0;
    }

    static protected Edge GetEdgeAt(int index) {
        return principalCurve.GetEdgeAt(index);// *
        // ** return null;
    }

    abstract public void ReplaceEdgeIndexesForInsertion(int oei, int nei1, int nei2, Vertex vertex);

    abstract public void ReplaceEdgeIndexesForDeletion(int oei, int nei);

    abstract public void DecrementEdgeIndexes(int lowerIndex);

    abstract public void SetVertexIndexOfEdges(int vi);

    abstract public boolean EmptySet();

    abstract public NumeratorAndDenominator GetNumeratorAndDenominatorForMSE();
}
