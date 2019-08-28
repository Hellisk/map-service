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
public class SequenceMemory {
    private Map<String, Integer> sequenceCandidateVotes = new HashMap<>();
    private LinkedList<Pair<StateMemory, StateSample>> stateMemoryVector = new LinkedList<>();
    private int maxStateNum = -1;
    private long maxWaitingTime = -1;

    public SequenceMemory() {
    }

    public SequenceMemory(int maxStateNum) {
        this.maxStateNum = maxStateNum;
    }

    public SequenceMemory(long maxWaitingTime) {
        this.maxWaitingTime = maxWaitingTime;
    }

    public SequenceMemory(int maxStateNum, long maxWaitingTime) {
        this.maxStateNum = maxStateNum;
        this.maxWaitingTime = maxWaitingTime;

    }

    /**
     * Return the most likely candidate at last state
     *
     * @return candidate
     */
    public StateCandidate optimalPredecessor() {
        if (stateMemoryVector.isEmpty()) {
            return null;
        }

        StateCandidate estimate = null;

        StateMemory lastState = stateMemoryVector.peekLast()._1();
        if (lastState == null) return null;

        for (StateCandidate candidate : lastState.getStateCandidates().values()) {
            if (estimate == null || estimate.getFiltProb() < candidate.getFiltProb()) {
                estimate = candidate;
            }
        }
        return estimate;
    }


    /**
     * The window expands forward as new trajectory points are processed
     */
    private void expand(StateMemory stateMemory, StateSample lastSample) {
        Set<StateCandidate> candidates = new LinkedHashSet<>(stateMemory.getStateCandidates().values());

        if (candidates.isEmpty()) return;
        if (!stateMemoryVector.isEmpty()
                && stateMemoryVector.peekLast()._2().getTime() > lastSample.getTime()) {
            throw new RuntimeException("inconsistent time sequence");
        }

        StateCandidate estimate = optimalPredecessor();

        for (StateCandidate candidate : candidates) {
            // add latest candidates to vote count map
            sequenceCandidateVotes.put(candidate.getId(), 0);

            if (candidate.getPredecessor() == null) candidate.setPredecessor(estimate);

            // predecessors must be candidates in previous state
            if (candidate.getPredecessor() != null) {
                if (!sequenceCandidateVotes.containsKey(candidate.getPredecessor().getId())
                        || !stateMemoryVector.peekLast()._1().getStateCandidates().containsKey(candidate.getPredecessor().getId())) {
                    throw new RuntimeException("inconsistent update vector");
                }
                // update votes of predecessors
                sequenceCandidateVotes.put(candidate.getPredecessor().getId(),
                        sequenceCandidateVotes.get(candidate.getPredecessor().getId()) + 1);

            }
        }
    }

    /**
     * Delete redundant candidates (whose votes are 0 in the chain) in sequence and map
     */
    private void deleteCandidates() {
        if (stateMemoryVector.isEmpty()) return;

        StateMemory lastState = stateMemoryVector.peekLast()._1();

        Set<StateCandidate> deletes = new LinkedHashSet<>();

        for (StateCandidate candidate : lastState.getStateCandidates().values()) {
            if (sequenceCandidateVotes.get(candidate.getId()) == 0) deletes.add(candidate);
        }

        for (StateCandidate delete : deletes) {
            remove(delete, stateMemoryVector.size() - 1);
        }

    }

    /**
     * Once a candidate needs to be removed, updates votes of its ancestors
     *
     * @param candidate the candidate to be removed
     * @param index     state index of this candidate (start from the last index)
     */
    private void remove(StateCandidate candidate, int index) {
        while (index >= 0) {
            sequenceCandidateVotes.remove(candidate.getId());
            stateMemoryVector.get(index)._1().getStateCandidates().remove(candidate.getId());

            StateCandidate predecessor = candidate.getPredecessor();
            if (predecessor == null) {
                return;
            }
            sequenceCandidateVotes.put(predecessor.getId(), sequenceCandidateVotes.get(predecessor.getId()) - 1);

            if (sequenceCandidateVotes.get(predecessor.getId()) == 0) {
                candidate = predecessor;
                index -= 1;
            } else return;
        }
    }


    /**
     * The window shrinks from behind when
     * 1) a convergence point is found anywhere in the Markov chain covered by the window;
     * 2) or reaches the maximum number of states
     * <p>
     * Note: when program calls this method, the sequence has already expanded.
     * i.e. stateMemoryVector.peekLast()._1() returns current state
     */
    private List<StateCandidate> manageWindSize(StateSample latestSample) {
        if (maxStateNum < 0 && maxWaitingTime < 0) return new LinkedList<>();
        if (getStateMemoryVector().size() == 1) return new LinkedList<>(); // just finished initial mm

        StateMemory last = stateMemoryVector.get(stateMemoryVector.size() - 2)._1();
        List<StateMemory> deletes = new LinkedList<>();
        LinkedList<StateCandidate> routeMatchResult = new LinkedList<>();

        if (last.getStateCandidates().size() == 0) {
            throw new RuntimeException("matching break");

        } else if (last.getStateCandidates().size() == 1) {
            // if only one candidate in last state is stored in the chain, this candidate match is known as convergence point

            // pull local path result
            StateCandidate kestimate = optimalPredecessor();
            for (int i = stateMemoryVector.size() - 1; i >= 0; --i) {
                if (kestimate != null) {
                    routeMatchResult.push(kestimate);
                    kestimate = kestimate.getPredecessor();
                }
            }

            // converging state then becomes first state in the sequence
            while (getStateMemoryVector().size() > 1) {
                deletes.add(stateMemoryVector.removeFirst()._1());
            }

        } else if (maxStateNum < stateMemoryVector.size() || (maxWaitingTime >= 0 &&
                latestSample.getTime() - stateMemoryVector.peekFirst()._2().getTime() > maxWaitingTime)) {

            StateCandidate estimate = null;
            for (StateCandidate candidate : stateMemoryVector.peekFirst()._1().getStateCandidates().values()) {
                if (estimate == null || estimate.getFiltProb() < candidate.getFiltProb()) {
                    estimate = candidate;
                }
            }
            routeMatchResult.add(estimate);
            deletes.add(stateMemoryVector.removeFirst()._1());
        }

        for (StateMemory delete : deletes) {
            for (StateCandidate stateCandidate : delete.getStateCandidates().values()) {
                sequenceCandidateVotes.remove(stateCandidate.getId());
            }
        }

        // set predecessors of candidates in the first state of new sequence to null
        StateMemory newFirst = stateMemoryVector.peekFirst()._1();
        for (StateCandidate stateCandidate : newFirst.getStateCandidates().values()) {
            stateCandidate.setPredecessor(null);
        }

        assert (maxStateNum < 0 || maxStateNum + 1 < stateMemoryVector.size());
        return routeMatchResult;
    }

    public List<StateCandidate> update(StateMemory latestStateMemory, StateSample lastSample) {
        expand(latestStateMemory, lastSample);

        deleteCandidates(); // delete redundant candidates in the chain

        stateMemoryVector.add(new Pair<>(latestStateMemory, lastSample));

        return manageWindSize(lastSample);
    }
    
    public StateMemory lastStateMemory() {
        if (stateMemoryVector.isEmpty()) return null;
        return stateMemoryVector.peekLast()._1();
    }

    public LinkedList<Pair<StateMemory, StateSample>> getStateMemoryVector() {
        return stateMemoryVector;
    }
}