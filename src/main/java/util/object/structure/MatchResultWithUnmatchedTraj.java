package util.object.structure;

/*
  The matching result of a trajectory, including the matching result and the derived unmatched trajectories.
 */

import util.object.spatialobject.Trajectory;

import java.io.Serializable;
import java.util.List;

public class MatchResultWithUnmatchedTraj implements Serializable {
	
	private TrajectoryMatchResult matchResult;
    private List<Triplet<Trajectory, String, String>> unmatchedTrajectoryList;
	
	public MatchResultWithUnmatchedTraj(TrajectoryMatchResult matchResult, List<Triplet<Trajectory, String, String>> unmatchedTrajectoryList) {
		this.matchResult = matchResult;
        this.unmatchedTrajectoryList = unmatchedTrajectoryList;
    }

    public String getTrajID() {
		return matchResult.getTrajID();
    }
	
	public TrajectoryMatchResult getMatchResult() {
		return matchResult;
    }

    public List<Triplet<Trajectory, String, String>> getUnmatchedTrajectoryList() {
        return unmatchedTrajectoryList;
    }
}