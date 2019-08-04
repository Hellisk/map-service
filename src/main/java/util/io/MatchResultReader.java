package util.io;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.structure.MultipleTrajectoryMatchResult;
import util.object.structure.Pair;
import util.object.structure.PointMatch;

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
	
	public static MultipleTrajectoryMatchResult readMatchResult(String filePath, DistanceFunction df) {
		String line = IOService.readFileContent(filePath);
		return MultipleTrajectoryMatchResult.parseTrajectoryMatchResult(line, df);
	}
	
	public static List<MultipleTrajectoryMatchResult> readMatchResultsToList(String fileFolder, DistanceFunction df) {
		File inputFolder = new File(fileFolder);
		if (!inputFolder.exists())
			throw new IllegalArgumentException("The input matching result path doesn't exist: " + fileFolder);
		List<MultipleTrajectoryMatchResult> matchResultList = new ArrayList<>();
		if (inputFolder.isDirectory()) {
			File[] matchResultFiles = inputFolder.listFiles();
			if (matchResultFiles != null) {
				for (File matchResultFile : matchResultFiles) {
					matchResultList.add(readMatchResult(matchResultFile.getAbsolutePath(), df));
				}
			} else
				LOG.error("The input matching result dictionary is empty: " + fileFolder);
		} else
			matchResultList.add(readMatchResult(inputFolder.getAbsolutePath(), df));
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
		List<Pair<Integer, List<String>>> gtResult = new ArrayList<>();
		if (f.isDirectory()) {
			File[] fileList = f.listFiles();
			if (fileList != null) {
				for (File file : fileList) {
					List<String> matchResult = IOService.readFile(file.getAbsolutePath());
					// TODO check whether "\n"(empty line) is included in the results
					int fileNum = Integer.parseInt(file.getName().substring(file.getName().indexOf('_') + 1, file.getName().indexOf('.')));
					gtResult.add(new Pair<>(fileNum, matchResult));
				}
			}
		}
		return gtResult;
	}
	
	/**
	 * Read the matching results which only have the point matches.
	 *
	 * @param fileFolder The matching result folder
	 * @return The list of matching result pairs, each of which contains the trajectory ID and its point match list
	 */
	public static List<Pair<Integer, List<PointMatch>>> readPointMatchResults(String fileFolder, DistanceFunction df) {
		File inputFolder = new File(fileFolder);
		List<Pair<Integer, List<PointMatch>>> matchResultList = new ArrayList<>();
		if (inputFolder.isDirectory()) {
			File[] fileList = inputFolder.listFiles();
			if (fileList != null) {
				for (File file : fileList) {
					List<String> lines = IOService.readFile(file.getAbsolutePath());
					List<PointMatch> matchResult = new ArrayList<>();
					for (String line : lines) {
						matchResult.add(PointMatch.parsePointMatch(line, df));
					}
					int fileNum = Integer.parseInt(file.getName().substring(file.getName().indexOf('_') + 1, file.getName().indexOf('.')));
					matchResultList.add(new Pair<>(fileNum, matchResult));
				}
			}
		} else {
			List<String> lines = IOService.readFile(inputFolder.getAbsolutePath());
			List<PointMatch> matchResult = new ArrayList<>();
			for (String line : lines) {
				matchResult.add(PointMatch.parsePointMatch(line, df));
			}
			int fileNum = Integer.parseInt(inputFolder.getName().substring(inputFolder.getName().indexOf('_') + 1, inputFolder.getName().indexOf('.')));
			matchResultList.add(new Pair<>(fileNum, matchResult));
		}
		return matchResultList;
	}
}
