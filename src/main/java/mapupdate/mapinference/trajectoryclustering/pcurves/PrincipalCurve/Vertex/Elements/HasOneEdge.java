package mapupdate.mapinference.trajectoryclustering.pcurves.PrincipalCurve.Vertex.Elements;

// if you compile first time, comment out //*, and uncomment //**
// once PrincipalCurve.Vertex.Elements.Vertex exists, restore the file
public interface HasOneEdge {
    public EdgeDirection GetEdgeIndex(Vertex vertex);// *

    public Vertex GetVertex();// *
}
