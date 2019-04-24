package algorithm.mapinference.trajectoryclustering.pcurves.AWT;

import java.awt.*;

final public class ColorChoice extends Choice {
    public final static int NUM_OF_COLORS = 13;
    public final static Color[] colors;
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final static String[] colorStrings = {"Black", "Blue", "Cyan", "Dark gray", "Gray", "Green", "Light gray",
            "Magenta", "Orange", "Pink", "Red", "White", "Yellow"};

    static {
        colors = new Color[13];
        int i1 = 0, i2 = 63, i3 = 127; // bug in LiNuX compiler
        colors[0] = new Color(i1, i1, i1);
        colors[1] = new Color(0, 0, 255);
        colors[2] = new Color(0, 255, 255);
        colors[3] = new Color(i2, i2, i2);
        colors[4] = new Color(i3, i3, i3);
        colors[5] = new Color(0, 255, 0);
        colors[6] = new Color(191, 191, 191);
        colors[7] = new Color(255, 0, 255);
        colors[8] = new Color(255, 204, 0);
        colors[9] = new Color(255, 179, 179);
        colors[10] = new Color(255, 0, 0);
        colors[11] = new Color(255, 255, 255);
        colors[12] = new Color(255, 255, 0);
    }

    public ColorChoice(String initialColorString) {
        super();
        for (int i = 0; i < NUM_OF_COLORS; i++)
            addItem(colorStrings[i]);
        for (int i = 0; i < NUM_OF_COLORS; i++)
            if (initialColorString.equals(colorStrings[i]))
                select(i);
    }

    public ColorChoice(Color initialColor) {
        super();
        for (int i = 0; i < NUM_OF_COLORS; i++)
            addItem(colorStrings[i]);
        for (int i = 0; i < NUM_OF_COLORS; i++)
            if (initialColor.equals(colors[i]))
                select(i);
    }

    public static Color GetColor(String colorString) {
        for (int i = 0; i < NUM_OF_COLORS; i++)
            if (colorString.equals(colorStrings[i]))
                return colors[i];
        return colors[11]; // white
    }

    public static void main(String args[]) {
        for (int i = 0; i < NUM_OF_COLORS; i++) {
            System.out.println(colorStrings[i] + ":\t    " + colors[i].getRed() + "    \t" + colors[i].getGreen()
                    + "    \t" + colors[i].getBlue());
        }
    }

    final public Color getSelectedColor() {
        return colors[getSelectedIndex()];
    }
}
