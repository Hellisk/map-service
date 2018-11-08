package mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.Elements;

// if you compile first time, comment out //*, and uncomment //**
// once PrincipalCurve.Vertex.Elements.Vertex exists, restore the file
public interface HasTwoSymmetricEdges extends HasOneEdge {
    public EdgeDirection GetOppositeEdgeIndex(Vertex vertex);// *

    public Vertex GetOppositeVertex(Vertex vertex);// *
}
