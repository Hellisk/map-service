package mapupdate.mapinference.trajectoryclustering.pcurves.LinearAlgebra;

import mapupdate.util.function.GreatCircleDistanceFunction;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

abstract public class GraphAbstract extends Sample {
    protected GraphAbstract() {
        super();
    }

    protected GraphAbstract(int size) {
        super(size);
    }

    GraphAbstract(Sample sample) {
        points = sample.points;
    }

    protected abstract int GetVektorIndex1OfEdgeAt(int index);

    protected abstract int GetVektorIndex2OfEdgeAt(int index);

    protected abstract int GetNumOfLineSegments();

    public LineSegment GetLineSegmentAt(int index) {
        return new LineSegmentObject(GetPointAt(GetVektorIndex1OfEdgeAt(index)),
                GetPointAt(GetVektorIndex2OfEdgeAt(index)));
    }

    final double Dist2Squared(Vektor vektor) {
        double dist;
        GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
        try {
            dist = vektor.Dist2Squared(GetLineSegmentAt(0));
        } catch (ArrayIndexOutOfBoundsException e) {
            return 0;
        }
        double d;
        for (int i = 1; i < GetNumOfLineSegments(); i++) {
            d = vektor.Dist2Squared(GetLineSegmentAt(i));
            if (d < dist)
                dist = d;
        }
        return dist;
    }

    final public double Dist2(Vektor vektor) {
        return Math.sqrt(Dist2Squared(vektor));
    }

    @Override
    public String toString() {
        String string = super.toString();
        string += "NumOfEdges = " + GetNumOfLineSegments() + "\n";
        for (int i = 0; i < GetNumOfLineSegments(); i++)
            string += "(" + GetVektorIndex1OfEdgeAt(i) + "," + GetVektorIndex2OfEdgeAt(i) + ")\n";
        return string;
    }

    @Override
    public void Save(String fileName) {
        if (getSize() == 0) {
            System.err.println("Can't save " + fileName + ", size = 0");
            return;
        }
        try {
            FileOutputStream fOut = new FileOutputStream(fileName);
            PrintStream pOut = new PrintStream(fOut);
            pOut.println(GetPointAt(0).Dimension());
            for (int i = 0; i < getSize(); i++) {
                GetPointAt(i).Save(pOut);
                pOut.println();
            }
            pOut.println();
            for (int i = 0; i < GetNumOfLineSegments(); i++)
                pOut.println(GetVektorIndex1OfEdgeAt(i) + " " + GetVektorIndex2OfEdgeAt(i));
            pOut.close();
            fOut.close();
        } catch (IOException e) {
            System.out.println("Can't open file " + fileName);
        }
    }

    @Override
    public String SaveToString() {
        String str = new String();
        for (int i = 0; i < getSize(); i++)
            str += GetPointAt(i).SaveToString() + "\n";
        str += "\n";
        for (int i = 0; i < GetNumOfLineSegments(); i++)
            str += GetVektorIndex1OfEdgeAt(i) + " " + GetVektorIndex2OfEdgeAt(i);
        return str;
    }
}
