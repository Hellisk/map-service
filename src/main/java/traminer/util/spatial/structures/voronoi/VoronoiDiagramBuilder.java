/*
 * The author of this algorithm is Steven Fortune.  
 * Copyright (c) 1994 by AT&T Bell Laboratories.
 * 
 * Permission to use, copy, modify, and distribute this software for any
 * purpose without fee is hereby granted, provided that this entire notice
 * is included in all copies of any software which is or includes a copy
 * or modification of this software and in all copies of the supporting
 * documentation for such software.
 */
package traminer.util.spatial.structures.voronoi;

import traminer.util.spatial.SpatialInterface;
import traminer.util.spatial.objects.Point;

import java.util.*;

/**
 * Build a Voronoi diagram from a set of pivot points (sites).
 * Implements Fortune's algorithm.
 * 
 * @author Zhenyu Pan, uqdalves
 */
@SuppressWarnings("serial")
public class VoronoiDiagramBuilder implements SpatialInterface {
    /**
     * Auxiliary edge.
     */
    private class HalfEdge {
        HalfEdge ELleft, ELright;
        VoronoiSegment ELedge;
        boolean deleted;
        int ELpm;
        Point vertex;
        double ystar;
        HalfEdge PQnext;

        public HalfEdge() {
            PQnext = null;
        }
    }

    /**
     * Auxiliary line segment.
     */
    private class VoronoiSegment {
        public double a = 0, b = 0, c = 0;
        Point[] ep;  // JH: End points
        Point[] reg; // JH: Sites this segment bisects

        VoronoiSegment() {
            ep = new Point[2];
            reg = new Point[2];
        }
    }

    private double borderMinX, borderMaxX, borderMinY, borderMaxY;
    private int siteidx;
    private double xmin, xmax, ymin, ymax, deltax, deltay;
    private int nvertices;
    private int nsites;
    private Point[] sites;
    private Point bottomsite;
    private int sqrt_nsites;
    private double minDistanceBetweenSites;
    private int PQcount;
    private int PQmin;
    private int PQhashsize;
    private HalfEdge PQhash[];    
    private final static int LE = 0;
    private final static int RE = 1;

    private int ELhashsize;
    private HalfEdge ELhash[];
    private HalfEdge ELleftend, ELrightend;
    private List<VoronoiEdge> allEdges;

    /**
     * Starts a new Voronoi diagram builder.
     */
    public VoronoiDiagramBuilder() {
        siteidx = 0;
        sites = null;
        allEdges = null;
        this.minDistanceBetweenSites = MIN_DIST;
    }

    /**
     * Generate the Voronoi Polygons for the given pivot points (sites), 
     * each polygon is composed by its pivot, edges and a list of adjacent polygons.
     * <br> Uses Fortune's algorithm.
     * 
     * @param pointsList The list of generator pivots.
     * @return The list of Voronoi polygons in the diagram.
     */
    public List<VoronoiPolygon> buildVoronoiPolygons(List<? extends Point> pointsList) {
        // initially creates an empty polygon for each point in the list
        List<VoronoiPolygon> polyList =
                new ArrayList<VoronoiPolygon>(pointsList.size());
        int id = 0;
        for (Point pivot : pointsList) {
            VoronoiPolygon vp =
                    new VoronoiPolygon(pivot, "" + (id++));
            polyList.add(vp);
        }

        // generate the polygons edges
        List<VoronoiEdge> edges =
                buildVoronoiEdges(pointsList);

        // assign each edge to its respective polygons
        // and build adjacent list
        for (VoronoiEdge ve : edges) {
            int poly1 = Integer.parseInt(ve.getLeftSite());
            int poly2 = Integer.parseInt(ve.getRightSite());

            polyList.get(poly1).addEdge(ve);
            polyList.get(poly2).addEdge(ve);

            // add each polygon to each other list of
            // neighbor polygons
            polyList.get(poly1).addAdjacent("" + poly2);
            polyList.get(poly2).addAdjacent("" + poly1);
        }

        return polyList;
    }

    /**
     * Generate the edges of a Voronoi Polygon for the given pivot points (sites)
     * <br> Uses Fortune's algorithm.
     * 
     * @param pointsList The list of generator pivots.
     * @return The list of edges in the Voronoi diagram.
     */
    public List<VoronoiEdge> buildVoronoiEdges(List<? extends Point> pointsList) {
        int n = pointsList.size();
        double[] xValuesIn = new double[n];
        double[] yValuesIn = new double[n];
        for (int i = 0; i < n; i++) {
            Point p = pointsList.get(i);
            xValuesIn[i] = p.x();
            yValuesIn[i] = p.y();
        }

        return buildVoronoi(xValuesIn, yValuesIn,
                -INFINITY, INFINITY, -INFINITY, INFINITY);
    }

    /**
     * Generate the edges of a Voronoi Polygon for the given points (sites).
     * Implements the Fortune's algorithm.
     * 
     * @param xValuesIn Array of X values for each site.
     * @param yValuesIn Array of Y values for each site. Must be identical length to yValuesIn
     * @param minX The minimum X of the bounding box around the voronoi
     * @param maxX The maximum X of the bounding box around the voronoi
     * @param minY The minimum Y of the bounding box around the voronoi
     * @param maxY The maximum Y of the bounding box around the voronoi
     * 
     * @return The list of edges in the Voronoi diagram.
     */
    private List<VoronoiEdge> buildVoronoi(
            double[] xValuesIn, double[] yValuesIn,
            double minX, double maxX, double minY, double maxY) {
        sort(xValuesIn, yValuesIn, xValuesIn.length);

        // Check bounding box inputs - if mins are bigger than maxes, swap them
        double temp = 0;
        if (minX > maxX) {
            temp = minX;
            minX = maxX;
            maxX = temp;
        }
        if (minY > maxY) {
            temp = minY;
            minY = maxY;
            maxY = temp;
        }
        borderMinX = minX;
        borderMinY = minY;
        borderMaxX = maxX;
        borderMaxY = maxY;

        siteidx = 0;
        voronoi_bd();

        return allEdges;
    }

    private void sort(double[] xValuesIn, double[] yValuesIn, int count) {
        sites = null;
        allEdges = new LinkedList<VoronoiEdge>();

        nsites = count;
        nvertices = 0;

        double sn = (double) nsites + 4;
        sqrt_nsites = (int) Math.sqrt(sn);

        // Copy the inputs so we don't modify the originals
        double[] xValues = new double[count];
        double[] yValues = new double[count];
        for (int i = 0; i < count; i++) {
            xValues[i] = xValuesIn[i];
            yValues[i] = yValuesIn[i];
        }
        sortNode(xValues, yValues, count);
    }

    private void qsort(Point[] sites) {
        List<Point> listSites = new ArrayList<Point>(sites.length);
        listSites.addAll(Arrays.asList(sites));

        listSites.sort(new Comparator<Point>() {
            public final int compare(Point p1, Point p2) {
                if (p1.y() < p2.y()) {
                    return (-1);
                }
                if (p1.y() > p2.y()) {
                    return (1);
                }
                if (p1.x() < p2.x()) {
                    return (-1);
                }
                if (p1.x() > p2.x()) {
                    return (1);
                }
                return (0);
            }
        });

        // Copy back into the array
        for (int i = 0; i < sites.length; i++) {
            sites[i] = listSites.get(i);
        }
    }

    private void sortNode(double xValues[], double yValues[], int numPoints) {
        int i;
        nsites = numPoints;
        sites = new Point[nsites];
        xmin = xValues[0];
        ymin = yValues[0];
        xmax = xValues[0];
        ymax = yValues[0];
        for (i = 0; i < nsites; i++) {
            sites[i] = new Point(xValues[i], yValues[i]);
            sites[i].setId("" + i);//;getId().equals(""+i);// i;

            if (xValues[i] < xmin) {
                xmin = xValues[i];
            } else if (xValues[i] > xmax) {
                xmax = xValues[i];
            }

            if (yValues[i] < ymin) {
                ymin = yValues[i];
            } else if (yValues[i] > ymax) {
                ymax = yValues[i];
            }
        }
        qsort(sites);
        deltay = ymax - ymin;
        deltax = xmax - xmin;
    }

    /* return a single in-storage site */
    private Point nextone() {
        Point s;
        if (siteidx < nsites) {
            s = sites[siteidx];
            siteidx += 1;
            return (s);
        } else {
            return (null);
        }
    }

    private VoronoiSegment bisect(Point s1, Point s2) {
        double dx, dy, adx, ady;
        VoronoiSegment newedge;

        newedge = new VoronoiSegment();

        // store the sites that this edge is bisecting
        newedge.reg[0] = s1;
        newedge.reg[1] = s2;
        // to begin with, there are no endpoints on the bisector - it goes to
        // infinity
        newedge.ep[0] = null;
        newedge.ep[1] = null;

        // get the difference in x dist between the sites
        dx = s2.x() - s1.x();
        dy = s2.y() - s1.y();
        // make sure that the difference in positive
        adx = dx > 0 ? dx : -dx;
        ady = dy > 0 ? dy : -dy;
        newedge.c = s1.x() * dx + s1.y() * dy + (dx * dx + dy
                * dy) * 0.5;// get the slope of the line

        if (adx > ady) {
            newedge.a = 1.0f;
            newedge.b = dy / dx;
            newedge.c /= dx;// set formula of line, with x fixed to 1
        } else {
            newedge.b = 1.0f;
            newedge.a = dx / dy;
            newedge.c /= dy;// set formula of line, with y fixed to 1
        }

        return (newedge);
    }

    private void makevertex(Point v) {
        v.getId().equals("" + nvertices);
        nvertices += 1;
    }

    private boolean PQinitialize() {
        PQcount = 0;
        PQmin = 0;
        PQhashsize = 4 * sqrt_nsites;
        PQhash = new HalfEdge[PQhashsize];

        for (int i = 0; i < PQhashsize; i += 1) {
            PQhash[i] = new HalfEdge();
        }
        return true;
    }

    private int PQbucket(HalfEdge he) {
        int bucket;

        bucket = (int) ((he.ystar - ymin) / deltay * PQhashsize);
        if (bucket < 0) {
            bucket = 0;
        }
        if (bucket >= PQhashsize) {
            bucket = PQhashsize - 1;
        }
        if (bucket < PQmin) {
            PQmin = bucket;
        }
        return (bucket);
    }

    // push the HalfEdge into the ordered linked list of vertices
    private void PQinsert(HalfEdge he, Point v, double offset) {
        HalfEdge last, next;

        he.vertex = v;
        he.ystar = v.y() + offset;
        last = PQhash[PQbucket(he)];
        while ((next = last.PQnext) != null
                && (he.ystar > next.ystar ||
                (he.ystar == next.ystar && v.x() > next.vertex.x()))) {
            last = next;
        }
        he.PQnext = last.PQnext;
        last.PQnext = he;
        PQcount += 1;
    }

    // remove the HalfEdge from the list of vertices
    private void PQdelete(HalfEdge he) {
        HalfEdge last;

        if (he.vertex != null) {
            last = PQhash[PQbucket(he)];
            while (last.PQnext != he) {
                last = last.PQnext;
            }

            last.PQnext = he.PQnext;
            PQcount -= 1;
            he.vertex = null;
        }
    }

    private boolean PQempty() {
        return (PQcount == 0);
    }

    private Point PQ_min() {
        double x, y;
        while (PQhash[PQmin].PQnext == null) {
            PQmin += 1;
        }
        x = PQhash[PQmin].PQnext.vertex.x();
        y = PQhash[PQmin].PQnext.ystar;
        return new Point(x, y);
    }

    private HalfEdge PQextractmin() {
        HalfEdge curr = PQhash[PQmin].PQnext;
        PQhash[PQmin].PQnext = curr.PQnext;
        PQcount -= 1;
        return (curr);
    }

    private HalfEdge HEcreate(VoronoiSegment e, int pm) {
        HalfEdge answer = new HalfEdge();
        answer.ELedge = e;
        answer.ELpm = pm;
        answer.PQnext = null;
        answer.vertex = null;
        return (answer);
    }

    private boolean ELinitialize() {
        int i;
        ELhashsize = 2 * sqrt_nsites;
        ELhash = new HalfEdge[ELhashsize];

        for (i = 0; i < ELhashsize; i += 1) {
            ELhash[i] = null;
        }
        ELleftend = HEcreate(null, 0);
        ELrightend = HEcreate(null, 0);
        ELleftend.ELleft = null;
        ELleftend.ELright = ELrightend;
        ELrightend.ELleft = ELleftend;
        ELrightend.ELright = null;
        ELhash[0] = ELleftend;
        ELhash[ELhashsize - 1] = ELrightend;

        return true;
    }

    private HalfEdge ELright(HalfEdge he) {
        return (he.ELright);
    }

    private HalfEdge ELleft(HalfEdge he) {
        return (he.ELleft);
    }

    private Point leftreg(HalfEdge he) {
        if (he.ELedge == null) {
            return (bottomsite);
        }
        return (he.ELpm == LE ? he.ELedge.reg[LE] : he.ELedge.reg[RE]);
    }

    private void ELinsert(HalfEdge lb, HalfEdge newHe) {
        newHe.ELleft = lb;
        newHe.ELright = lb.ELright;
        (lb.ELright).ELleft = newHe;
        lb.ELright = newHe;
    }

    /**
     * This delete routine can't reclaim node, since pointers from hash table
     * may be present.
     */
    private void ELdelete(HalfEdge he) {
        (he.ELleft).ELright = he.ELright;
        (he.ELright).ELleft = he.ELleft;
        he.deleted = true;
    }

    /** 
     * Get entry from hash table, pruning any deleted nodes 
     */
    private HalfEdge ELgethash(int b) {
        HalfEdge he;

        if (b < 0 || b >= ELhashsize) {
            return (null);
        }
        he = ELhash[b];
        if (he == null || !he.deleted) {
            return (he);
        }

        /* Hash table points to deleted half edge. Patch as necessary. */
        ELhash[b] = null;
        return (null);
    }

    private HalfEdge ELleftbnd(Point p) {
        int i, bucket;
        HalfEdge he;

        /* Use hash table to get close to desired halfedge */
        // use the hash function to find the place in the hash map that this
        // HalfEdge should be
        bucket = (int) ((p.x() - xmin) / deltax * ELhashsize);

        // make sure that the bucket position in within the range of the hash
        // array
        if (bucket < 0) {
            bucket = 0;
        }
        if (bucket >= ELhashsize) {
            bucket = ELhashsize - 1;
        }

        he = ELgethash(bucket);
        if (he == null) {
            // if the HE isn't found, search backwards and forwards in the hash map
            // for the first non-null entry
            for (i = 1; i < ELhashsize; i += 1) {
                if ((he = ELgethash(bucket - i)) != null) {
                    break;
                }
                if ((he = ELgethash(bucket + i)) != null) {
                    break;
                }
            }
        }
        /* Now search linear list of halfedges for the correct one */
        if (he == ELleftend || (he != ELrightend && right_of(he, p))) {
            // keep going right on the list until either the end is reached, or
            // you find the 1st edge which the point isn't to the right of
            do {
                he = he.ELright;
            } while (he != ELrightend && right_of(he, p));
            he = he.ELleft;
        } else {
            // if the point is to the left of the HalfEdge, then search left for
            // the HE just to the left of the point
            do {
                he = he.ELleft;
            } while (he != ELleftend && !right_of(he, p));
        }

        /* Update hash table and reference counts */
        if (bucket > 0 && bucket < ELhashsize - 1) {
            ELhash[bucket] = he;
        }
        return (he);
    }

    private void pushGraphEdge(Point leftSite, Point rightSite,
                               double x1, double y1, double x2, double y2) {
        VoronoiEdge newEdge = new VoronoiEdge(x1, y1, x2, y2,
                leftSite.getId(), rightSite.getId());
        allEdges.add(newEdge);
    }

    private void clip_line(VoronoiSegment e) {
        double pxmin, pxmax, pymin, pymax;
        Point s1, s2;
        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;

        x1 = e.reg[0].x();
        x2 = e.reg[1].x();
        y1 = e.reg[0].y();
        y2 = e.reg[1].y();

        // if the distance between the two points this line was created from is
        // less than the square root of 2, then ignore it
        if (Math.sqrt(((x2 - x1) * (x2 - x1)) +
                ((y2 - y1) * (y2 - y1))) <
                minDistanceBetweenSites) {
            return;
        }
        pxmin = borderMinX;
        pxmax = borderMaxX;
        pymin = borderMinY;
        pymax = borderMaxY;

        if (e.a == 1.0 && e.b >= 0.0) {
            s1 = e.ep[1];
            s2 = e.ep[0];
        } else {
            s1 = e.ep[0];
            s2 = e.ep[1];
        }

        if (e.a == 1.0) {
            y1 = pymin;
            if (s1 != null && s1.y() > pymin) {
                y1 = s1.y();
            }
            if (y1 > pymax) {
                y1 = pymax;
            }
            x1 = e.c - e.b * y1;
            y2 = pymax;
            if (s2 != null && s2.y() < pymax) {
                y2 = s2.y();
            }

            if (y2 < pymin) {
                y2 = pymin;
            }
            x2 = (e.c) - (e.b) * y2;
            if (((x1 > pxmax) & (x2 > pxmax)) |
                    ((x1 < pxmin) & (x2 < pxmin))) {
                return;
            }
            if (x1 > pxmax) {
                x1 = pxmax;
                y1 = (e.c - x1) / e.b;
            }
            if (x1 < pxmin) {
                x1 = pxmin;
                y1 = (e.c - x1) / e.b;
            }
            if (x2 > pxmax) {
                x2 = pxmax;
                y2 = (e.c - x2) / e.b;
            }
            if (x2 < pxmin) {
                x2 = pxmin;
                y2 = (e.c - x2) / e.b;
            }
        } else {
            x1 = pxmin;
            if (s1 != null && s1.x() > pxmin) {
                x1 = s1.x();
            }
            if (x1 > pxmax) {
                x1 = pxmax;
            }
            y1 = e.c - e.a * x1;
            x2 = pxmax;
            if (s2 != null && s2.x() < pxmax) {
                x2 = s2.x();
            }
            if (x2 < pxmin) {
                x2 = pxmin;
            }
            y2 = e.c - e.a * x2;
            if (((y1 > pymax) & (y2 > pymax)) |
                    ((y1 < pymin) & (y2 < pymin))) {
                return;
            }
            if (y1 > pymax) {
                y1 = pymax;
                x1 = (e.c - y1) / e.a;
            }
            if (y1 < pymin) {
                y1 = pymin;
                x1 = (e.c - y1) / e.a;
            }
            if (y2 > pymax) {
                y2 = pymax;
                x2 = (e.c - y2) / e.a;
            }
            if (y2 < pymin) {
                y2 = pymin;
                x2 = (e.c - y2) / e.a;
            }
        }

        pushGraphEdge(e.reg[0], e.reg[1], x1, y1, x2, y2);
    }

    private void endpoint(VoronoiSegment e, int lr, Point s) {
        e.ep[lr] = s;
        if (e.ep[RE - lr] == null) {
            return;
        }
        clip_line(e);
    }

    /**
     * Returns 1 if p is to right of halfedge e 
     */
    private boolean right_of(HalfEdge el, Point p) {
        VoronoiSegment e = el.ELedge;
        Point topsite = e.reg[1];
        boolean right_of_site;
        boolean above, fast;
        double dxp, dyp, dxs, t1, t2, t3, yl;

        right_of_site = p.x() > topsite.x();
        if (right_of_site && el.ELpm == LE) {
            return (true);
        }
        if (!right_of_site && el.ELpm == RE) {
            return (false);
        }

        if (e.a == 1.0) {
            dyp = p.y() - topsite.y();
            dxp = p.x() - topsite.x();
            fast = false;
            if ((!right_of_site & (e.b < 0.0)) |
                    (right_of_site & (e.b >= 0.0))) {
                above = dyp >= e.b * dxp;
                fast = above;
            } else {
                above = p.x() + p.y() * e.b > e.c;
                if (e.b < 0.0) {
                    above = !above;
                }
                if (!above) {
                    fast = true;
                }
            }
            if (!fast) {
                dxs = topsite.x() - (e.reg[0]).x();
                above = e.b * (dxp * dxp - dyp * dyp) < dxs * dyp
                        * (1.0 + 2.0 * dxp / dxs + e.b * e.b);
                if (e.b < 0.0) {
                    above = !above;
                }
            }
        } else /* e.b==1.0 */ {
            yl = e.c - e.a * p.x();
            t1 = p.y() - yl;
            t2 = p.x() - topsite.x();
            t3 = yl - topsite.y();
            above = t1 * t1 > t2 * t2 + t3 * t3;
        }
        return ((el.ELpm == LE) == above);
    }

    private Point rightreg(HalfEdge he) {
        if (he.ELedge == null) {
            // if this halfedge has no edge, return the bottom site (whatever
            // that is)
            return (bottomsite);
        }

        // if the ELpm field is zero, return the site 0 that this edge bisects,
        // otherwise return site number 1
        return (he.ELpm == LE ? he.ELedge.reg[RE] : he.ELedge.reg[LE]);
    }

    private double dist(Point s, Point t) {
        double dx, dy;
        dx = s.x() - t.x();
        dy = s.y() - t.y();
        return Math.sqrt(dx * dx + dy * dy);
    }

    // create a new site where the HalfEdges el1 and el2 intersect - note that
    // the Point in the argument list is not used, don't know why it's there
    private Point intersect(HalfEdge el1, HalfEdge el2) {
        VoronoiSegment e1, e2, e;
        HalfEdge el;
        double d, xint, yint;
        boolean right_of_site;

        e1 = el1.ELedge;
        e2 = el2.ELedge;
        if (e1 == null || e2 == null) {
            return null;
        }

        // if the two edges bisect the same parent, return null
        if (e1.reg[1] == e2.reg[1]) {
            return null;
        }

        d = e1.a * e2.b - e1.b * e2.a;
        if (-1.0e-10 < d && d < 1.0e-10) {
            return null;
        }

        xint = (e1.c * e2.b - e2.c * e1.b) / d;
        yint = (e2.c * e1.a - e1.c * e2.a) / d;

        if ((e1.reg[1].y() < e2.reg[1].y())
                || (e1.reg[1].y() == e2.reg[1].y() &&
                e1.reg[1].x() < e2.reg[1].x())) {
            el = el1;
            e = e1;
        } else {
            el = el2;
            e = e2;
        }

        right_of_site = xint >= e.reg[1].x();
        if ((right_of_site && el.ELpm == LE)
                || (!right_of_site && el.ELpm == RE)) {
            return null;
        }

        // create a new site at the point of intersection - this is a new vector
        // event waiting to happen
        Point v = new Point(xint, yint);
        return (v);
    }

    /**
     * Implicit parameters: nsites, sqrt_nsites, xmin, xmax, ymin, ymax, deltax,
     * deltay (can all be estimates). Performance suffers if they are wrong;
     * better to make nsites, deltax, and deltay too big than too small. (?)
     */
    private boolean voronoi_bd() {
        Point newsite, bot, top, temp, p;
        Point v;
        Point newintstar = null;
        int pm;
        HalfEdge lbnd, rbnd, llbnd, rrbnd, bisector;
        VoronoiSegment e;

        PQinitialize();
        ELinitialize();

        bottomsite = nextone();
        newsite = nextone();
        while (true) {
            if (!PQempty()) {
                newintstar = PQ_min();
            }
            // if the lowest site has a smaller y value than the lowest vector
            // intersection,
            // process the site otherwise process the vector intersection

            if (newsite != null
                    && (PQempty() || newsite.y() < newintstar.y() ||
                    (newsite.y() == newintstar.y() && newsite.x() < newintstar.x()))) {
                /* new site is smallest -this is a site event */
                // get the first HalfEdge to the LEFT of the new site
                lbnd = ELleftbnd((newsite));
                // get the first HalfEdge to the RIGHT of the new site
                rbnd = ELright(lbnd);
                // if this halfedge has no edge,bot =bottom site (whatever that
                // is)
                bot = rightreg(lbnd);
                // create a new edge that bisects
                e = bisect(bot, newsite);

                // create a new HalfEdge, setting its ELpm field to 0
                bisector = HEcreate(e, LE);
                // insert this new bisector edge between the left and right
                // vectors in a linked list
                ELinsert(lbnd, bisector);

                // if the new bisector intersects with the left edge,
                // remove the left edge's vertex, and put in the new one
                if ((p = intersect(lbnd, bisector)) != null) {
                    PQdelete(lbnd);
                    PQinsert(lbnd, p, dist(p, newsite));
                }
                lbnd = bisector;
                // create a new HalfEdge, setting its ELpm field to 1
                bisector = HEcreate(e, RE);
                // insert the new HE to the right of the original bisector
                // earlier in the IF stmt
                ELinsert(lbnd, bisector);

                // if this new bisector intersects with the new HalfEdge
                if ((p = intersect(bisector, rbnd)) != null) {
                    // push the HE into the ordered linked list of vertices
                    PQinsert(bisector, p, dist(p, newsite));
                }
                newsite = nextone();
            } 
            /* intersection is smallest - this is a vector event */
            else if (!PQempty()) {
                // pop the HalfEdge with the lowest vector off the ordered list
                // of vectors
                lbnd = PQextractmin();
                // get the HalfEdge to the left of the above HE
                llbnd = ELleft(lbnd);
                // get the HalfEdge to the right of the above HE
                rbnd = ELright(lbnd);
                // get the HalfEdge to the right of the HE to the right of the
                // lowest HE
                rrbnd = ELright(rbnd);
                // get the Site to the left of the left HE which it bisects
                bot = leftreg(lbnd);
                // get the Site to the right of the right HE which it bisects
                top = rightreg(rbnd);

                v = lbnd.vertex; // get the vertex that caused this event
                makevertex(v); // set the vertex number - couldn't do this
                // earlier since we didn't know when it would be processed
                endpoint(lbnd.ELedge, lbnd.ELpm, v);
                // set the endpoint of
                // the left HalfEdge to be this vector
                endpoint(rbnd.ELedge, rbnd.ELpm, v);
                // set the endpoint of the right HalfEdge to
                // be this vector
                ELdelete(lbnd); // mark the lowest HE for
                // deletion - can't delete yet because there might be pointers
                // to it in Hash Map
                PQdelete(rbnd);
                // remove all vertex events to do with the right HE
                ELdelete(rbnd); // mark the right HE for
                // deletion - can't delete yet because there might be pointers
                // to it in Hash Map
                pm = LE; // set the pm variable to zero

                // if the site to the left of the event is higher than the
                // Site
                if (bot.y() > top.y()) {
                    // to the right of it, then swap them and
                    // set the 'pm' variable to 1
                    temp = bot;
                    bot = top;
                    top = temp;
                    pm = RE;
                }
                e = bisect(bot, top); // create an Edge (or line)
                // that is between the two Sites. This creates the formula of
                // the line, and assigns a line number to it
                bisector = HEcreate(e, pm); // create a HE from the Edge 'e',
                // and make it point to that edge
                // with its ELedge field
                ELinsert(llbnd, bisector); // insert the new bisector to the
                // right of the left HE
                endpoint(e, RE - pm, v); // set one endpoint to the new edge
                // to be the vector point 'v'.
                // If the site to the left of this bisector is higher than the
                // right Site, then this endpoint
                // is put in position 0; otherwise in pos 1

                // if left HE and the new bisector intersect, then delete
                // the left HE, and reinsert it
                if ((p = intersect(llbnd, bisector)) != null) {
                    PQdelete(llbnd);
                    PQinsert(llbnd, p, dist(p, bot));
                }

                // if right HE and the new bisector intersect, then
                // reinsert it
                if ((p = intersect(bisector, rrbnd)) != null) {
                    PQinsert(bisector, p, dist(p, bot));
                }
            } else {
                break;
            }
        }

        for (lbnd = ELright(ELleftend); lbnd != ELrightend; lbnd = ELright(lbnd)) {
            e = lbnd.ELedge;
            clip_line(e);
        }

        return true;
    }
}
