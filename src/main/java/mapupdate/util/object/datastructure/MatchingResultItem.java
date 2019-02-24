package mapupdate.util.object.datastructure;

/*
  The matching result of a trajectory, including the matching result and the derived unmatched trajectories.
 */

import mapupdate.util.object.spatialobject.Trajectory;

import java.util.List;

public class MatchingResultItem {
    private TrajectoryMatchingResult matchingResult;
    private List<Triplet<Trajectory, String, String>> unmatchedTrajectoryList;

    public MatchingResultItem(TrajectoryMatchingResult matchingResult, List<Triplet<Trajectory, String, String>> unmatchedTrajectoryList) {
        this.matchingResult = matchingResult;
        this.unmatchedTrajectoryList = unmatchedTrajectoryList;
    }

    public String getTrajID() {
        return matchingResult.getTrajID();
    }

    public TrajectoryMatchingResult getMatchingResult() {
        return matchingResult;
    }

    public List<Triplet<Trajectory, String, String>> getUnmatchedTrajectoryList() {
        return unmatchedTrajectoryList;
    }
}