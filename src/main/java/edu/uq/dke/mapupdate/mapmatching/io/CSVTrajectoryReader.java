package edu.uq.dke.mapupdate.mapmatching.io;

import traminer.util.spatial.objects.st.STPoint;
import traminer.util.trajectory.Trajectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by uqpchao on 23/05/2017.
 */
public class CSVTrajectoryReader {
    public CSVTrajectoryReader() {
    }

    public Trajectory readTrajectory(File trajectoryFile) throws IOException {
        BufferedReader brTrajectory = new BufferedReader(new FileReader(trajectoryFile));
        Trajectory newTrajectory = new Trajectory();
        String line;
        String id;
        while ((line = brTrajectory.readLine()) != null) {
            String[] pointInfo = line.split(" ");
            STPoint newSTPoint = new STPoint(Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1]), (long) Double.parseDouble(pointInfo[2]));
            newTrajectory.add(newSTPoint);
        }
        brTrajectory.close();
        return newTrajectory;
    }

    public Stream<Trajectory> readTrajectoryFiles(String csvTrajectoryPath) throws IOException {
        File inputFile = new File(csvTrajectoryPath);
        List<Trajectory> trajectoryList = new ArrayList<>();
        if (inputFile.isDirectory()) {
            File[] trajectoryFiles = inputFile.listFiles();
            for (int i = 0; i < trajectoryFiles.length; i++) {
                trajectoryList.add(readTrajectory(trajectoryFiles[i]));
            }
        } else {
            trajectoryList.add(readTrajectory(inputFile));
        }
        Stream<Trajectory> trajectoryStream = trajectoryList.stream();
        return trajectoryStream;
    }
}
