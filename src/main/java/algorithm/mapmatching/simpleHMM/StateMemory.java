package algorithm.mapmatching.simpleHMM;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages StateCandidates at a same timestamp
 */
public class StateMemory {
    //    private Set<StateCandidate> stateCandidates = new HashSet<>();
    private Map<String, StateCandidate> stateCandidates = new HashMap<>();
    private StateSample sample;

    public StateMemory(StateSample sample) {
        this.sample = sample;
    }

    public StateMemory(Set<StateCandidate> stateCandidates, StateSample sample) {
//        this.stateCandidates = stateCandidates;
        for (StateCandidate stateCandidate : stateCandidates) {
            this.stateCandidates.put(stateCandidate.getId(), stateCandidate);
        }
        this.sample = sample;
    }

    public Map<String, StateCandidate> getStateCandidates() {
        return this.stateCandidates;
    }

    public StateSample getSample() {
        return this.sample;
    }

    public void addCandidate(StateCandidate candidate) {
        this.stateCandidates.put(candidate.getId(), candidate);
    }

}
