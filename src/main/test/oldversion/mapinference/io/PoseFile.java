package edu.uq.dke.mapupdate.oldversion.mapinference.io;

/**
 * Frechet-based map construction 2.0 Copyright 2013 Mahmuda Ahmed and Carola Wenk
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * ------------------------------------------------------------------------
 * <p>
 * This software is based on the following article. Please cite this article when using this code
 * as part of a research publication:
 * <p>
 * Mahmuda Ahmed and Carola Wenk, "Constructing Street Networks from GPS Trajectories", European
 * Symposium on Algorithms (ESA): 60-71, Ljubljana, Slovenia, 2012
 * <p>
 * ------------------------------------------------------------------------
 * <p>
 * Author: Mahmuda Ahmed Filename: AhmedTraceMerge2012.java
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * An object that represents a track.
 */
public class PoseFile {
    String fileName;
    ArrayList<Vertex> curve;

    PoseFile() {
        this.fileName = "";
        this.curve = new ArrayList<Vertex>();
    }

    PoseFile(String curveName, ArrayList<Vertex> curve) {
        this.fileName = curveName;
        this.curve = curve;
    }

    public String getFileName() {
        return fileName;
    }

    public ArrayList<Vertex> getPose() {
        return curve;
    }

    public double getLength() {
        double length = 0;
        for (int i = 1; i < curve.size(); i++) {
            length = length + curve.get(i - 1).dist(curve.get(i));
        }
        return length;
    }

    public static PoseFile readFile(File inputFile, boolean hasAltitude) {
        PoseFile poseFile = new PoseFile();
        poseFile.fileName = inputFile.getName();
        String str = "";

        try {
            BufferedReader in = new BufferedReader(new FileReader(
                    inputFile.getAbsolutePath()));
            double prev_time = 0;
            double x, y, z;
            while ((str = in.readLine()) != null) {
                StringTokenizer strToken = new StringTokenizer(str);
                // strToken.nextToken();
                // track file in "x y timestamp" or "x y z timestamp" format

                x = Double.parseDouble(strToken.nextToken());
                y = Double.parseDouble(strToken.nextToken());
                if (hasAltitude) {
                    z = Double.parseDouble(strToken.nextToken());
                } else {
                    z = 0.0;
                }
                double timestamp = Double.parseDouble(strToken.nextToken());
                Vertex newPoint = new Vertex(x, y, z, timestamp);
                if (poseFile.curve.size() > 0) {
                    double dist = newPoint.dist(poseFile.curve
                            .get(poseFile.curve.size() - 1));
                    if (timestamp - prev_time > 120)
                        break;
                    if (dist > 2.0)
                        poseFile.curve.add(newPoint);
                } else {
                    poseFile.curve.add(newPoint);
                }
                prev_time = timestamp;

            }
            in.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return poseFile;
    }
}

