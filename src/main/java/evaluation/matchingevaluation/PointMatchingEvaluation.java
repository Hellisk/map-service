package evaluation.matchingevaluation;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.structure.Pair;
import util.object.structure.PointMatch;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The evaluation session used for measuring the accuracy of point matching result. Each point match result is represented by a list of
 * <tt>PointMatch</tt> presenting the match point of each trajectory point. The size of the PointMatch list should be equivalent to the
 * size of each trajectory.
 *
 * @author Hellisk
 * @since 10/07/2017
 */
public class PointMatchingEvaluation {
	
	private static final Logger LOG = Logger.getLogger(PointMatchingEvaluation.class);
	
	/**
	 * Evaluate the precision, recall and F-score of the point matching results.
	 *
	 * @param matchedResultList The output point match result list.
	 * @param gtResultList      The ground-truth point match result list.
	 * @return Accuracy.
	 */
	public static String accuracyEvaluation(List<Pair<Integer, List<PointMatch>>> matchedResultList, List<Pair<Integer,
			List<PointMatch>>> gtResultList) {
		DecimalFormat df = new DecimalFormat("0.000");
		if (matchedResultList.size() != gtResultList.size())
			throw new IllegalArgumentException("The size of the output point match list is different from the ground-truth: "
					+ matchedResultList.size() + "," + gtResultList.size() + ".");
		if (matchedResultList.size() == 0)
			throw new IllegalArgumentException("The output point match list is empty.");
		DistanceFunction distFunc = matchedResultList.get(0)._2().get(0).getDistanceFunction();
		
		// insert all ground truth road match into trajID2GTResultMapping
		Map<Integer, List<PointMatch>> trajID2GTResultMapping = new HashMap<>();
		for (Pair<Integer, List<PointMatch>> gtResult : gtResultList) {
			trajID2GTResultMapping.put(gtResult._1(), gtResult._2());
		}
		// start the count
		
		double totalPointCount = 0;    // total count of the trajectory points.
		double totalCorrectlyMatchedCount = 0;      // total count of perfectly matched points.
		for (Pair<Integer, List<PointMatch>> r : matchedResultList) {
			if (r._2().size() == 0)
				throw new IllegalArgumentException("The current point match result is empty: " + r._1());
			List<PointMatch> gtPointMatchList = trajID2GTResultMapping.get(r._1());
			if (r._2().size() != gtPointMatchList.size())
				throw new IllegalArgumentException("The output point match result has different size as the ground-truth: " + r._2().size() +
						"," + gtPointMatchList.size() + ".");
			double currCorrectlyMatchedCount = 0;
			
			for (int i = 0; i < r._2().size(); i++) {
				PointMatch currOutputMatch = r._2().get(i);
				PointMatch currGTMatch = gtPointMatchList.get(i);
				if (distFunc.distance(currOutputMatch.getMatchPoint(), currGTMatch.getMatchPoint()) == 0) {
					totalCorrectlyMatchedCount++;
					currCorrectlyMatchedCount++;
				}
			}
			
			if (matchedResultList.size() != 1) {
				double currCountAccuracy = currCorrectlyMatchedCount / r._2().size();
				String currCountAccuracyString = convertPercentage(currCountAccuracy, df);
				LOG.debug("Current map-matching result evaluated, count accuracy: " + currCountAccuracyString + "%.");
				totalPointCount += r._2().size();
			}
		}
		
		double CountAccuracy = totalCorrectlyMatchedCount / totalPointCount;
		String CountAccuracyString = convertPercentage(CountAccuracy, df);
		LOG.info("Map-matching result evaluated, count accuracy: " + CountAccuracyString + "%.");
		return CountAccuracyString;
	}
	
	/**
	 * Root Mean Square Error (RMSE) evaluates the normalised distance between each match point to its ground-truth, it is proposed in:
	 * <p>
	 * Singh, J., Singh, S., Singh, S., Singh, H.: Evaluating the performance of map matching algorithms for navigation systems: an
	 * empirical study. Spatial Information Research pp. 1{12 (2018)
	 *
	 * @param matchedResultList Output point match result list.
	 * @param gtResultList      Ground-truth point match result list.
	 * @return Evaluation result.
	 */
	public static String rootMeanSquareErrorEvaluation(List<Pair<Integer, List<PointMatch>>> matchedResultList,
													   List<Pair<Integer, List<PointMatch>>> gtResultList) {
		if (matchedResultList.size() != gtResultList.size())
			throw new IllegalArgumentException("The size of the output point match list is different from the ground-truth: "
					+ matchedResultList.size() + "," + gtResultList.size() + ".");
		if (matchedResultList.size() == 0)
			throw new IllegalArgumentException("The output point match list is empty.");
		DistanceFunction distFunc = matchedResultList.get(0)._2().get(0).getDistanceFunction();
		// insert all ground truth road match into trajID2GTResultMapping
		Map<Integer, List<PointMatch>> trajID2GTResultMapping = new HashMap<>();
		for (Pair<Integer, List<PointMatch>> gtResult : gtResultList) {
			trajID2GTResultMapping.put(gtResult._1(), gtResult._2());
		}
		
		double totalRMSE = 0;    // total Count of the incorrect output roads
		for (Pair<Integer, List<PointMatch>> r : matchedResultList) {
			if (r._2().size() == 0)
				throw new IllegalArgumentException("The current point match result is empty: " + r._1());
			double currRMSE = 0;
			List<PointMatch> gtPointMatchList = trajID2GTResultMapping.get(r._1());
			if (r._2().size() != gtPointMatchList.size())
				throw new IllegalArgumentException("The output point match result has different size as the ground-truth: " + r._2().size() +
						"," + gtPointMatchList.size() + ".");
			for (int i = 0; i < r._2().size(); i++) {
				PointMatch currOutputMatch = r._2().get(i);
				PointMatch currGTMatch = gtPointMatchList.get(i);
				currRMSE += Math.pow(distFunc.distance(currOutputMatch.getMatchPoint(), currGTMatch.getMatchPoint()), 2);
			}
			currRMSE = Math.sqrt(currRMSE / r._2().size());
			totalRMSE += currRMSE;
		}
		totalRMSE = totalRMSE / matchedResultList.size();
		LOG.info("Map-matching result evaluated, the average Root Mean Square Error (RMSE): " + totalRMSE + ".");
		return totalRMSE + "";
	}
	
	/**
	 * Convert the current decimal into percentage, =0 if the current decimal is not a number (already divided by zero)
	 *
	 * @param decimal Input decimal.
	 * @param df      Decimal format.
	 * @return The number * 100.
	 */
	private static String convertPercentage(double decimal, DecimalFormat df) {
		return df.format(Double.isNaN(decimal) ? 0 : decimal * 100);
	}
}