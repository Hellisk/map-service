package algorithm.mapmatching.simpleHMM;

import util.object.structure.PointMatch;

public class StateCandidate {

    private StateSample stateSample;
    private StateCandidate predecessor = null;
    private double filtProb = 0d;
    private double seqProb = 0d;
    private PointMatch self;
    private StateTransition transition = new StateTransition();
    private double emiProb = 0d;

    public StateCandidate(PointMatch self, StateSample stateSample) {
        this.self = self;
        this.stateSample = stateSample;
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

    public double getSeqProb() {
        return seqProb;
    }

    public void setSeqProb(double seqProb) {
        this.seqProb = seqProb;
    }

    public StateSample getStateSample() {
        return stateSample;
    }

    public PointMatch getGeometry() {
        return self;
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
        return self.lon();
    }

    public double lat() {
        return self.lat();
    }
}
