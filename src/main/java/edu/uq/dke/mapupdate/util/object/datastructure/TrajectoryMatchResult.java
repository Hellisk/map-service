package edu.uq.dke.mapupdate.util.object.datastructure;

import edu.uq.dke.mapupdate.util.object.spatialobject.Trajectory;

import java.util.ArrayList;
import java.util.List;

public class TrajectoryMatchResult {
    private String trajID;
    private List<PointNodePair> matchingResult = new ArrayList<>();
    private List<String> matchWayList = new ArrayList<>();
    private double probability = -1;

    public TrajectoryMatchResult(Trajectory traj) {
        this.trajID = traj.getId();
    }

    public TrajectoryMatchResult(String traj) {
        this.trajID = traj;
    }

    public String getTrajID() {
        return trajID;
    }

    public List<PointNodePair> getMatchingResult() {
        return matchingResult;
    }

    public void setMatchingResult(List<PointNodePair> result) {
        this.matchingResult = result;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public List<String> getMatchWayList() {
        return matchWayList;
    }

    public void setMatchWayList(List<String> matchWayList) {
        this.matchWayList = matchWayList;
    }

    public void addMatchWay(String roadWay) {
        this.matchWayList.add(roadWay);
    }
}