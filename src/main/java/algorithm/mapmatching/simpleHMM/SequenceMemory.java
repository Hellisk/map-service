package algorithm.mapmatching.simpleHMM;

import util.object.structure.Pair;

import java.util.*;

/**
 * Organize StateMemories
 * 1. Deletes StateCandidates with 0 vote in last StateMemory
 * 2. Expand forward the sequence
 * 2. Manage size of sliding window
 * 3. Derive the optimal route to current SampleMeasurement
 */
public class SequenceMemory extends StateMemory {
    private Map<StateCandidate, Integer> sequenceCandidateVotes = new HashMap<>();
    private LinkedList<Pair<StateMemory, StateSample>> stateMemoryVector = new LinkedList<>();
    private int maxStateNum = -1;
    private long maxWaitingTime = -1;

    public SequenceMemory() {
    }

    public SequenceMemory(int maxStateNum, long maxWaitingTime) {
        this.maxStateNum = maxStateNum;
        this.maxWaitingTime = maxWaitingTime;

    }

    /**
     * Return the most likely candidate at last timestamp
     *
     * @return candidate
     */
    public StateCandidate optimalPredecessor() {
        if (stateMemoryVector.isEmpty()) {
            return null;
        }

        StateCandidate estimate = null;

        StateMemory lastState = stateMemoryVector.getLast()._1();
        if (lastState == null) return null;

        for (StateCandidate candidate : lastState.getStateCandidates()) {
            if (estimate == null || estimate.getFiltProb() < candidate.getFiltProb()) {
                estimate = candidate;
            }
        }
        return estimate;
    }

    /**
     * Once a candidate needs to be removed, updates votes of its ancestors
     *
     * @param candidate the candidate to be removed
     * @param timeStamp timestamp of this candidate (start from 0)
     */
    private void remove(StateCandidate candidate, int timeStamp) {
        while (timeStamp >= 0) {
            Set<StateCandidate> candidateSet = stateMemoryVector.get(timeStamp)._1().getStateCandidates();
            sequenceCandidateVotes.remove(candidate);
            candidateSet.remove(candidate);

            StateCandidate predecessor = candidate.getPredecessor();
            sequenceCandidateVotes.put(predecessor, sequenceCandidateVotes.get(predecessor) - 1);

            if (sequenceCandidateVotes.get(predecessor) == 0) {
                candidate = predecessor;
                timeStamp -= 1;
            } else return;
        }
    }

    /**
     * Delete redundant candidates (whose votes are 0) in sequence and map
     */
    private void deleteCandidates() {
        if (stateMemoryVector.isEmpty()) return;

        StateMemory lastState = stateMemoryVector.peekLast()._1();

        Set<StateCandidate> deletes = new HashSet<>();

        for (StateCandidate candidate : lastState.getStateCandidates()) {
            if (lastState.getCandidateVote(candidate) == 0) deletes.add(candidate);
        }

        for (StateCandidate delete : deletes) {
            remove(delete, stateMemoryVector.size() - 1);
        }

    }

    /**
     * The window expands forward as new trajectory points are processed
     */
    private void expand(Set<StateCandidate> latestCandidates, StateSample lastSample) {
        if (latestCandidates.isEmpty()) return;
        if (stateMemoryVector.isEmpty()
                || stateMemoryVector.peekLast()._2().getSampleTime() > lastSample.getSampleTime()) {
            throw new RuntimeException("inconsistent time sequence");
        }

        StateCandidate estimate = optimalPredecessor();

        for (StateCandidate latestCandidate : latestCandidates) {
            sequenceCandidateVotes.put(latestCandidate, 0);
            if (latestCandidate.getPredecessor() == null) latestCandidate.setPredecessor(estimate);

            // latest candidate must vote to a predecessor that is a candidate in previous timestamp
            if (latestCandidate.getPredecessor() != null) {
                if (!sequenceCandidateVotes.containsKey(latestCandidate.getPredecessor()) ||
                        !stateMemoryVector.peekLast()._1().getStateCandidates().contains(latestCandidate.getPredecessor())) {
                    throw new RuntimeException("inconsistent update vector");
                }
            }

            // update votes of predecessors
            sequenceCandidateVotes.put(latestCandidate.getPredecessor(),
                    sequenceCandidateVotes.get(latestCandidate.getPredecessor()) + 1);
        }
    }

    /**
     * The window expands forward as new trajectory points are processed and shrinks from behind when
     * a convergence point is found anywhere in the Markov chain covered by the window.
     */
    private void manageWindSize() {

    }
}