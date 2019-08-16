package util.io;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.structure.MultipleTrajectoryMatchResult;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.object.structure.SimpleTrajectoryMatchResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Read the map-matching result from file.
 *
 * @author uqpchao
 * Created 7/04/2019
 */
public class MatchResultReader {
	
	private static final Logger LOG = Logger.getLogger(MatchResultReader.class);
	
	private static MultipleTrajectoryMatchResult readComplexMatchResult(String filePath, DistanceFunction df) {
		String line = IOService.readFileContent(filePath);
		return MultipleTrajectoryMatchResult.parseTrajectoryMatchResult(line, df);
	}
	
	private static SimpleTrajectoryMatchResult readSimpleMatchResult(String filePath, String trajID, DistanceFunction df) {
		String line = IOService.readFileContent(filePath);
		return SimpleTrajectoryMatchResult.parseSimpleTrajMatchResult(line, trajID, df);
	}
	
	/**
	 * Read the match results into <tt>MultipleTrajectoryMatchResult</tt> from given folder.
	 *
	 * @param fileFolder Input folder
	 * @param df         Distance Function
	 * @return A list of complex trajectory matching results.
	 */
	public static List<MultipleTrajectoryMatchResult> readComplexMatchResultsToList(String fileFolder, DistanceFunction df) {
		File inputFolder = new File(fileFolder);
		if (!inputFolder.exists())
			throw new IllegalArgumentException("The input matching result path doesn't exist: " + fileFolder);
		List<MultipleTrajectoryMatchResult> matchResultList = new ArrayList<>();
		if (inputFolder.isDirectory()) {
			File[] matchResultFiles = inputFolder.listFiles();
			if (matchResultFiles != null) {
				for (File matchResultFile : matchResultFiles) {
					if (matchResultFile.toString().contains("matchresult_"))
						matchResultList.add(readComplexMatchResult(matchResultFile.getAbsolutePath(), df));
				}
			} else
				LOG.error("The input matching result dictionary is empty: " + fileFolder);
		} else {
			if (inputFolder.toString().contains("matchresult_"))
				matchResultList.add(readComplexMatchResult(inputFolder.getAbsolutePath(), df));
		}
		if (matchResultList.isEmpty())
			throw new IllegalArgumentException("No match result is found in the folder: " + fileFolder);
		return matchResultList;
	}
	
	/**
	 * Read the match results into <tt>SimpleTrajectoryMatchResult</tt> from given folder.
	 *
	 * @param fileFolder Input folder
	 * @param df         Distance Function
	 * @return A list of simple trajectory matching results.
	 */
	public static List<SimpleTrajectoryMatchResult> readSimpleMatchResultsToList(String fileFolder, DistanceFunction df) {
		File inputFolder = new File(fileFolder);
		if (!inputFolder.exists())
			throw new IllegalArgumentException("The input matching result path doesn't exist: " + fileFolder);
		List<SimpleTrajectoryMatchResult> matchResultList = new ArrayList<>();
		if (inputFolder.isDirectory()) {
			File[] matchResultFiles = inputFolder.listFiles();
			if (matchResultFiles != null) {
				for (File matchResultFile : matchResultFiles) {
					if (matchResultFile.toString().contains("matchresult_")) {
						String trajID = matchResultFile.toString().substring(matchResultFile.toString().lastIndexOf("_") + 1,
								matchResultFile.toString().lastIndexOf("."));
						matchResultList.add(readSimpleMatchResult(matchResultFile.getAbsolutePath(), trajID, df));
					}
				}
			} else
				LOG.error("The input matching result dictionary is empty: " + fileFolder);
		} else {
			if (inputFolder.toString().contains("matchresult_")) {
				String trajID = inputFolder.toString().substring(inputFolder.toString().lastIndexOf("_") + 1,
						inputFolder.toString().lastIndexOf("."));
				matchResultList.add(readSimpleMatchResult(inputFolder.getAbsolutePath(), trajID, df));
			}
		}
		if (matchResultList.isEmpty())
			throw new IllegalArgumentException("No match result is found in the folder: " + fileFolder);
		return matchResultList;
	}
	
	/**
	 * Read the matching results which only have the match route IDs.
	 *
	 * @param fileFolder The matching result folder
	 * @return The list of matching result pairs, each of which contains the trajectory ID and its road ID list
	 */
	public static List<Pair<Integer, List<String>>> readRouteMatchResults(String fileFolder) {
		File f = new File(fileFolder);
		List<Pair<Integer, List<String>>> routeMatchResult = new ArrayList<>();
		if (f.isDirectory()) {
			File[] fileList = f.listFiles();
			if (fileList != null) {
				for (File file : fileList) {
					if (file.toString().contains("routematch_")) {
						List<String> matchResult = IOService.readFile(file.getAbsolutePath());
						int fileNum = Integer.parseInt(file.getName().substring(file.getName().indexOf('_') + 1, file.getName().indexOf('.')));
						routeMatchResult.add(new Pair<>(fileNum, matchResult));
					}
				}
			}
		}
		if (routeMatchResult.isEmpty())
			throw new IllegalArgumentException("No route match result is found in the folder: " + fileFolder);
		return routeMatchResult;
	}
	
	/**
	 * Read the matching results which only have the point matches.
	 *
	 * @param fileFolder The matching result folder
	 * @return The list of matching result pairs, each of which contains the trajectory ID and its point match list
	 */
	public static List<Pair<Integer, List<PointMatch>>> readPointMatchResults(String fileFolder, DistanceFunction df) {
		File inputFolder = new File(fileFolder);
		List<Pair<Integer, List<PointMatch>>> pointMatchResult = new ArrayList<>();
		if (inputFolder.isDirectory()) {
			File[] fileList = inputFolder.listFiles();
			if (fileList != null) {
				for (File file : fileList) {
					if (file.toString().contains("pointmatch_")) {
						List<String> lines = IOService.readFile(file.getAbsolutePath());
						List<PointMatch> matchResult = new ArrayList<>();
						for (String line : lines) {
							matchResult.add(PointMatch.parsePointMatch(line, df));
						}
						int fileNum = Integer.parseInt(file.getName().substring(file.getName().indexOf('_') + 1, file.getName().indexOf('.')));
						pointMatchResult.add(new Pair<>(fileNum, matchResult));
					}
				}
			}
		} else {
			if (inputFolder.toString().contains("pointmatch_")) {
				List<String> lines = IOService.readFile(inputFolder.getAbsolutePath());
				List<PointMatch> matchResult = new ArrayList<>();
				for (String line : lines) {
					matchResult.add(PointMatch.parsePointMatch(line, df));
				}
				int fileNum = Integer.parseInt(inputFolder.getName().substring(inputFolder.getName().indexOf('_') + 1, inputFolder.getName().indexOf('.')));
				pointMatchResult.add(new Pair<>(fileNum, matchResult));
			}
		}
		if (pointMatchResult.isEmpty())
			throw new IllegalArgumentException("No route match result is found in the folder: " + fileFolder);
		return pointMatchResult;
	}
}
