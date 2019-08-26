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

//        StateMemory lastState = stateMemoryVector.peekLast()._1();
        StateMemory lastState = stateMemoryVector.peekFirst()._1();
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
     * @param index     state index of this candidate (start from 0)
     */
    private void remove(StateCandidate candidate, int index) {
//        while (index >= 0) {
        while (index < stateMemoryVector.size() - 1) {
            Set<StateCandidate> candidateSet = stateMemoryVector.get(index)._1().getStateCandidates();
            sequenceCandidateVotes.remove(candidate.getId());
            candidateSet.remove(candidate);

            StateCandidate predecessor = candidate.getPredecessor();
            if (predecessor == null) {
                return;
            }
            sequenceCandidateVotes.put(predecessor.getId(), sequenceCandidateVotes.get(predecessor.getId()) - 1);

            if (sequenceCandidateVotes.get(predecessor.getId()) == 0) {
                candidate = predecessor;
//                index -= 1;
                index += 1;
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
//                && stateMemoryVector.peekLast()._2().getTime() > lastSample.getTime()) {
                && stateMemoryVector.peekFirst()._2().getTime() > lastSample.getTime()) {
            System.out.println(stateMemoryVector.isEmpty());
            throw new RuntimeException("inconsistent time sequence");
        }

        StateCandidate estimate = optimalPredecessor();

        for (StateCandidate candidate : candidates) {
            // add latest candidates to vote count map
            sequenceCandidateVotes.put(candidate.getId(), 0);

            if (candidate.getPredecessor() == null) candidate.setPredecessor(estimate);

            // predecessors must be candidates in previous state
            if (candidate.getPredecessor() != null) {
                if (!sequenceCandidateVotes.containsKey(candidate.getPredecessor().getId())) {
                    throw new RuntimeException("inconsistent update vector");
                }
//                if (!stateMemoryVector.peekFirst()._1().getStateCandidates().contains(candidate.getPredecessor())) {
//                    throw new RuntimeException("inconsistent update vector");
//                }

                boolean notFound = true;
                for (StateCandidate sc : stateMemoryVector.peekFirst()._1().getStateCandidates()) {
                    if (sc.getId().equals(candidate.getPredecessor().getId())) {
                        notFound = false;
                    }
                }
                if (notFound) {
                    double time = stateMemoryVector.peekFirst()._2().getTime();
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

//        StateMemory lastState = stateMemoryVector.peekLast()._1();
        StateMemory lastState = stateMemoryVector.peekFirst()._1();

        Set<StateCandidate> deletes = new HashSet<>();

        for (StateCandidate candidate : lastState.getStateCandidates()) {
            if (sequenceCandidateVotes.get(candidate.getId()) == 0) deletes.add(candidate);
        }

        for (StateCandidate delete : deletes) {
//            remove(delete, stateMemoryVector.size() - 1);
            remove(delete, 0);
        }

    }


    /**
     * The window shrinks from behind when
     * 1) a convergence point is found anywhere in the Markov chain covered by the window;
     * 2) or reaches the maximum number of states
     */
    private List<StateMemory> manageWindSize(StateSample latestSample) {

        if (maxStateNum < 0 && maxWaitingTime < 0) return null;

//        StateMemory last = stateMemoryVector.peekLast()._1();
        StateMemory last = stateMemoryVector.peekFirst()._1();
        int cnt = 0;
        for (StateCandidate stateCandidate : last.getStateCandidates()) {
            if (sequenceCandidateVotes.containsKey(stateCandidate.getId())) cnt += 1;
        }

        List<StateMemory> deletes = new LinkedList<>();
        if (cnt == 0) {
            throw new RuntimeException("matching break");

        } else if (cnt == 1) {
            // if only one candidate in last state is stored in the chain, this candidate match is known as convergence point
            // it then become first state in the sequence
            while (getStateMemoryVector().size() > 1) {
                deletes.add(stateMemoryVector.removeLast()._1());
            }

        } else if (maxStateNum + 1 < stateMemoryVector.size() ||
//           && latestSample.getTime() - stateMemoryVector.peekFirst()._2().getTime() > maxWaitingTime)) {
                latestSample.getTime() - stateMemoryVector.peekLast()._2().getTime() > maxWaitingTime) {

//            StateMemory deletes = stateMemoryVector.removeFirst()._1();
            deletes.add(stateMemoryVector.removeLast()._1());
        }

        if (deletes.size() == 0) return deletes;

        for (StateMemory delete : deletes) {

            for (StateCandidate stateCandidate : delete.getStateCandidates())
                sequenceCandidateVotes.remove(stateCandidate.getId());

//            StateMemory newFirst = stateMemoryVector.peekFirst()._1();
            StateMemory newFirst = stateMemoryVector.peekLast()._1();
            for (StateCandidate stateCandidate : newFirst.getStateCandidates()) {
                stateCandidate.setPredecessor(null);
            }
        }
        assert (maxStateNum < 0 || maxStateNum + 1 < stateMemoryVector.size());
        return deletes;
    }

    public void update(StateMemory latestStateMemory, StateSample lastSample) {
        expand(latestStateMemory, lastSample);

        deleteCandidates(); // delete redundant candidates in the chain

        stateMemoryVector.push(new Pair<>(latestStateMemory, lastSample));

//        manageWindSize(lastSample);

        double lastTime = stateMemoryVector.peekLast()._2().getTime();
        double firstTime = stateMemoryVector.peekFirst()._2().getTime();

        double sample = lastSample.getTime();
    }

    /**
     * Gets the most likely sequence of state candidates <i>s<sub>0</sub>, s<sub>1</sub>, ...,
     * s<sub>t</sub></i>.
     *
     * @return List of the most likely sequence of state candidates.
     */
    public List<StateCandidate> sequence() {
        if (stateMemoryVector.isEmpty()) {
            return null;
        }

        StateCandidate kestimate = optimalPredecessor();
        LinkedList<StateCandidate> ksequence = new LinkedList<>();

        for (int i = stateMemoryVector.size() - 1; i >= 0; --i) {
            if (kestimate != null) {
                ksequence.push(kestimate);
                kestimate = kestimate.getPredecessor();
            }
        }

        return ksequence;
    }

    public StateMemory lastStateMemory() {
        if (stateMemoryVector.isEmpty()) return null;
//        return stateMemoryVector.peekLast()._1();
        return stateMemoryVector.peekFirst()._1();
    }

    public LinkedList<Pair<StateMemory, StateSample>> getStateMemoryVector() {
        return stateMemoryVector;
    }
}