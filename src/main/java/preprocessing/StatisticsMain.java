package preprocessing;

import algorithm.mapinference.lineclustering.DouglasPeuckerFilter;
import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.EuclideanDistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.MapReader;
import util.io.MatchResultReader;
import util.io.TrajectoryReader;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Trajectory;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.settings.MapServiceLogger;
import util.settings.PreprocessingProperty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Hellisk
 * @since 15/05/2019
 */
public class StatisticsMain {
	public static void main(String[] args) throws IOException {
		
		// initialize arguments
		PreprocessingProperty property = new PreprocessingProperty();
		property.loadPropertiesFromResourceFile("preprocessing.properties", args);
		long initTaskTime = System.currentTimeMillis();
		
		// setup java log
		String logPath = property.getPropertyString("algorithm.preprocessing.log.LogFolder");  // obtain the log folder from args
		String dataSet = property.getPropertyString("data.Dataset");
		String inputTrajFolder = property.getPropertyString("path.InputTrajectoryFolder");
		String gtPointMatchResultFolder = property.getPropertyString("path.GroundTruthPointMatchResultFolder");
		String inputMapPath = property.getPropertyString("path.InputMapFolder");
		int downSampleRate = property.getPropertyInteger("data.DownSample");
		double tolerance = property.getPropertyDouble("data.Tolerance");
		// log file name
		String logFileName = "statistics_" + dataSet + "_" + initTaskTime;
		// initialize log file
		MapServiceLogger.logInit(logPath, logFileName);
		
		final Logger LOG = Logger.getLogger(PreprocessingMain.class);
		LOG.info("Data preprocessing statistic process start.");
		
		DistanceFunction distFunc;
		if (dataSet.contains("Beijing")) {
			distFunc = new GreatCircleDistanceFunction();
		} else {
			distFunc = new EuclideanDistanceFunction();
		}
		Stream<Trajectory> inputTrajStream = TrajectoryReader.readTrajectoriesToStream(inputTrajFolder, downSampleRate, tolerance,
				distFunc);
		List<Trajectory> inputCompressedTrajList = inputTrajStream.collect(Collectors.toList());
		RoadNetworkGraph inputMap = MapReader.readMap(inputMapPath + "0.txt", false, distFunc);
		List<Pair<Integer, List<PointMatch>>> gtPointMatchResultList = MatchResultReader.readPointMatchResults(gtPointMatchResultFolder,
				downSampleRate, distFunc);
		HashMap<Integer, List<PointMatch>> id2GTPointMatch = new HashMap<>();
		List<Pair<Integer, List<PointMatch>>> revisedGTPointMatchResult = new ArrayList<>();
		for (Pair<Integer, List<PointMatch>> currGTPair : gtPointMatchResultList) {
			if (id2GTPointMatch.containsKey(currGTPair._1()))
				throw new IllegalArgumentException("Duplicate ID from ground-truth map-matching result: " + currGTPair._1());
			id2GTPointMatch.put(currGTPair._1(), currGTPair._2());
		}
		List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, downSampleRate, distFunc);
		DouglasPeuckerFilter dpFilter = new DouglasPeuckerFilter(tolerance, distFunc);
		for (int i = 0; i < inputTrajList.size(); i++) {
			Trajectory currTraj = inputTrajList.get(i);
			List<Integer> keyTrajPointList = dpFilter.dpSimplifier(currTraj);    // the indices of the key trajectory points for
			List<PointMatch> currPointMatchList = id2GTPointMatch.get(Integer.parseInt(currTraj.getID()));
			if (currTraj.size() != currPointMatchList.size())
				throw new IllegalArgumentException("The pre-compression trajectory has different length with the ground-truth:"
						+ currTraj.size() + "," + currPointMatchList.size() + "," + currTraj.getID() + "," + i);
			List<PointMatch> revisedPointMatchList = new ArrayList<>();
			for (Integer index : keyTrajPointList) {
				revisedPointMatchList.add(currPointMatchList.get(index));
			}
			revisedGTPointMatchResult.add(new Pair<>(Integer.parseInt(currTraj.getID()), revisedPointMatchList));
		}
		gtPointMatchResultList = revisedGTPointMatchResult;
		PreprocessingStatistics.datasetStatsCalc(inputCompressedTrajList, inputMap, gtPointMatchResultList);
		LOG.info("Statistics calculation done.");
	}
}
