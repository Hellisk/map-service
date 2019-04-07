package mapupdate.mapinference.trajectoryclustering.pcurves.PostScript;

import mapupdate.mapinference.trajectoryclustering.pcurves.Utilities.MyMath;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

public class PostScriptDocument {
    public final static int SQUARED_LINECAP = 0;
    public final static int ROUND_LINECAP = 1;
    public final static int PROJECTED_LINECAP = 2;
    public final static int ANGLE_LINEJOIN = 0;
    public final static int ROUND_LINEJOIN = 1;
    public final static int TRIANGLE_LINEJOIN = 2;
    protected static double boundingBoxX1 = 100;
    protected static double boundingBoxX2 = 500;
    protected static double boundingBoxY1 = 100;
    protected static double boundingBoxY2 = 500;
    protected static int numOfPages = 1;
    protected FileOutputStream fStream;
    protected PrintStream pStream;

    @SuppressWarnings("deprecation")
    public PostScriptDocument(Frame frame) {
        FileDialog fd = new FileDialog(frame, "Save to PostScript", FileDialog.SAVE);
        fd.show();
        if (fd.getFile() != null) {
            PostScriptSetupDialog pd = new PostScriptSetupDialog(frame);
            pd.show();
            Constructor(fd.getDirectory(), fd.getFile());
        }
    }

    @SuppressWarnings("deprecation")
    public PostScriptDocument(Frame frame, Panel panel) {
        FileDialog fd = new FileDialog(frame, "Save to PostScript", FileDialog.SAVE);
        fd.show();
        if (fd.getFile() != null) {
            PostScriptSetupDialog pd = new PostScriptSetupDialog(frame, panel);
            pd.show();
            Constructor(fd.getDirectory(), fd.getFile());
        }
    }

    public PostScriptDocument(String path, String fileName, double bbx1, double bby1, double bbx2, double bby2,
                              int in_numOfPages) {
        boundingBoxX1 = bbx1;
        boundingBoxX2 = bbx2;
        boundingBoxY1 = bby1;
        boundingBoxY2 = bby2;
        numOfPages = in_numOfPages;
        Constructor(path, fileName);
    }

    public PostScriptDocument(String path, String fileName, double bbx1, double bby1, double bbx2, double bby2) {
        boundingBoxX1 = bbx1;
        boundingBoxX2 = bbx2;
        boundingBoxY1 = bby1;
        boundingBoxY2 = bby2;
        numOfPages = 1;
        ConstructorShort(path, fileName);
    }

    public static double ConvertRGBColor(int c) {
        return MyMath.TruncateDouble((c + 1) / 256.0, 2);
    }

    public static void main(String args[]) {
    }

    private void Constructor(String path, String fileName) {
        try {
            fStream = new FileOutputStream(path + fileName);
            pStream = new PrintStream(fStream);
            pStream.println("%!PS-Adobe-2.0");
            pStream.println("%%Title: " + fileName);
            pStream.println("%%Creator: PostScriptDocument.java");
            Date date = new Date();
            pStream.println("%%CreationDate: " + date);
            pStream.println("%%BoundingBox: " + boundingBoxX1 + " " + boundingBoxY1 + " " + boundingBoxX2 + " "
                    + boundingBoxY2);
            pStream.println("%%Pages: " + numOfPages);
            pStream.println("%%EndComments");
            pStream.println();

            NewPath();
            MoveTo(boundingBoxX1, boundingBoxY1);
            LineTo(boundingBoxX2, boundingBoxY1);
            LineTo(boundingBoxX2, boundingBoxY2);
            LineTo(boundingBoxX1, boundingBoxY2);
            ClosePath();
            Clip();
        } catch (IOException e) {
            System.out.println("Can't open file " + path + fileName);
        }
    }

    // PostScript operators

    private void ConstructorShort(String path, String fileName) {
        try {
            fStream = new FileOutputStream(path + fileName);
            pStream = new PrintStream(fStream);
            pStream.println("%!PS-Adobe-2.0");
            pStream.println("%%BoundingBox: " + boundingBoxX1 + " " + boundingBoxY1 + " " + boundingBoxX2 + " "
                    + boundingBoxY2);
            pStream.println();
        } catch (IOException e) {
            System.out.println("Can't open file " + path + fileName);
        }
    }

    final public void End() {
        try {
            pStream.close();
            fStream.close();
        } catch (IOException e) {
            System.out.println("Can't close file");
        }
    }

    final public void Arc(double x, double y, double r, double angleStart, double angleEnd) {
        pStream.println(x + " " + y + " " + r + " " + angleStart + " " + angleEnd + " arc");
    }

    final public void Arc(double r, double angleStart, double angleEnd) {
        pStream.println(r + " " + angleStart + " " + angleEnd + " arc");
    }

    final public void Clip() {
        pStream.println("clip");
    }

    final public void ClosePath() {
        pStream.println("closepath");
    }

    final public void Copy(int i) {
        pStream.println(i + " copy");
    }

    final public void Fill() {
        pStream.println("fill");
    }

    final public void FindFont(String font) {
        pStream.println("/" + font + " findfont");
    }

    final public void GSave() {
        pStream.println("gsave");
    }

    final public void GRestore() {
        pStream.println("grestore");
    }

    final public void LineTo() {
        pStream.println("lineto");
    }

    final public void LineTo(double x, double y) {
        pStream.println(x + " " + y + " lineto");
    }

    final public void MoveTo(double x, double y) {
        pStream.println(x + " " + y + " moveto");
    }

    final public void MoveTo() {
        pStream.println("moveto");
    }

    final public void CurveTo() {
        pStream.println("curveto");
    }

    final public void NewPath() {
        pStream.println("newpath");
    }

    final public void Pop() {
        pStream.println("pop");
    }

    final public void RLineTo(double x, double y) {
        pStream.println(x + " " + y + " rlineto");
    }

    final public void RMoveTo(double x, double y) {
        pStream.println(x + " " + y + " rmoveto");
    }

    final public void ScaleFont(double x) {
        pStream.println(x + " scalefont");
    }

    final public void SetDash(double[] pattern, double offset) {
        pStream.print("[");
        for (double aPattern : pattern)
            pStream.print(aPattern + " ");
        pStream.println("] " + offset + " setdash");
    }

    final public void SetFont() {
        pStream.println("setfont");
    }

    final public void SetLineCap(int lineCap) {
        pStream.println(lineCap + " setlinecap");
    }

    final public void SetLineJoin(int lineJoin) {
        pStream.println(lineJoin + " setlinejoin");
    }

    final public void SetLineWidth(double x) {
        pStream.println(x + " setlinewidth");
    }

    final public void SetGray() {
        pStream.println(" setgray");
    }

    final public void SetGray(double x) {
        pStream.println(x + " setgray");
    }

    final public void SetRGBColor() {
        pStream.println("setrgbcolor");
    }

    final public void SetRGBColor(double r, double g, double b) {
        pStream.println(r + " " + g + " " + b + " setrgbcolor");
    }

    final public void Show(String s) {
        pStream.println("(" + s + ") show");
    }

    // Additional operators

    final public void ShowPage() {
        pStream.println("showpage");
    }

    final public void Stroke() {
        pStream.println("stroke");
    }

    final public void BeginFor(int init, int step, int last) {
        pStream.println(init + " " + step + " " + (last - 1) + "{");
    }

    final public void EndFor() {
        pStream.println("} for");
    }

    final public void DrawCircle(double x, double y, double r) {
        NewPath();
        MoveTo(x + r, y);
        Arc(x, y, r, 0, 540);
        Stroke();
    }

    final public void DrawCircle(double r) {
        RMoveTo(r, 0);
        Arc(r, 0, 540);
    }

    final public void DrawPolygon(double[] xCoords, double[] yCoords) {
        if ((xCoords.length == 0) || (xCoords.length != yCoords.length))
            return;
        NewPath();
        MoveTo(xCoords[0], yCoords[0]);
        for (int i = 1; i < xCoords.length; i++) {
            Push(xCoords[i]);
            Push(yCoords[i]);
        }
        pStream.println();
        BeginFor(0, 1, (xCoords.length - 1));
        {
            Pop();
            LineTo();
        }
        EndFor();
        // pStream.println("1 1 " + (xCoords.length - 1) + " {pop lineto} for");
        Stroke();
    }

    final public void DrawClosedPolygon(double[] xCoords, double[] yCoords) {
        if ((xCoords.length == 0) || (xCoords.length != yCoords.length))
            return;
        NewPath();
        MoveTo(xCoords[0], yCoords[0]);
        for (int i = 1; i < xCoords.length; i++)
            pStream.print(xCoords[i] + " " + yCoords[i] + " ");
        pStream.println();
        pStream.println("1 1 " + (xCoords.length - 1) + " {pop lineto} for");
        pStream.println("closepath");
        Stroke();
    }

    final public void FillCircle(double x, double y, double r) {
        NewPath();
        MoveTo(x + r, y);
        Arc(x, y, r, 0, 540);
        Fill();
        Stroke();
    }

    final public void FillCircle(double r) {
        RMoveTo(r, 0);
        Arc(r, 0, 540);
        Fill();
    }

    final public void NewLine() {
        pStream.println();
    }

    final public void PrintText(String s, double x, double y) {
        NewPath();
        MoveTo(x, y);
        Show(s);
        Stroke();
    }

    final public void Push(double d) {
        pStream.print(d + " ");
    }

    final public void Print(String s) {
        pStream.print(s);
    }

    final public void SetRGBColor(Color color) {
        double r = ConvertRGBColor(color.getRed());
        double g = ConvertRGBColor(color.getGreen());
        double b = ConvertRGBColor(color.getBlue());
        SetRGBColor(r, g, b);
    }

    final public void SetupFont(String font, double fontSize) {
        FindFont(font);
        ScaleFont(fontSize);
        SetFont();
    }
}
