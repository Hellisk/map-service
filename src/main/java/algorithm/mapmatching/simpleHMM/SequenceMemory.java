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
     * Return the most likely candidate at last state
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
     * @param index state index of this candidate (start from 0)
     */
    private void remove(StateCandidate candidate, int index) {
        while (index >= 0) {
            Set<StateCandidate> candidateSet = stateMemoryVector.get(index)._1().getStateCandidates();
            sequenceCandidateVotes.remove(candidate);
            candidateSet.remove(candidate);

            StateCandidate predecessor = candidate.getPredecessor();
            if (predecessor == null) {
                return;
            }
            sequenceCandidateVotes.put(predecessor, sequenceCandidateVotes.get(predecessor) - 1);

            if (sequenceCandidateVotes.get(predecessor) == 0) {
                candidate = predecessor;
                index -= 1;
            } else return;
        }
    }


    /**
     * The window expands forward as new trajectory points are processed
     */
    private void expand(StateMemory stateMemory, StateSample lastSample) {
        Set<StateCandidate> candidates = stateMemory.getStateCandidates();

        if (candidates.isEmpty()) return;
        if (!stateMemoryVector.isEmpty()
                && stateMemoryVector.peekLast()._2().getTime() > lastSample.getTime()) {
            System.out.println(stateMemoryVector.isEmpty());
            throw new RuntimeException("inconsistent time sequence");
        }

        StateCandidate estimate = optimalPredecessor();

        for (StateCandidate candidate : candidates) {
            // add latest candidates to vote count map
            sequenceCandidateVotes.put(candidate, 0);

            if (candidate.getPredecessor() == null) candidate.setPredecessor(estimate);

            // predecessors must be candidates in previous state
            if (candidate.getPredecessor() != null) {
                if (!sequenceCandidateVotes.containsKey(candidate.getPredecessor())
                        || !stateMemoryVector.peekLast()._1().getStateCandidates().contains(candidate.getPredecessor())) {
                    throw new RuntimeException("inconsistent update vector");
                }
                // update votes of predecessors
                sequenceCandidateVotes.put(candidate.getPredecessor(),
                        sequenceCandidateVotes.get(candidate.getPredecessor()) + 1);

            }
        }
    }

    /**
     * Delete redundant candidates (whose votes are 0 in the chain) in sequence and map
     */
    private void deleteCandidates() {
        if (stateMemoryVector.isEmpty()) return;

        StateMemory lastState = stateMemoryVector.peekLast()._1();

        Set<StateCandidate> deletes = new HashSet<>();

        for (StateCandidate candidate : lastState.getStateCandidates()) {
            if (sequenceCandidateVotes.get(candidate) == 0) deletes.add(candidate);
        }

        for (StateCandidate delete : deletes) {
            remove(delete, stateMemoryVector.size() - 1);
        }

    }


    /**
     * The window shrinks from behind when
     * 1) a convergence point is found anywhere in the Markov chain covered by the window;
     * 2) or reaches the maximum number of states
     */
    private void manageWindSize(StateSample latestSample) {
        // if only one candidate in last state is stored in the chain, this candidate match is known as convergence point
        StateMemory last = stateMemoryVector.peekLast()._1();
        int cnt = 0;
        for (StateCandidate stateCandidate : last.getStateCandidates()) {
            if (sequenceCandidateVotes.containsKey(stateCandidate)) cnt += 1;
        }

        if (cnt == 0) {
            throw new RuntimeException("matching break");
        } else if (
            // converge
                cnt == 1
                        // waiting time
                        || (maxWaitingTime > 0 &&
                        latestSample.getTime() - stateMemoryVector.peekFirst()._2().getTime() > maxWaitingTime)
                        // sequence length upperbound
                        || (maxStateNum >= 0 && maxStateNum + 1 < stateMemoryVector.size())) {

            StateMemory deletes = stateMemoryVector.removeFirst()._1();
            for (StateCandidate stateCandidate : deletes.getStateCandidates()) {
                sequenceCandidateVotes.remove(stateCandidate);
            }

            StateMemory newFirst = stateMemoryVector.peekFirst()._1();
            for (StateCandidate stateCandidate : newFirst.getStateCandidates()) {
                stateCandidate.setPredecessor(null);
            }
            assert (maxStateNum < 0 || maxStateNum + 1 < stateMemoryVector.size());
        }
    }

    public void update(StateMemory latestStateMemory, StateSample lastSample) {
        expand(latestStateMemory, lastSample);

        deleteCandidates(); // delete redundant candidates in the chain

        stateMemoryVector.push(new Pair<>(latestStateMemory, lastSample));

        manageWindSize(lastSample);
    }

    public StateSample lastSample() {
        if (stateMemoryVector.isEmpty()) return null;
        return stateMemoryVector.peekLast()._2();
    }

    public StateMemory lastStateMemory() {
        if (stateMemoryVector.isEmpty()) return null;
        return stateMemoryVector.peekLast()._1();
    }
}