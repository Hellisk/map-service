package edu.uq.dke.mapupdate.mapmatching.hmm;

import edu.uq.dke.mapupdate.datatype.TrajectoryMatchResult;
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

    public List<TrajectoryMatchResult> trajectoryListMatchingProcess(List<Trajectory> inputTrajectory, RoadNetworkGraph currMap) {

        GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
        HMMMapMatching hmm = new HMMMapMatching(distFunc, 50, 20, currMap);
        // sequential test
        List<TrajectoryMatchResult> result = new ArrayList<>();
        int matchCount = 0;
        for (Trajectory traj : inputTrajectory) {
            TrajectoryMatchResult matchResult = hmm.doMatching(traj, currMap);
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

    public TrajectoryMatchResult trajectoryMatchingProcess(Trajectory inputTrajectory, RoadNetworkGraph currMap) {

        GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
        HMMMapMatching hmm = new HMMMapMatching(distFunc, 50, 20, currMap);
        // sequential test
        TrajectoryMatchResult matchResult = hmm.doMatching(inputTrajectory, currMap);
//            if (inputTrajectory.size() > 100)
//                if (matchCount % (inputTrajectory.size() / 100) == 0)
//                    System.out.println("Map matching finish " + matchCount / (inputTrajectory.size() / 100) + "%. Broken trajectory count:" + hmm.getBrokenTrajCount() + ".");
//            matchCount++;
        System.out.println("Matching finished:" + inputTrajectory.getId());
        unmatchedTraj = hmm.getUnMatchedTraj();
        return matchResult;
    }

    public List<Trajectory> getUnmatchedTraj() {
        return unmatchedTraj;
    }
}
