package edu.uq.dke.mapupdate.mapmatching.hmm;

import edu.uq.dke.mapupdate.util.function.GreatCircleDistanceFunction;
import edu.uq.dke.mapupdate.util.object.datastructure.TrajectoryMatchingResult;
import edu.uq.dke.mapupdate.util.object.roadnetwork.RoadNetworkGraph;
import edu.uq.dke.mapupdate.util.object.spatialobject.Trajectory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Stream;

/**
 * Created by uqpchao on 22/05/2017.
 */
public class NewsonHMM2009 {
    private int candidateRange;    // in meter
    private int gapExtensionRange; // in meter
    private int rankLength; // in meter

    private List<Trajectory> unmatchedTraj = new ArrayList<>();

    public NewsonHMM2009(int candidateRange, int unmatchedTrajThreshold, int rankLength) {
        this.candidateRange = candidateRange;
        this.gapExtensionRange = unmatchedTrajThreshold;
        this.rankLength = rankLength;
    }

    public List<TrajectoryMatchingResult> trajectoryListMatchingProcess(List<Trajectory> rawTrajectory, RoadNetworkGraph currMap) {

        GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
        HMMMapMatching hmm = new HMMMapMatching(distFunc, candidateRange, gapExtensionRange, rankLength, currMap);
        // sequential test
        List<TrajectoryMatchingResult> result = new ArrayList<>();
        int matchCount = 0;
        for (Trajectory traj : rawTrajectory) {
//            if(traj.getId().equals("1027"))
//                System.out.println("test");
            TrajectoryMatchingResult matchResult = hmm.doMatching(traj);
            result.add(matchResult);
            if (rawTrajectory.size() > 100)
                if (matchCount % (rawTrajectory.size() / 100) == 0 && matchCount / (rawTrajectory.size() / 100) <= 100)
                    System.out.println("Map matching finish " + matchCount / (rawTrajectory.size() / 100) + "%.");
            matchCount++;
        }
        this.unmatchedTraj = hmm.getUnMatchedTraj();
        return result;
    }

    public Stream<TrajectoryMatchingResult> trajectoryStreamMatchingProcess(Stream<Trajectory> inputTrajectory, RoadNetworkGraph currMap, int
            numOfThreads) throws ExecutionException, InterruptedException {

        if (inputTrajectory == null) {
            throw new IllegalArgumentException("Trajectory stream for map-matching must not be null.");
        }
        if (currMap == null || currMap.isEmpty()) {
            throw new IllegalArgumentException("Road-Network-Graph for map-matching must not be empty nor null.");
        }

        final ThreadLocal<HMMMapMatching> matchingMethodThread =
                new ThreadLocal<>();


        GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
        HMMMapMatching hmm = new HMMMapMatching(distFunc, candidateRange, gapExtensionRange, rankLength, currMap);
        // sequential test
        ForkJoinPool forkJoinPool = new ForkJoinPool(numOfThreads);
        ForkJoinTask<Stream<TrajectoryMatchingResult>> taskResult =
                forkJoinPool.submit(() -> {
                    matchingMethodThread.set(hmm);
                    Stream<TrajectoryMatchingResult> matchResultStream =
                            inputTrajectory.parallel().map(trajectory -> {
                                TrajectoryMatchingResult matchPairs = matchingMethodThread.get()
                                        .doMatching(trajectory);
                                try {
                                    Thread.sleep(5);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                return matchPairs;
                            });
                    this.unmatchedTraj = hmm.getUnMatchedTraj();
                    return matchResultStream;
                });
        return taskResult.get();
    }

    public TrajectoryMatchingResult trajectoryMatchingProcess(Trajectory inputTrajectory, RoadNetworkGraph currMap) {

        GreatCircleDistanceFunction distFunc = new GreatCircleDistanceFunction();
        HMMMapMatching hmm = new HMMMapMatching(distFunc, candidateRange, gapExtensionRange, rankLength, currMap);
        // sequential test
        TrajectoryMatchingResult matchResult = hmm.doMatching(inputTrajectory);
//            if (inputTrajectory.size() > 100)
//                if (matchCount % (inputTrajectory.size() / 100) == 0)
//                    System.out.println("Map matching finish " + matchCount / (inputTrajectory.size() / 100) + "%. Broken trajectory count:" + hmm.getBrokenTrajCount() + ".");
//            matchCount++;
        System.out.println("Matching finished:" + inputTrajectory.getId());
        this.unmatchedTraj = hmm.getUnMatchedTraj();
        return matchResult;
    }

    public List<Trajectory> getUnmatchedTraj() {
        return unmatchedTraj;
    }
}
