package edu.uq.dke.mapupdate.io;

import org.jdom2.JDOMException;
import traminer.util.map.roadnetwork.RoadNetworkGraph;

import java.io.*;
import java.text.DecimalFormat;

/**
 * Created by uqpchao on 5/07/2017.
 */
public class RawFileOperation {
    private int totalRecordCount;

    public RawFileOperation() {
        this.totalRecordCount = 50000;
    }

    public RawFileOperation(int count) {
        this.totalRecordCount = count;
    }

    public void RawTrajectoryParser(String roadMapInputPath, String inputRawTrajectoryPath, String outputTrajectoryFolder, String outputGroundTruthMatchResultFolder) throws IOException, JDOMException {
        CSVMapReader map = new CSVMapReader(roadMapInputPath + "beijing_vertices.txt", roadMapInputPath + "beijing_edges.txt");
        RoadNetworkGraph roadGraph = map.readShapeCSV();
        BufferedReader brTrajectory = new BufferedReader(new FileReader(inputRawTrajectoryPath + "beijingTrajectory-" + totalRecordCount));

        // create folders for further writing
        File createFolder = new File(outputTrajectoryFolder);
        cleanPath(createFolder);
        createFolder = new File(outputGroundTruthMatchResultFolder);
        cleanPath(createFolder);
        DecimalFormat df = new DecimalFormat(".00000");
        String line;
        int tripID = 0;
        while ((line = brTrajectory.readLine()) != null) {
            String[] trajectoryInfo = line.split(",");
            String[] rawTrajectory = trajectoryInfo[28].split("\\|");
            BufferedWriter bwRawTrajectory = new BufferedWriter(new FileWriter(outputTrajectoryFolder + "trip_" + tripID + ".txt"));
            double firstLon = Double.parseDouble(rawTrajectory[0].split(":")[0]) / 100000;
            double firstLat = Double.parseDouble(rawTrajectory[0].split(":")[1]) / 100000;
//            System.out.println(rawTrajectory[0]);
            long firstTime = Long.parseLong(rawTrajectory[0].split((":"))[3]);
            bwRawTrajectory.write(firstLon + " " + firstLat + " " + firstTime + "\n");
            boolean isInsideTrajectory = true;
            for (int i = 1; i < rawTrajectory.length; i++) {
                double lon = firstLon + (Double.parseDouble(rawTrajectory[i].split(":")[0]) / 100000);
                double lat = firstLat + (Double.parseDouble(rawTrajectory[i].split(":")[1]) / 100000);
                if (isInside(lon, lat, roadGraph)) {
                    long time = firstTime + Long.parseLong(rawTrajectory[i].split(":")[3]);
                    bwRawTrajectory.write(df.format(lon) + " " + df.format(lat) + " " + time + "\n");
                } else {
                    isInsideTrajectory = false;
                    break;
                }
            }
            if (isInsideTrajectory) {
                bwRawTrajectory.close();
                BufferedWriter bwMatchedTrajectory = new BufferedWriter(new FileWriter(outputGroundTruthMatchResultFolder + "realtrip_" + tripID + ".txt"));
                String[] matchLines = trajectoryInfo[29].split("\\|");
                for (String l : matchLines) {
                    String[] matchPointInfo = l.split(":");
                    bwMatchedTrajectory.write(matchPointInfo[3] + " " + matchPointInfo[4] + " " + matchPointInfo[5] + " " + matchPointInfo[0] + "\n");
                }
                bwMatchedTrajectory.close();
                tripID++;
            } else {
                bwRawTrajectory.close();
                File currFile = new File(outputTrajectoryFolder + "trip_" + tripID + ".txt");
                currFile.delete();
            }
        }
//        System.out.println("Total columns:" + lineString.length);
    }

    private void cleanPath(File currFolder) {
        if (!currFolder.exists()) {
            currFolder.mkdirs();
        } else {
            File[] fileList = currFolder.listFiles();
            if (fileList != null) {
                for (File f : fileList) {
                    f.delete();
                }
            }
        }
    }

    public void trajectoryFilter(String inputFilePath, int recordCount) throws IOException {
        this.totalRecordCount = recordCount;
        BufferedReader brTrajectory = new BufferedReader(new FileReader(inputFilePath));
        BufferedWriter bwTrajectory = new BufferedWriter(new FileWriter(inputFilePath + "-" + recordCount));
        for (int i = 0; i < recordCount; i++) {
            String line = brTrajectory.readLine();
            bwTrajectory.write(line + "\n");
        }
        brTrajectory.close();
        bwTrajectory.close();
    }

    private boolean isInside(double pointX, double pointY, RoadNetworkGraph roadGraph) {
        boolean inside = false;
        if (pointX > roadGraph.getMinLon() && pointX < roadGraph.getMaxLon())
            if (pointY > roadGraph.getMinLat() && pointY < roadGraph.getMaxLat())
                inside = true;
        return inside;
    }
}
