package edu.uq.dke.mapupdate.mapmatching.hmm;

import edu.uq.dke.mapupdate.datatype.MatchingResult;
import traminer.util.map.matching.PointNodePair;
import traminer.util.map.roadnetwork.RoadNetworkGraph;
import traminer.util.spatial.distance.GreatCircleDistanceFunction;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by uqpchao on 22/05/2017.
 */
public class NewsonHMM2009 {

    private List<Trajectory> unmatchedTraj = new ArrayList<>();

    public List<MatchingResult> mapMatchingProcess(List<Trajectory> inputTrajectory, RoadNetworkGraph currMap) {

        GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
        HMMMapMatching hmm = new HMMMapMatching(distFunc, 50, 15, currMap);
        // sequential test
        List<MatchingResult> result = new ArrayList<>();
        int matchCount = 0;
        for (Trajectory traj : inputTrajectory) {
            List<PointNodePair> pointMatchPair = hmm.doMatching(traj, currMap);
            MatchingResult matchResult = new MatchingResult(traj);
            matchResult.setMatchingResult(pointMatchPair);
            result.add(matchResult);
            matchCount++;
//            if (inputTrajectory.size() > 100)
//                if (matchCount % (inputTrajectory.size() / 100) == 0)
//                    System.out.println("Map matching finish " + matchCount / (inputTrajectory.size() / 100) + "%. Broken trajectory count:" + hmm.getBrokenTrajCount() + ".");
//            matchCount++;
            System.out.println("Matching finished:" + matchCount);
        }
        unmatchedTraj = hmm.getUnMatchedTraj();
        return result;
    }

    public List<Trajectory> getUnmatchedTraj() {
        return unmatchedTraj;
    }
}
