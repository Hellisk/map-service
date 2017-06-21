package traminer.util.spatial.structures.delaunay;

import traminer.util.spatial.SpatialInterface;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.objects.Edges;
import traminer.util.spatial.objects.Point;
import traminer.util.spatial.objects.Triangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements a Delaunay Triangulation, using
 * Bowyer-Watson algorithm.
 * <p>
 * Bowyer, Adrian (1981). "Computing Dirichlet tessellations".
 * <p>
 * Watson, David F. (1981). "Computing the n-dimensional Delaunay
 * tessellation with application to Voronoi polytopes".
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class DelaunayTriangulation implements SpatialInterface {
    /**
     * Compute the Delaunay triangulation for a finite set of points.
     * Return a list of Delaunay triangles.
     *
     * @require The list of points must not contain repeated points,
     * i.e. point with same X and Y coordinates.
     */
    public List<Triangle> triangulate(List<Point> pointsList) {
        return BowyerWatson(pointsList);
    }

    /**
     * The Bowyer-Watson algorithm for Delaunay triangulation.
     * Compute the Delaunay triangulation for a finite set of points
     * by inserting each point at a time in the triangles mesh.
     * Return a list of triangles.
     */
    private List<Triangle> BowyerWatson(List<Point> pointsList) {
        // initialize triangle list
        List<Triangle> triList = new ArrayList<Triangle>();

        // create super triangle, must be large enough to
        // completely contain all the points in pointsList
        Point p1 = new Point(-INFINITY, -INFINITY);
        Point p2 = new Point(0, INFINITY);
        Point p3 = new Point(INFINITY, INFINITY);
        Triangle superTriangle = new Triangle(p1.x(), p1.y(),
                p2.x(), p2.y(), p3.x(), p3.y());

        // add super-triangle to triangulation
        triList.add(superTriangle);

        // add all the points one at a time to the triangulation
        for (Point p : pointsList) {
            List<Triangle> badTriangles = new ArrayList<Triangle>();
            for (Triangle tri : triList) {
                // check if the point is inside circumcircle of the triangle
                double radius = tri.circumradius();
                Point circ_center = tri.circumcenter();
                double dist = p.distance(circ_center,
                        new EuclideanDistanceFunction());
                // find all the triangles that are no longer
                // valid due to the point insertion
                if ((dist + MIN_DIST) < radius) {
                    badTriangles.add(tri);
                }
            }
            // find the boundary of the polygonal hole
            List<Edges> edgeList = new ArrayList<Edges>();
            for (int i = 0; i < badTriangles.size(); i++) {
                Triangle bad_i = badTriangles.get(i);
                for (Edges edge : bad_i.getEdges()) {
                    // if edge is not shared by any other triangles in badTriangles
                    boolean shared = false;
                    for (int j = 0; j < badTriangles.size(); j++) {
                        Triangle bad_j = badTriangles.get(j);
                        if (j != i && bad_j.hasEdge(edge)) {
                            shared = true;
                            break;
                        }
                    }
                    if (!shared) {
                        edgeList.add(edge);
                    }
                }
            }
            // remove bad triangles from triangulation
            for (Triangle tri : badTriangles) {
                triList.remove(tri);
            }
            // re-triangulate the polygonal hole.
            // for each edge, form a triangle from edge to point
            for (Edges edge : edgeList) {
                Triangle newTri = new Triangle(p.x(), p.y(),
                        edge.x1(), edge.y1(), edge.x2(), edge.y2());
                triList.add(newTri);
            }
        }
        // done inserting points, now clean up
        List<Triangle> triangulation = new ArrayList<Triangle>();
        for (Triangle tri : triList) {
            if (!tri.hasVertex(p1) && !tri.hasVertex(p2) && !tri.hasVertex(p3)) {
                triangulation.add(tri);
            }
        }

        return triangulation;
    }
}
