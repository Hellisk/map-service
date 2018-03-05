package edu.uq.dke.mapupdate.datatype;

import traminer.util.map.matching.PointNodePair;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

public class MatchingResult {
    private String trajID;
    private List<PointNodePair> matchingResult = new ArrayList<>();
    private double probability = -1;

    public MatchingResult(Trajectory traj) {
        this.trajID = traj.getId();
    }

    public MatchingResult(String traj) {
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

}
