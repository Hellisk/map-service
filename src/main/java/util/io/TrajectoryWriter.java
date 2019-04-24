package util.io;

import org.apache.log4j.Logger;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

/**
 * Write trajectories to files in the given folder. Only used in writing unmatched trajectories.
 *
 * @author Hellisk
 * @since 23/05/2017
 */
public class TrajectoryWriter {
	
	private static final Logger LOG = Logger.getLogger(TrajectoryWriter.class);
	
	/**
	 * Write trajectories to files
	 *
	 * @param trajectoryList List of trajectories.
	 * @param fileFolder     The folder for output files.
	 */
	public static void writeTrajectories(List<Trajectory> trajectoryList, String fileFolder) {
		IOService.createFolder(fileFolder);
		IOService.cleanFolder(fileFolder);
		File folder = new File(fileFolder);
		Stream<Trajectory> trajectoryStream = trajectoryList.stream();
		
		// parallel processing
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		forkJoinPool.submit(() -> trajectoryStream.parallel().forEach(x -> {
			Iterator<TrajectoryPoint> iter = x.iterator();
			List<String> lines = new ArrayList<>();
			while (iter.hasNext()) {
				TrajectoryPoint p = iter.next();
				lines.add(p.toString());
			}
			IOService.writeFile(lines, fileFolder, "trip_" + x.getID() + ".txt");
		}));
		while (Objects.requireNonNull(folder.list()).length != trajectoryList.size()) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		LOG.debug("Trajectories written, total files: " + trajectoryList.size());
	}
	
	/**
	 * Write unmatched trajectories to files
	 *
	 * @param trajectoryList  List of unmatched trajectories.
	 * @param fileFolder      The folder for output files.
	 * @param nextInputFolder The folder for Biagioni KDE input.
	 */
	public static void writeUnmatchedTrajectories(List<Trajectory> trajectoryList, String fileFolder, String nextInputFolder) {
//		if (iteration == -1) {
//			outputTrajectoryFolder = new File(this.outputFolder + "unmatchedTraj/");
//			nextInputUnmatchedTrajectoryFolder = new File(this.outputFolder + "unmatchedNextInput/");
//		} else {
//			outputTrajectoryFolder = new File(this.outputFolder + "unmatchedTraj/TP" + MIN_TRAJ_TIME_SPAN + "_TI" +
//					MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/" + iteration + "/");
//			nextInputUnmatchedTrajectoryFolder = new File(this.outputFolder + "unmatchedNextInput/TP" + MIN_TRAJ_TIME_SPAN + "_TI" +
//					MAX_TIME_INTERVAL + "_TC" + TRAJECTORY_COUNT + "/" + iteration + "/");
//		}
		int tripCount = 0;
		int pointCount = 1;
		IOService.createFolder(fileFolder);
		IOService.cleanFolder(fileFolder);
		if (!nextInputFolder.equals("")) {
			IOService.createFolder(nextInputFolder);
			IOService.cleanFolder(nextInputFolder);
		}
		for (Trajectory w : trajectoryList) {
			String fileName = "/trip_" + w.getID() + ".txt";
			Iterator<TrajectoryPoint> iter = w.iterator();
			int startPointCount = pointCount;
			int matchingPointCount = w.getSTPoints().size();
			List<String> lines = new ArrayList<>();
			List<String> nextInputLines = new ArrayList<>();    // only used for Biagioni KDE input. Will be removed eventually
			while (iter.hasNext()) {
				TrajectoryPoint p = iter.next();
				lines.add(p.toString());
				nextInputLines.add(pointCount + "," + p.y() + "," + p.x() + "," + p.time() + "," + (pointCount == startPointCount ?
						"None" : pointCount - 1) + "," + (pointCount != startPointCount + matchingPointCount - 1 ? pointCount + 1 : "None") + "\n");
				pointCount++;
			}
			IOService.writeFile(lines, fileFolder, fileName);
			if (!nextInputFolder.equals(""))
				IOService.writeFile(nextInputLines, nextInputFolder, fileName);
			tripCount++;
		}
		LOG.debug("Unmatched trajectories written, total files:" + tripCount + ", total trajectory points:" + (pointCount - 1));
	}
}
