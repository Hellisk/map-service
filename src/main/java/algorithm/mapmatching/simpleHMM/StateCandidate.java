package algorithm.mapmatching.simpleHMM;

import util.object.structure.PointMatch;

public class StateCandidate {

    private StateSample stateSample;
    private StateCandidate predecessor = null;
    private double filtProb = 0d;
    //    private double seqProb = 0d;
    private PointMatch pointMatch;
    private StateTransition transition = new StateTransition();
    private double emiProb = 0d;
    private String id;

    public StateCandidate() {
        super();
    }

    public StateCandidate(PointMatch self, StateSample stateSample) {
        this.pointMatch = self;
        this.stateSample = stateSample;
        this.id = self.getRoadID() + "_" + stateSample.getTime();
    }

    public StateCandidate getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(StateCandidate predecessor) {
        this.predecessor = predecessor;
    }

    public double getFiltProb() {
        return filtProb;
    }

    public void setFiltProb(double filtProb) {
        this.filtProb = filtProb;
    }

    public StateSample getStateSample() {
        return stateSample;
    }

    public PointMatch getPointMatch() {
        return pointMatch;
    }

    public StateTransition getTransition() {
        return transition;
    }

    public void setTransition(StateTransition transition) {
        this.transition = transition;
    }

    public double getEmiProb() {
        return emiProb;
    }

    public void setEmiProb(double emiProb) {
        this.emiProb = emiProb;
    }

    public double lon() {
        return pointMatch.lon();
    }

    public double lat() {
        return pointMatch.lat();
    }

    public String getId() {
        return id;
    }
}
