package algorithm.mapinference.trajectoryclustering.pcurves.Internet;

import algorithm.mapinference.trajectoryclustering.pcurves.Utilities.Environment;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;

final class EmailFile {
    @SuppressWarnings("deprecation")
    public EmailFile(String email, String subject, String filename) {
        try {
            URL url =
                    new URL(Environment.cgiRootURL + "/mailFile.cgi?email=" + email + "&subject=" + subject + "&file=" + filename);
            DataInputStream dIn = new DataInputStream(url.openStream());
            String string = dIn.readLine();
            while (string != null) {
                string = dIn.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("MYERROR: " + e.getMessage() + " in Internet.EmailString.init()");
        }
    }
    // public static void main(String args[]) {
    // EmailString email= new EmailString("kegl@iro.umontreal.ca","trySubject","tryBody\ntryBody2");
    // }
}
