package util.io;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Read trajectories from given folder.
 *
 * @author Hellisk
 * @since 23/05/2017
 */
public class TrajectoryReader {
	
	private static final Logger LOG = Logger.getLogger(TrajectoryReader.class);
	
	public static Trajectory readTrajectory(String filePath, String trajID, int downSampleRate, DistanceFunction distFunc) {
		List<String> pointInfo = IOService.readFile(filePath);
		Trajectory newTrajectory = new Trajectory(trajID, distFunc);
		for (int i = 0; i < pointInfo.size(); i++) {
			String s = pointInfo.get(i);
			TrajectoryPoint newTrajectoryPoint = TrajectoryPoint.parseTrajectoryPoint(s, distFunc);
			if (i == 0 || i % downSampleRate == 0 || i == pointInfo.size() - 1)
				newTrajectory.add(newTrajectoryPoint);
		}
		return newTrajectory;
	}
	
	/**
	 * Read all trajectories from a folder and store as a list.
	 *
	 * @param fileFolder The folder path.
	 * @param df         The distance function
	 * @return The output trajectory list.
	 */
	public static List<Trajectory> readTrajectoriesToList(String fileFolder, int downSampleRate, DistanceFunction df) {
		File inputFile = new File(fileFolder);
		List<Trajectory> trajectoryList = new ArrayList<>();
		if (!inputFile.exists())
			throw new IllegalArgumentException("The input trajectory path doesn't exist: " + fileFolder);
		if (inputFile.isDirectory()) {
			File[] trajectoryFiles = inputFile.listFiles();
			if (trajectoryFiles != null) {
				for (File trajectoryFile : trajectoryFiles) {
					if (!trajectoryFile.getName().substring(trajectoryFile.getName().indexOf(".")).matches(".txt")) {
						continue;
					}
					String trajID = trajectoryFile.getName().substring(trajectoryFile.getName().indexOf('_') + 1,
							trajectoryFile.getName().indexOf('.'));
					Trajectory newTrajectory = readTrajectory(trajectoryFile.getAbsolutePath(), trajID, downSampleRate, df);
					trajectoryList.add(newTrajectory);
				}
			} else
				LOG.error("The input trajectory dictionary is empty: " + fileFolder);
		} else {
			trajectoryList.add(readTrajectory(fileFolder, 0 + "", downSampleRate, df));
		}
		int count = 0;
		for (Trajectory t : trajectoryList) {
			count += t.getCoordinates().size();
		}
		
		LOG.debug("Trajectories reading finished, total number of trajectories:" + trajectoryList.size() + ", trajectory points:" + count);
		return trajectoryList;
	}
	
	/**
	 * Read and parse the input CSV trajectory files to a Stream
	 * of trajectories.
	 *
	 * @param fileFolder     The trajectory input path.
	 * @param downSampleRate Down-sample the input trajectory rate by
	 * @param df             The distance function.
	 */
	public static Stream<Trajectory> readTrajectoriesToStream(String fileFolder, int downSampleRate, DistanceFunction df) {
		// read input data
		File inputFile = new File(fileFolder);
		if (!inputFile.exists())
			LOG.error("The input trajectory path doesn't exist: " + fileFolder);
		Stream<File> dataFiles = IOService.getFiles(fileFolder);
//		if (indexType != 0)
//			indexPointList = Collections.synchronizedList(new ArrayList<>());
		return dataFiles.parallel().map(
				file -> {
					if (!file.getName().matches("trip[_][0-9]*[.]txt")) {
//						System.out.println(file.getName());
						return null;
					}
					String trajID = file.getName().substring(file.getName().indexOf('_') + 1, file.getName().lastIndexOf('.'));
					Trajectory newTrajectory = readTrajectory(file.getAbsolutePath(), trajID, downSampleRate, df);
					newTrajectory.setID(trajID);
					return newTrajectory;
				});
	}
	
	/**
	 * Read and parse the input CSV trajectory files to a Stream of trajectories given a list of trajectory ID.
	 *
	 * @param fileFolder The trajectory input path
	 * @param trajIDSet  The set of trajectory ID to be read
	 */
	public static Stream<Trajectory> readTrajectoriesToStream(String fileFolder, Set<String> trajIDSet, DistanceFunction df) {
		// read input data
		File inputFile = new File(fileFolder);
		if (!inputFile.exists())
			LOG.error("ERROR! The input trajectory path doesn't exist: " + fileFolder);
		Stream<File> dataFiles = IOService.getFilesWithIDs(fileFolder, trajIDSet);
		return dataFiles.parallel().map(
				file -> {
					String trajID = file.getName().substring(file.getName().indexOf('_') + 1, file.getName().indexOf('.'));
					Trajectory newTrajectory = readTrajectory(file.getAbsolutePath(), trajID, 1, df);
					newTrajectory.setID(trajID);
					return newTrajectory;
				});
	}
}
