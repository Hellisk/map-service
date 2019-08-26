package algorithm.mapmatching.simpleHMM;


import java.util.HashSet;
import java.util.Set;

/**
 * Manages StateCandidates at a same timestamp
 */
public class StateMemory {
    private Set<StateCandidate> stateCandidates = new HashSet<>();
    private StateSample sample;

    public StateMemory(StateSample sample) {
        this.sample = sample;
    }

    public StateMemory(Set<StateCandidate> stateCandidates, StateSample sample) {
        this.stateCandidates = stateCandidates;
        this.sample = sample;
    }

    public Set<StateCandidate> getStateCandidates() {
        return this.stateCandidates;
    }

    public StateSample getSample() {
        return this.sample;
    }

    public void addCandidate(StateCandidate candidate) {
        this.stateCandidates.add(candidate);
    }

}
