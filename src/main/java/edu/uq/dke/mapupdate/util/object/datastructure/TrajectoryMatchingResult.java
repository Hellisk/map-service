package edu.uq.dke.mapupdate.util.object.datastructure;

import edu.uq.dke.mapupdate.util.object.spatialobject.STPoint;
import edu.uq.dke.mapupdate.util.object.spatialobject.Trajectory;

import java.util.ArrayList;
import java.util.List;

public class TrajectoryMatchingResult {
    private Trajectory trajectory;
    private List<List<PointMatch>> matchingResult;
    private List<List<String>> matchWayList;
    private double[] probabilities;
    private int rankLength;

    public TrajectoryMatchingResult(Trajectory traj, int rankLength) {
        if (rankLength < 1) throw new IndexOutOfBoundsException("ERROR! Rank should be at least 1");
        this.trajectory = traj;
        this.rankLength = rankLength;
        this.matchingResult = new ArrayList<>(rankLength);
        this.matchWayList = new ArrayList<>(rankLength);
        this.probabilities = new double[rankLength];
        for (int i = 0; i < rankLength; i++) {
            this.matchingResult.add(new ArrayList<>());
            this.matchWayList.add(new ArrayList<>());
            this.probabilities[i] = 0;
        }
    }


    public String getTrajID() {
        return trajectory.getId();
    }

    public Trajectory getRawTrajectory() {
        return this.trajectory;
    }

    public int getTrajLength() {
        return this.trajectory.size();
    }

    public STPoint getTrajPoint(int position) {
        return this.trajectory.get(position);
    }

    /**
     * Get the highest ranked matching result
     *
     * @return the optimal matching result
     */
    public List<PointMatch> getBestMatchingResult() {
        return matchingResult.get(0);
    }

    /**
     * Get the specified ranked matching result
     *
     * @param rank the rank specified
     * @return the optimal matching result
     */
    public List<PointMatch> getMatchingResult(int rank) {
        if (rank > rankLength) throw new IndexOutOfBoundsException("ERROR! The specified rank is out of range.");
        return matchingResult.get(rank);
    }

    /**
     * Set the matching result of the specified rank
     *
     * @param rank   the specified rank
     * @param result matching result
     */
    public void setMatchingResult(List<PointMatch> result, int rank) {
        if (rank > rankLength) throw new IndexOutOfBoundsException("ERROR! Matching result set failed: the specified rank is out of range" +
                ".");
        if (result.size() != trajectory.size() && !result.isEmpty()) System.out.println("Match result size is different from the raw " +
                "trajectory:" + result.size() + ":" + trajectory.size());
        this.matchingResult.set(rank, result);
    }

    public double getBestProbability() {
        return probabilities[0];
    }

    public double getProbability(int rank) {
        if (rank > rankLength) throw new IndexOutOfBoundsException("ERROR! Probability get failed: the specified rank is out of range.");
        return probabilities[rank];
    }

    public void setProbability(double probability, int rank) {
        if (rank > rankLength) throw new IndexOutOfBoundsException("ERROR! Probability set failed: the specified rank is out of range.");
        this.probabilities[rank] = probability;
    }

    public void setProbabilities(double[] probabilities) {
        if (probabilities.length != rankLength) throw new IndexOutOfBoundsException("ERROR! Probabilities set failed: inconsistent " +
                "probabilities set size");
        this.probabilities = probabilities;
    }

    public List<String> getBestMatchWayList() {
        return matchWayList.get(0);
    }

    public List<String> getMatchWayList(int rank) {
        if (rank > rankLength) throw new IndexOutOfBoundsException("ERROR! way list get failed: the specified rank is out of range.");
        return matchWayList.get(rank);
    }

    public void setMatchWayList(List<String> matchWayList, int rank) {
        if (rank > rankLength) throw new IndexOutOfBoundsException("ERROR! Match way list set failed: the specified rank is out of range.");
        this.matchWayList.set(rank, matchWayList);
    }

    public void addMatchWay(String roadWay, int rank) {
        if (rank > rankLength) throw new IndexOutOfBoundsException("ERROR! Match way add failed: the specified rank is out of range.");
        this.matchWayList.get(rank).add(roadWay);
    }

    public void setMatchWayLists(List<List<String>> matchWayLists) {
        if (matchWayLists.size() != rankLength) throw new IndexOutOfBoundsException("ERROR! Match way lists set failed: inconsistent " +
                "list size");
        this.matchWayList = matchWayLists;
    }

    /**
     * Get the count of matching results that have positive matching probabilities
     *
     * @return the count of top-ranked none-zero matching results
     */
    public int getNumOfPositiveRank() {
        for (int i = 0; i < rankLength; i++) {
            if (probabilities[i] == 0)
                return i + 1;
        }
        return rankLength;
    }
}