package algorithm.mapinference.trajectoryclustering.pcurves.Internet;

import algorithm.mapinference.trajectoryclustering.pcurves.Utilities.Environment;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;

class UpLoadFileOffline {
    final static public char[] specialCharacters = {'\n', '\t', '\r', '\b', ' ', '&', '='};
    final static public String[] cgiStrings = {"~nl~", "~tab~", "~cr~", "~bs~", "+", "~amp~", "~eq~"};
    public long uniqueNumber; // to be attached after the filename
    String filename;
    String stringToAppend;
    int chunkSize = 1000;

    public UpLoadFileOffline(String in_filename, long in_uniquenumber) {
        Initialize(in_filename, in_uniquenumber);
    }

    // Random uniqueNumber
    public UpLoadFileOffline(String in_filename) {
        Initialize(in_filename, Math.abs(Runtime.getRuntime().freeMemory() - System.currentTimeMillis()) % 10000000);
    }

    static public synchronized String Convert(String input) {
        String converted = new String();
        int j;
        for (int i = 0; i < input.length(); i++) {
            for (j = 0; j < specialCharacters.length; j++)
                if (specialCharacters[j] == input.charAt(i))
                    break;
            if (j == specialCharacters.length)
                converted += input.charAt(i);
            else
                converted += cgiStrings[j];
        }
        return converted;
    }

    private synchronized void Initialize(String in_filename, long in_uniquenumber) {
        filename = in_filename;
        uniqueNumber = in_uniquenumber;
        stringToAppend = new String();
    }

    @SuppressWarnings("deprecation")
    final protected synchronized void Open() {
        try {
            URL url =
                    new URL(Environment.cgiRootURL + "/openFile.cgi?file="
                            + filename + "&uniqueNumber=" + uniqueNumber);
            DataInputStream dIn = new DataInputStream(url.openStream());
            String string = dIn.readLine();
            while (string != null) {
                string = dIn.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("MYERROR: " + e.getMessage() + " in Internet.UpLoadFile.Open()");
        }
    }

    final public void Append(String string) {
        stringToAppend = string;
        Append();
    }

    @SuppressWarnings("deprecation")
    final protected synchronized void Append() {
        String chunk;
        try {
            for (int i = 0; i < (stringToAppend.length() - 1) / chunkSize + 1; i++) {
                chunk = stringToAppend.substring(i * chunkSize, Math.min((i + 1) * chunkSize, stringToAppend.length()));
                URL url =
                        new URL(Environment.cgiRootURL + "/appendFile.cgi?file=" + filename + "&string=" + Convert(chunk) + "&uniqueNumber=" + uniqueNumber);
                DataInputStream dIn = new DataInputStream(url.openStream());
                String line = dIn.readLine();
                while (line != null) {
                    line = dIn.readLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("MYERROR: " + e + " in Internet.UpLoadFile.Append()");
        }
    }
}
