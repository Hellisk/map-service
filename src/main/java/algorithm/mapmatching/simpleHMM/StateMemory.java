package algorithm.mapmatching.simpleHMM;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages StateCandidates at a same timestamp
 */
public class StateMemory {
    private Map<StateCandidate, Integer> stateCandidateVotes = new HashMap<>();

    public int getCandidateVote(StateCandidate candidate) {
        return stateCandidateVotes.get(candidate);
    }

    public void modifyCandidateVote(StateCandidate candidate) {
        stateCandidateVotes.put(candidate, 0);
    }

    public void modifyCandidateVote(StateCandidate candidate, int newVote) {
        stateCandidateVotes.put(candidate, newVote);
    }

    public Set<StateCandidate> getStateCandidates() {
        return stateCandidateVotes.keySet();
    }

    public void addStateCandidate(StateCandidate candidate, int vote) {
        stateCandidateVotes.put(candidate, vote);
    }

    public void removeStateCandidate(StateCandidate candidate) {
        stateCandidateVotes.remove(candidate);
    }

    public int size() {
        return stateCandidateVotes.size();
    }

}
