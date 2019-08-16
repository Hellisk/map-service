package util.io;

import org.apache.log4j.Logger;
import util.object.structure.MultipleTrajectoryMatchResult;
import util.object.structure.Pair;
import util.object.structure.PointMatch;
import util.object.structure.SimpleTrajectoryMatchResult;

import java.io.File;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

/**
 * Write map-matching result to the file.
 *
 * @author uqpchao
 * Created 7/04/2019
 */
public class MatchResultWriter {
	
	private static final Logger LOG = Logger.getLogger(TrajectoryReader.class);
	
	/**
	 * Writer for writing matching results. The format follows the <tt>MultipleTrajectoryMatchResult.toString()</tt> format.
	 *
	 * @param matchingList The matching results.
	 * @param fileFolder   The output folder path.
	 */
	public static void writeMultipleMatchResults(List<MultipleTrajectoryMatchResult> matchingList, String fileFolder) {
		if (matchingList == null)
			throw new NullPointerException("The input matching result list is empty.");
		
		IOService.createFolder(fileFolder);
		IOService.cleanFolder(fileFolder);
		
		File folder = new File(fileFolder);
		Stream<MultipleTrajectoryMatchResult> trajectoryMatchingStream = matchingList.stream();
		
		// parallel processing
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		forkJoinPool.submit(() -> trajectoryMatchingStream.parallel().forEach(x -> IOService.writeFile(x.toString(), fileFolder
				, "matchresult_" + x.getTrajID() + ".txt")));
		while (Objects.requireNonNull(folder.list()).length != matchingList.size()) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		LOG.debug("Matching results written, total file count: " + matchingList.size());
	}
	
	/**
	 * Writer for writing matching results. The format follows the <tt>SimpleTrajectoryMatchResult.toString()</tt> format.
	 *
	 * @param matchingList The matching results.
	 * @param fileFolder   The output folder path.
	 */
	public static void writeMatchResults(List<SimpleTrajectoryMatchResult> matchingList, String fileFolder) {
		if (matchingList == null)
			throw new NullPointerException("The input matching result list is empty.");
		
		IOService.createFolder(fileFolder);
		IOService.cleanFolder(fileFolder);
		
		File folder = new File(fileFolder);
		Stream<SimpleTrajectoryMatchResult> trajectoryMatchingStream = matchingList.stream();
		
		// parallel processing
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		forkJoinPool.submit(() -> trajectoryMatchingStream.parallel().forEach(x -> IOService.writeFile(x.toString(), fileFolder
				, "matchresult_" + x.getTrajID() + ".txt")));
		while (Objects.requireNonNull(folder.list()).length != matchingList.size()) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		LOG.debug("Matching results written, total file count: " + matchingList.size());
	}
	
	/**
	 * Writer for writing point match results. The result format follows the <tt>PointMatch.toString()</tt> format.
	 *
	 * @param pointMatchResultList The matching results.
	 * @param fileFolder           The output folder path.
	 */
	public static void writePointMatchResults(List<Pair<Integer, List<PointMatch>>> pointMatchResultList, String fileFolder) {
		if (pointMatchResultList == null)
			throw new NullPointerException("The input point match result list is empty.");
		
		IOService.createFolder(fileFolder);
		IOService.cleanFolder(fileFolder);
		
		File folder = new File(fileFolder);
		Stream<Pair<Integer, List<PointMatch>>> trajectoryMatchingStream = pointMatchResultList.stream();
		
		// parallel processing
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		forkJoinPool.submit(() -> trajectoryMatchingStream.parallel().forEach(x -> {
			List<String> output = new ArrayList<>();
			for (PointMatch pointMatch : x._2()) {
				output.add(pointMatch.toString());
			}
			IOService.writeFile(output, fileFolder, "pointmatch_" + x._1() + ".txt");
		}));
		while (Objects.requireNonNull(folder.list()).length != pointMatchResultList.size()) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		LOG.debug("Point matching results written, total file count: " + pointMatchResultList.size());
	}
	
	/**
	 * Write route match results. Each line of the result consists of a road ID.
	 *
	 * @param routeMatchResultList The matching results.
	 * @param fileFolder           The output folder path.
	 */
	public static void writeRouteMatchResults(List<Pair<Integer, List<String>>> routeMatchResultList, String fileFolder) {
		if (routeMatchResultList == null)
			throw new NullPointerException("The input point match result list is empty.");
		
		IOService.createFolder(fileFolder);
		IOService.cleanFolder(fileFolder);
		
		File folder = new File(fileFolder);
		Stream<Pair<Integer, List<String>>> trajectoryMatchingStream = routeMatchResultList.stream();
		
		// parallel processing
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		forkJoinPool.submit(() -> trajectoryMatchingStream.parallel().forEach(x -> IOService.writeFile(x._2(), fileFolder,
				"routematch_" + x._1() + ".txt")));
		while (Objects.requireNonNull(folder.list()).length != routeMatchResultList.size()) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		LOG.debug("Point matching results written, total file count: " + routeMatchResultList.size());
	}
	
	/**
	 * Merge the matching results generated before and after the result update and write new map-matching results to the file. Only used
	 * in map update.
	 *
	 * @param oldMatchResults The original map-matching result before result update.
	 * @param newMatchResults The map-matching results affected by the result update.
	 * @param fileFolder      The output file folder.
	 * @return The merged map-matching results.
	 */
	public static List<MultipleTrajectoryMatchResult> writeAndMergeMatchResults(List<MultipleTrajectoryMatchResult> oldMatchResults,
																				List<MultipleTrajectoryMatchResult> newMatchResults, String fileFolder) {
		Set<String> newMatchResultIDList = new HashSet<>();
		for (MultipleTrajectoryMatchResult mr : newMatchResults)
			newMatchResultIDList.add(mr.getTrajID());
		oldMatchResults.removeIf(next -> newMatchResultIDList.contains(next.getTrajID()));
		oldMatchResults.addAll(newMatchResults);
		writeMultipleMatchResults(oldMatchResults, fileFolder);
		return oldMatchResults;
	}
}
