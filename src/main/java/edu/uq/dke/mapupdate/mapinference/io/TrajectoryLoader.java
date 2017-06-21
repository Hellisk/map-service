package edu.uq.dke.mapupdate.mapinference.io;

import com.sun.istack.internal.Nullable;

import java.io.*;
import java.util.*;

/**
 * Created by Hellisk on 4/9/2017.
 */
public class TrajectoryLoader {

    //GPS point information
    public class GPSPoint {
        private String ID;
        private double latitude;
        private double longitude;
        private double origLatitude;
        private double origLongitude;
        private Date time;
        private String prevPointID;
        private String nextPointID;
        private GPSPoint prevPoint;
        private GPSPoint nextPoint;

        public GPSPoint(String ID, String latitude, String longitude, String time) {
            this.ID = ID;
            this.latitude = Double.parseDouble(latitude);
            this.longitude = Double.parseDouble(longitude);
            this.origLatitude = Double.parseDouble(latitude);
            this.origLongitude = Double.parseDouble(longitude);

            long milliSeconds = (long) Double.parseDouble(time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(milliSeconds);
            this.time = calendar.getTime();
            this.prevPoint = null;
            this.nextPoint = null;
        }

        @Nullable
        public GPSPoint getPointByID(String ID) {
            if (this.ID.equals(ID)) {
                return this;
            } else {
                return null;
            }
        }

        // output GPS point in string: (ID,longitude,latitude,time,prevLocID,nextLocID)
        public String printGPSPoint() {
            String PointLocation = ID + "," + latitude + "," + longitude + "," + time.getTime();
            if (prevPoint != null) {
                PointLocation += "," + prevPoint.getID();
            } else {
                PointLocation += ",None";
            }

            if (nextPoint != null) {
                PointLocation += "," + nextPoint.getID();
            } else {
                PointLocation += ",None";
            }

            return PointLocation;
        }

        public String getID() {
            return ID;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getOrigLatitude() {
            return origLatitude;
        }

        public double getOrigLongitude() {
            return origLongitude;
        }

        public String getPrevPointID() {
            return prevPointID;
        }

        public String getNextPointID() {
            return nextPointID;
        }

        public GPSPoint getPrevPoint() {
            return prevPoint;
        }

        public GPSPoint getNextPoint() {
            return nextPoint;
        }

        public Date getTime() {
            return time;
        }

        public void setID(String ID) {
            this.ID = ID;
        }

        public void setLatitude(String latitude) {
            this.latitude = Double.parseDouble(latitude);
        }

        public void setLongitude(String longitude) {
            this.longitude = Double.parseDouble(longitude);
        }

        public void setOrigLatitude(String origLatitude) {
            this.origLatitude = Double.parseDouble(origLatitude);
        }

        public void setOrigLongitude(String origLongitude) {
            this.origLongitude = Double.parseDouble(origLongitude);
        }

        public void setTime(Date time) {
            this.time = time;
        }

        public void setPrevPointID(String prevPointID) {
            this.prevPointID = prevPointID;
        }

        public void setNextPointID(String nextPointID) {
            this.nextPointID = nextPointID;
        }

        public void setPrevPoint(GPSPoint prevPoint) {
            this.prevPoint = prevPoint;
        }

        public void setNextPoint(GPSPoint nextPoint) {
            this.nextPoint = nextPoint;
        }
    }

    public class Trajectory {
        private List<GPSPoint> pointList;

        public Trajectory() {
            pointList = new ArrayList<>();
        }

        public void addPoint(GPSPoint newPoint) {
            pointList.add(newPoint);
        }

        public int pointCount() {
            return pointList.size();
        }

        public List<GPSPoint> getPointList() {
            return pointList;
        }

        public Date getStartTime() {
            return pointList.get(0).getTime();
        }

        public Date getEndTime() {
            return pointList.get(pointCount() - 1).getTime();
        }

        public long getTimeSpan() {
            return getEndTime().getTime() - getStartTime().getTime();
        }
    }

    public List<Trajectory> getAllTraj(File trajInput) {
        List<Trajectory> trajSet = new ArrayList<>();
        if (trajInput.isFile()) {
            trajSet.add(readTrajFromFile(trajInput));
        } else if (trajInput.isDirectory()) {
            File[] subTrajFiles = trajInput.listFiles();
            if (subTrajFiles != null) {
                for (File subTrajFile : subTrajFiles) {
                    trajSet.addAll(getAllTraj(subTrajFile));
                }
            }
        }
        return trajSet;
    }

    // change the input path filter here
    @Nullable
    public Trajectory readTrajFromFile(File inputFile) {

        // create new trajectory
        Trajectory newTraj = new Trajectory();

        // maintain an index for points
        HashMap<String, GPSPoint> newPointList = new HashMap<>();

        String directoryPath = inputFile.getPath().substring(0, inputFile.getPath().lastIndexOf("\\") + 1);
        String fileName = inputFile.getPath().substring(inputFile.getPath().lastIndexOf("\\") + 1);

        try {
            if (fileName.startsWith("trip_")) {
                BufferedReader br = new BufferedReader(new FileReader(inputFile.getPath()));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] record = line.split(",");
                    GPSPoint newPoint = new GPSPoint(record[0], record[1], record[2], record[3]);

                    // index point by ID
                    newPointList.put(newPoint.getID(), newPoint);

                    // store prev/next point ID
                    newPoint.setPrevPointID(record[4]);
                    newPoint.setNextPointID(record[5]);

                    // add new point to trajectory
                    newTraj.addPoint(newPoint);
                }
                br.close();
            }
        } catch (FileNotFoundException e) {
            System.out.println("Input File not Found:" + inputFile.getPath());
        } catch (IOException e) {
            System.out.println("Readline failed.");
        }

        for (GPSPoint point : newTraj.getPointList()) {
            if (!point.getPrevPointID().equals("None")) {
                point.setPrevPoint(newPointList.get(point.getPrevPointID()));
            } else point.setPrevPoint(null);

            if (!point.getNextPointID().equals("None")) {
                point.setNextPoint(newPointList.get(point.getNextPointID()));
            } else point.setNextPoint(null);
        }
        System.out.println("Trajectory point count:" + newTraj.pointCount());
        return newTraj;

    }
}