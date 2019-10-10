package preprocessing;

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
import java.util.List;

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
		List<Trajectory> inputTrajList = TrajectoryReader.readTrajectoriesToList(inputTrajFolder, downSampleRate, distFunc);
		RoadNetworkGraph inputMap = MapReader.readMap(inputMapPath + "0.txt", false, distFunc);
		List<Pair<Integer, List<PointMatch>>> gtPointMatchResultList = MatchResultReader.readPointMatchResults(gtPointMatchResultFolder,
				downSampleRate, distFunc);
		PreprocessingStatistics.datasetStatsCalc(inputTrajList, inputMap, gtPointMatchResultList);
		LOG.info("Statistics calculation done.");
	}
}
