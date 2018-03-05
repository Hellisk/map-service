package edu.uq.dke.mapupdate.io;

import org.jdom2.JDOMException;
import traminer.util.map.roadnetwork.RoadNetworkGraph;

import java.io.*;
import java.text.DecimalFormat;

/**
 * Created by uqpchao on 5/07/2017.
 */
public class RawFileOperation {
    private int totalRecordCount = 20000;

    public RawFileOperation(int count) {
        this.totalRecordCount = count;
    }

    /**
     * read raw trajectories and filter them with a given size map, all trajectories that are completely inside the map bounds are outputted
     *
     * @param groundTruthMap                     input path for given map
     * @param rawTrajectories                    input path for raw trajectorie
     * @param initialTrajectories                folder for output trajectories
     * @param outputGroundTruthMatchResultFolder folder for all corresponding ground truth trajectory match result
     * @throws IOException   IO exception
     * @throws JDOMException unchecked exception
     */
    public void RawTrajectoryParser(String groundTruthMap, String rawTrajectories, String initialTrajectories, String outputGroundTruthMatchResultFolder) throws IOException {
        CSVMapReader map = new CSVMapReader(groundTruthMap);
        RoadNetworkGraph roadGraph = map.readMap(0);
        BufferedReader brTrajectory = new BufferedReader(new FileReader(rawTrajectories + "beijingTrajectory"));
//        Set<String> segmentLookup = new HashSet<>();

//        // maintain a road way list of given map area for filtering the match result
//        for (RoadWay r : roadGraph.getWays()) {
//            segmentLookup.add(r.getId());
//        }

        // create folders for further writing
        File createFolder = new File(initialTrajectories);
        cleanPath(createFolder);
        createFolder = new File(outputGroundTruthMatchResultFolder);
        cleanPath(createFolder);
        DecimalFormat df = new DecimalFormat(".00000");
        String line;
        int tripID = 0;
        while ((line = brTrajectory.readLine()) != null && tripID < totalRecordCount) {
            String[] trajectoryInfo = line.split(",");
            String[] rawTrajectory = trajectoryInfo[28].split("\\|");
            BufferedWriter bwRawTrajectory = new BufferedWriter(new FileWriter(initialTrajectories + "trip_" + tripID + ".txt"));
            boolean isInsideTrajectory = true;
            double firstLon = Double.parseDouble(rawTrajectory[0].split(":")[0]) / 100000;
            double firstLat = Double.parseDouble(rawTrajectory[0].split(":")[1]) / 100000;
            if (isInside(firstLon, firstLat, roadGraph)) {
                long firstTime = Long.parseLong(rawTrajectory[0].split((":"))[3]);
                bwRawTrajectory.write(firstLon + " " + firstLat + " " + firstTime + "\n");
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
            } else {
                isInsideTrajectory = false;
            }
            bwRawTrajectory.close();

            if (isInsideTrajectory) {
                BufferedWriter bwMatchedTrajectory = new BufferedWriter(new FileWriter(outputGroundTruthMatchResultFolder + "realtrip_" + tripID + ".txt"));
                String[] matchLines = trajectoryInfo[4].split("\\|");

                for (String l : matchLines)
                    bwMatchedTrajectory.write(l + "\n");

                bwMatchedTrajectory.close();
                tripID++;

//                boolean isInsideMatchedTrajectory = true;
//                for (String l : matchLines) {
//                    String[] matchPointInfo = l.split(":");
//                    if (!segmentLookup.contains(matchPointInfo[0])) {
//                        isInsideMatchedTrajectory = false;
//                        break;
//                    } else {
//                        bwMatchedTrajectory.write(matchPointInfo[3] + " " + matchPointInfo[4] + " " + matchPointInfo[5] + " " + matchPointInfo[0] + "\n");
//                    }
//                }
//                bwMatchedTrajectory.close();
//                if (isInsideMatchedTrajectory) {
//                    tripID++;
//                } else {
//                    File currTrajFile = new File(initialTrajectories + "trip_" + tripID + ".txt");
//                    File currMatchedTrajFile = new File(outputGroundTruthMatchResultFolder + "realtrip_" + tripID + ".txt");
//                    currTrajFile.delete();
//                    currMatchedTrajFile.delete();
//                }

            } else {
                File currTrajFile = new File(initialTrajectories + "trip_" + tripID + ".txt");
                currTrajFile.delete();
            }
        }
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

    private boolean isInside(double pointX, double pointY, RoadNetworkGraph roadGraph) {
        boolean inside = false;
        if (pointX > roadGraph.getMinLon() && pointX < roadGraph.getMaxLon())
            if (pointY > roadGraph.getMinLat() && pointY < roadGraph.getMaxLat())
                inside = true;
        return inside;
    }
}