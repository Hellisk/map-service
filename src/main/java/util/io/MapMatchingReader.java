package util.io;

import util.object.roadnetwork.RoadNetworkGraph;
import util.object.spatialobject.Trajectory;
import util.object.structure.MultipleTrajectoryMatchResult;
import util.settings.BaseProperty;

import java.util.List;

/**
 * Reader abstract class for map-matching applications which support reading input trajectories, underlying map(s) and ground-truth
 * map-matching results from raw data.
 *
 * @author Hellisk
 * @since 30/03/2019
 */
public interface MapMatchingReader {
	
	/**
	 * The main function for parsing input trajectory, map and ground-truth result. The parsed results are stored within the reader and
	 * retrieved by different read operations.
	 *
	 * @param baseFolderName The root path of the current dataset.
	 * @param prop           The list of parameters.
	 */
	void inputParser(String baseFolderName, BaseProperty prop);
	
	List<Trajectory> getTrajectoryList();
	
	List<MultipleTrajectoryMatchResult> getGroundTruthMatchResult();
	
	RoadNetworkGraph getMap();
}
