package traminer.util.map.matching.stmatching;

/**
 * ST-Matching off-line map matching algorithm.
 * <p>
 * Map matching algorithm proposed in the paper:
 * <br> Lou, Yin, et al. "Map-Matching for low-sampling-rate
 * GPS trajectories." ACM SIGSPATIAL, 2009.
 *
 * @author uqdalves
 */
public class STMatching {// TODO
    private double matchRadius;
/*
    static ArrayList<CandidateNode> relevantNodesList;
	static ArrayList<ArrayList<CandidateNode>> previousNodesList = new ArrayList<ArrayList<CandidateNode>>();
	static double lastBearing = 0, newBearing = 0, outlierBearing = 0, bearingDifference = 0;
	static boolean outlierDetected = false;
	static Boolean initPoint = true;

	//Mean and standard deviation in meters
	static double mu = 5;
	static double sigma = 10;
	static String DEBUG = "Bearing";

	//Likelihood that the raw fix should be mapped to the candidate in question, without considering the neighboring points
	public static void assignObservationProbability(){
		for(CandidateNode e:relevantNodesList){
			double probability = Distributions.normalDistribution(sigma, e.getDistanceToGPSFix()-mu);
			e.setObservationProbability(probability);
		}

		CandidateNode observationBestMatch = null, observationSecondBestMatch;

		observationBestMatch = relevantNodesList.get(0);
		observationSecondBestMatch = relevantNodesList.get(0);

		for(CandidateNode e:relevantNodesList){
			if(e.getObservationProbability()>observationBestMatch.getObservationProbability()){
				observationSecondBestMatch = observationBestMatch;
				observationBestMatch = e;
			}
			else if(!e.equals(observationBestMatch) && (observationBestMatch.equals(observationSecondBestMatch) || e.getObservationProbability()>observationSecondBestMatch.getObservationProbability()))
				observationSecondBestMatch = e;
		}

		pointWeighting(observationBestMatch, observationSecondBestMatch);

		previousNodesList.add(relevantNodesList);
	}

	public static void assignTransmissionProbability(){
		if(previousNodesList.size()>1){
			//For all the candidate nodes from the previous fix compute the likelihood that the 
			//“true” path from GPS_FIX_i-1 to GPS_FIX_i follows the shortest path from candidate node e to candidate node f
			double highestSpatialResult = 0;
			CandidateNode highestSpatialNode = null;
			int highestSpatialIndex = 0;

			for(CandidateNode f : previousNodesList.get(previousNodesList.size()-1)){
				for(CandidateNode e:previousNodesList.get(previousNodesList.size()-2)){
					//Compute the transmission probability only for the edges connecting the previous correct match and 
					//the new candidates for current observation
					if(e.bestMatch == true){
						//Distance between the GPS fixes
						double distanceBetweenRawPoints = 
								e.getParentFix().distance(f.getParentFix());

						Point locationOfThePreviousPoint = new Point(e.lon, e.lat);
						Point locationOfTheNextPoint = new Point(f.lon, f.lat);

						//Distance and approximate speed between the two candidate nodes
						double distanceBetweenTheCandidateNodes = 
								locationOfThePreviousPoint.distance(locationOfTheNextPoint);
						double meanSpeed = distanceBetweenTheCandidateNodes/(f.getTimestamp()-e.getTimestamp());

						//Computing transmission probability
						double transmissionProbability = 
								(distanceBetweenRawPoints / distanceBetweenTheCandidateNodes);
						f.setTransmissionProbability(e, transmissionProbability);

						//Always equals 1 because we're moving on one segment max,
						//between the measurements - TO IMPLEMENT moving on multiple segments (sums, etc.)
						double temporalAnalysisFunctionResult = 
								(f.getMaxSpeed()*meanSpeed)/(f.getMaxSpeed()*meanSpeed);
						f.setTemporalAnalysisResults(temporalAnalysisFunctionResult);
					}
				}

				//Determine the candidate node with the overall highest spatial/temporal function score
				for(double e : f.getSpatialAnalysisFunctionResults()){
					if(e > highestSpatialResult){
						highestSpatialResult = e;
						highestSpatialNode = f;
						highestSpatialIndex = f.spatialAnalysisFunctionResults.indexOf(e);
					}
				}
			} 

			//Eventually, consider this node the best possible match for the current observation, and use to connect 
			//to it during the succeding observation matching process, also draw it on the map.
			highestSpatialNode.bestMatch = true;
			previousNodesList.get(previousNodesList.size()-1).add(highestSpatialNode);
			OverlayMapViewer.setCandidatePoint(highestSpatialNode);

			//Build the GPS trajectory
			OverlayMapViewer.buildWaySegment(highestSpatialNode.pastNodesList.get(highestSpatialIndex).getParentFix(), highestSpatialNode.getParentFix(), Color.RED);

			//Build the first edge after the second matching process, compute the initial bearing
			if(initPoint){
				OverlayMapViewer.buildRoadSegment(highestSpatialNode.pastNodesList.get(highestSpatialIndex), highestSpatialNode, Color.BLUE);
				lastBearing =  highestSpatialNode.pastNodesList.get(highestSpatialIndex).getLocation().bearingTo(highestSpatialNode.getLocation());
				newBearing = lastBearing;
				Log.d(DEBUG, "Bearing: Initial point");
			}

			Log.d(DEBUG, "Last bearing: " + lastBearing);

			//Compute the succeeding bearing...
			if(!initPoint){
				if(!((highestSpatialNode.pastNodesList.get(highestSpatialIndex).getLatitude()==highestSpatialNode.getLatitude())
						&& (highestSpatialNode.pastNodesList.get(highestSpatialIndex).getLatitude()==highestSpatialNode.getLatitude())))
					newBearing = highestSpatialNode.pastNodesList.get(highestSpatialIndex).getLocation().bearingTo(highestSpatialNode.getLocation());
				else newBearing = lastBearing;
				Log.d(DEBUG, "New Bearing:" + newBearing);
			}

			//, absolute value of the difference between the bearings...
			bearingDifference = Math.abs(Math.abs(newBearing) - Math.abs(lastBearing));
			if(outlierDetected == false)
				Log.d(DEBUG, " Bearing difference: " + bearingDifference);

			initPoint = false;

			//... and check for the outliers.
			if(!initPoint){
				if(bearingDifference<35 && outlierDetected==false){
					lastBearing = newBearing;
					OverlayMapViewer.buildRoadSegment(highestSpatialNode.pastNodesList.get(highestSpatialIndex), highestSpatialNode, Color.BLUE);
				}
				else if(outlierDetected==true){
					//OverlayMapViewer.buildRoadSegment(highestSpatialNode.pastNodesList.get(highestSpatialIndex), highestSpatialNode, Color.BLUE);
					//lastBearing = newBearing;
					Log.d(DEBUG, "Newest bearing: " + newBearing);
					Log.d(DEBUG, "The passed outlier candidate bearing: " + outlierBearing);

					bearingDifference = Math.abs(Math.abs(newBearing) - Math.abs(outlierBearing));
					if(bearingDifference<35){
						//asumption that the candidate node was an outlier proven FALSE
						lastBearing = newBearing;
						OverlayMapViewer.buildRoadSegment(highestSpatialNode.pastNodesList.get(highestSpatialIndex), highestSpatialNode, Color.BLUE);
						outlierDetected = false;
					}
					else{ 
						//asumption that the candidate node was an outlier proven TRUE - continue using the bearing from the correct
						//matches, 2 ticks before
						outlierDetected = false;
						//OverlayMapViewer.buildRoadSegment(highestSpatialNode.pastNodesList.get(highestSpatialIndex), highestSpatialNode, Color.BLUE);
						//initPoint = true
					}
				}
				else if(bearingDifference>35 && highestSpatialNode.pastNodesList.get(highestSpatialIndex).getWayName()!= highestSpatialNode.getWayName()){
					//OverlayMapViewer.buildRoadSegment(highestSpatialNode.pastNodesList.get(highestSpatialIndex), highestSpatialNode, Color.RED);
					//lastBearing = newBearing;
					outlierDetected = true;
					outlierBearing =  highestSpatialNode.pastNodesList.get(highestSpatialIndex).getLocation().bearingTo(highestSpatialNode.getLocation());
					//OverlayMapViewer.buildRoadSegment(highestSpatialNode.pastNodesList.get(highestSpatialIndex), highestSpatialNode, Color.BLUE);
					Log.d(DEBUG, "Bearing: Outlier detected! Outlier bearing: " + outlierBearing);
				}
			}
		}}

	public static void updateRelevantNodesList(ArrayList<CandidateNode> list){
		relevantNodesList = list;
		Log.d(DEBUG, "Candidate Nodes list size: " + relevantNodesList.size());
	}

	public static void assignObservationProbability(CandidateNode node){
		double probability = (1/(Math.sqrt(6.28)*sigma)) * Math.exp (-((Math.pow((node.getDistanceToGPSFix()-mu), 2))/(2*Math.pow(sigma,2))));
		node.setObservationProbability(probability);
	}

	public static void pointWeighting(CandidateNode observationBestMatch, CandidateNode observationSecondBestMatch){
		if(observationBestMatch.getWayName().equals(observationSecondBestMatch.getWayName()))
		{
			Location locationBestMatch = observationBestMatch.getLocation();
			Location locationSecondMatch = observationSecondBestMatch.getLocation();

			double distanceBetween = locationSecondMatch.distanceTo(locationBestMatch);

			double ratio = (observationSecondBestMatch.getObservationProbability()/observationBestMatch.getObservationProbability())/2.0;
			double theTrueDistance = distanceBetween*ratio;

			double dist = (theTrueDistance/1000.0)/6371.0;
			double lat1 = Math.toRadians(observationBestMatch.getLatitude());
			double lon1 = Math.toRadians(observationBestMatch.getLongitude());
			double bearing = Math.toRadians(locationBestMatch.bearingTo(locationSecondMatch));

			double lat2 = Math.asin( Math.sin(lat1)*Math.cos(dist) + Math.cos(lat1)*Math.sin(dist)*Math.cos(bearing) );
			double a = Math.atan2(Math.sin(bearing)*Math.sin(dist)*Math.cos(lat1), Math.cos(dist)-Math.sin(lat1)*Math.sin(lat2));
			System.out.println("a = " +  a);
			double lon2 = lon1 + a;

			lon2 = (lon2+ 3*Math.PI) % (2*Math.PI) - Math.PI;

			Log.d(DEBUG, "Latitude = "+Math.toDegrees(lat2)+"\nLongitude = "+Math.toDegrees(lon2));
			double matchedLatitude = Math.toDegrees(lat2);
			double matchedLongitude = Math.toDegrees(lon2);

			CandidateNode geoMatchedCandidate= new CandidateNode(matchedLatitude, matchedLongitude, 
					observationBestMatch.getParentFix(), observationBestMatch.getStreetName(), 
					observationBestMatch.getWayName(), observationBestMatch.getMaxSpeed(), observationBestMatch.getTimestamp());

			assignObservationProbability(geoMatchedCandidate);

			if(previousNodesList.size()==0){
				geoMatchedCandidate.bestMatch = true;
				OverlayMapViewer.setCandidatePoint(geoMatchedCandidate);}

			relevantNodesList.add(geoMatchedCandidate);
		}
		else if(previousNodesList.size()==0){
			observationBestMatch.bestMatch = true;
			OverlayMapViewer.setCandidatePoint(observationBestMatch);
		}
	}
	*/
}
