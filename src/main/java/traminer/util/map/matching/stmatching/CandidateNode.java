package traminer.util.map.matching.stmatching;

import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.st.STPoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ST-Matching auxiliar road node object
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
class CandidateNode implements Serializable {
    private double lon, lat;
    private String streetName, wayID;
    private STPoint respondingGPSFix;
    private double maxSpeed;
    private double distanceToRespondingGPSFix;
    private double observationProbability;

    //contains all candidate nodes responding to the previous GPS Fix,
    //however, the actual transmission probability is computed only for the
    //most probable previous match
    private List<CandidateNode> pastNodesList =
            new ArrayList<CandidateNode>();
    private List<Double> transmissionProbabilities =
            new ArrayList<Double>();

    //The spatial analysis function results (in regard to one or more previously
    //obtained candidate nodes) are saved here
    private List<Double> spatialAnalysisFunctionResults =
            new ArrayList<Double>();
    private List<Double> temporalAnalysisFunctionResults =
            new ArrayList<Double>();

    public boolean startCandidate = false;
    public boolean endCandidate = false;
    public boolean connected = false;
    public boolean bestMatch = false;

    // the distance function to be used in this node
    private PointDistanceFunction distCalc;

    public CandidateNode(
            double latitude, double longitude,
            STPoint parentGPSFix, String name, String way,
            int maxSpeed, PointDistanceFunction distCalc) {
        this.lat = latitude;
        this.lon = longitude;
        this.respondingGPSFix = parentGPSFix;
        this.streetName = name;
        this.wayID = way;
        this.distCalc = distCalc;
        this.distanceToRespondingGPSFix = distCalc.pointToPointDistance(
                longitude, latitude, parentGPSFix.x(), parentGPSFix.y());
        this.maxSpeed = maxSpeed;
    }

    public boolean equals(CandidateNode nodeToCompare) {
        if (this.lat == nodeToCompare.lat &&
                this.lon == nodeToCompare.lon) {
            if (this.respondingGPSFix.equals(nodeToCompare.respondingGPSFix))
                return true;
        }
        return false;
    }

    public STPoint getParentFix() {
        return this.respondingGPSFix;
    }

    public String getStreetName() {
        return streetName;
    }

    public String getWayName() {
        return this.wayID;
    }

    public double getDistanceToGPSFix() {
        return this.distanceToRespondingGPSFix;
    }

    public void setStartOrEndNode(String input) {
        if (input.equals("start"))
            startCandidate = true;
        else endCandidate = true;
    }

    public void setObservationProbability(double probability) {
        this.observationProbability = probability;
    }

    public double getObservationProbability() {
        return this.observationProbability;
    }

    public long getTimestamp() {
        return this.respondingGPSFix.time();
    }

    public double getMaxSpeed() {
        return this.maxSpeed;
    }

    public void setTransmissionProbability(
            CandidateNode pastCandidate, double transmissionProbability) {
        this.pastNodesList.add(pastCandidate);
        this.transmissionProbabilities.add(transmissionProbability);
        this.spatialAnalysisFunctionResults.add(transmissionProbability * observationProbability);
    }

    public List<Double> getSpatialAnalysisFunctionResults() {
        return spatialAnalysisFunctionResults;
    }

    public void setTemporalAnalysisResults(double temporalAnalysisResult) {
        this.temporalAnalysisFunctionResults.add(temporalAnalysisResult);
    }

    public double distanceTo(STPoint p) {
        return distCalc.pointToPointDistance(lon, lat, p.x(), p.y());
    }
}
