package algorithm.mapmatching.simpleHMM;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages StateCandidates at a same timestamp
 */
public class StateMemory {
    private Map<String, StateCandidate> stateCandidates = new HashMap<>();
    private StateSample sample;
    private StateCandidate filtProbCandidate = null; // the candidate with the largest filter probability
    private String id;
    public StateMemory(Set<StateCandidate> stateCandidates, StateSample sample) {
        for (StateCandidate stateCandidate : stateCandidates) {
            this.stateCandidates.put(stateCandidate.getId(), stateCandidate);
        }
        this.sample = sample;
        this.id = Double.toString(sample.getTime());
        for (StateCandidate candidate : stateCandidates) {
            if (filtProbCandidate == null || candidate.getFiltProb() < candidate.getFiltProb()) {
                filtProbCandidate = candidate;
            }
        }
    }

    public Map<String, StateCandidate> getStateCandidates() {
        return this.stateCandidates;
    }

    public StateSample getSample() {
        return this.sample;
    }

    public StateCandidate getFiltProbCandidate() {
        return filtProbCandidate;
    }

    public String getId() {
        return id;
    }
}
