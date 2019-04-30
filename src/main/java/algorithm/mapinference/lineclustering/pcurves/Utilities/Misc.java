package algorithm.mapinference.lineclustering.pcurves.Utilities;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.StringTokenizer;

public final class Misc {
	// public static void swap(Object o1,Object o2) {
	// Object temp = o1;
	// o1 = o2;
	// o2 = temp;
	// }
	public static String MouseButton(int modifier) {
		if (modifier == 0)
			return "Left";
		else if (modifier == 4)
			return "Right";
		else if (modifier == 8)
			return "Middle";
		else
			return "";
	}
	
	public static String FormatString(double d, int width, int whiteSpaces) {
		String sOut = new String();
		sOut += d;
		if (sOut.length() > width - whiteSpaces)
			sOut = sOut.substring(0, width - whiteSpaces);
		for (int l = sOut.length(); l < width; l++)
			sOut += " ";
		return sOut;
	}
	
	public static int ReadInt(String filename) throws IOException {
		File f = new File(filename);
		DataInputStream din = new DataInputStream(new FileInputStream(f));
		@SuppressWarnings("deprecation")
		String line = new String(din.readLine());
		StringTokenizer t = new StringTokenizer(line);
		int integer = new Integer(t.nextToken());
		din.close();
		return integer;
	}
	
}
