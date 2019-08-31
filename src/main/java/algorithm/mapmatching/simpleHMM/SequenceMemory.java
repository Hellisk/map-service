package algorithm.mapmatching.simpleHMM;

import util.function.GreatCircleDistanceFunction;
import util.object.structure.PointMatch;

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
    private LinkedList<StateMemory> stateMemoryVector = new LinkedList<>();
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

        StateMemory lastState = stateMemoryVector.peekLast();
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
                && stateMemoryVector.peekLast().getSample().getTime() > lastSample.getTime()) {
            throw new RuntimeException("inconsistent time sequence");
        }

//        StateCandidate estimate = optimalPredecessor();

        for (StateCandidate candidate : candidates) {
            // add latest candidates to vote count map
            sequenceCandidateVotes.put(candidate.getId(), 0);

//            if (candidate.getPredecessor() == null) candidate.setPredecessor(estimate);

            // predecessors must be candidates in previous state
            if (candidate.getPredecessor() != null) {
                if (!sequenceCandidateVotes.containsKey(candidate.getPredecessor().getId())
                        || !stateMemoryVector.peekLast().getStateCandidates().containsKey(candidate.getPredecessor().getId())) {
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

        StateMemory lastState = stateMemoryVector.peekLast();

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
            stateMemoryVector.get(index).getStateCandidates().remove(candidate.getId());

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
    private Map<String, StateCandidate> manageWindSize(
            StateSample latestSample, Map<String, StateCandidate> routeMatchResult) {
        if (getStateMemoryVector().size() == 1) return new HashMap<>(); // just finished initial mm

        StateMemory last = stateMemoryVector.get(stateMemoryVector.size() - 2);
        List<StateMemory> deletes = new LinkedList<>();

        if (last.getStateCandidates().size() == 1) {
            StateCandidate kEstimate = last.getFiltProbCandidate(); // 不可能是null
            for (int i = stateMemoryVector.size() - 2; i >= 0; --i) {
                String stateID = stateMemoryVector.get(i).getId(); // start from the convergence state
                if (routeMatchResult.containsKey(stateID)) continue;

                if (kEstimate != null) {
                    routeMatchResult.put(stateID, kEstimate);
                    kEstimate = kEstimate.getPredecessor();
                } else {
                    StateMemory breakState = stateMemoryVector.get(i);
                    StateCandidate probCandidate = breakState.getFiltProbCandidate();
                    if (probCandidate != null) {
                        routeMatchResult.put(stateID, probCandidate);
                    } else {
                        // this state got no candidate (no neighbour points)
                        routeMatchResult.put(stateID, new StateCandidate(new PointMatch(new GreatCircleDistanceFunction()), latestSample));
                    }
                }
            }

            // new sequence contains two states: converging state -> current state
            while (getStateMemoryVector().size() > 2) {
                deletes.add(stateMemoryVector.removeFirst());
            }

        } else if ((maxStateNum < stateMemoryVector.size() && maxStateNum > 0)
                || (maxWaitingTime >= 0 && latestSample.getTime() - stateMemoryVector.peekFirst().getSample().getTime() > maxWaitingTime)) {
            // reach maximum bound, force to output the most likely candidate of the first state
            StateMemory firstState = stateMemoryVector.peekFirst();

            if (firstState.getStateCandidates().isEmpty()) {
                // peek the candidate with the highest filter prob
                if (!routeMatchResult.containsKey(firstState.getId())) {
                    routeMatchResult.put(firstState.getId(), firstState.getFiltProbCandidate());
                }
            } else {
                // it was NOT a convergence state, so need to output for this time
                // peek the candidate with the greatest vote
                int vote = -1;
                StateCandidate optimalCandidate = null;
                for (StateCandidate firstStateCandidate : firstState.getStateCandidates().values()) {
                    if (optimalCandidate == null || vote < sequenceCandidateVotes.get(firstStateCandidate.getId())) {
                        vote = sequenceCandidateVotes.get(firstStateCandidate.getId());
                        optimalCandidate = firstStateCandidate;
                    }
                }
                if (!routeMatchResult.containsKey(firstState.getId())) {
                    routeMatchResult.put(firstState.getId(), optimalCandidate);
                }
            }
            deletes.add(stateMemoryVector.removeFirst());
        }

        for (StateMemory delete : deletes) {
            for (StateCandidate stateCandidate : delete.getStateCandidates().values()) {
                sequenceCandidateVotes.remove(stateCandidate.getId());
            }
        }

        // set predecessors of candidates in the first state of new sequence to null
        if (deletes.size() > 0) {
            StateMemory newFirst = stateMemoryVector.peekFirst();
            for (StateCandidate stateCandidate : newFirst.getStateCandidates().values()) {
                stateCandidate.setPredecessor(null);
            }
        }

        assert (maxStateNum < 0 || maxStateNum + 1 < stateMemoryVector.size());
        return routeMatchResult;
    }

    public Map<String, StateCandidate> update(
            StateMemory latestStateMemory, StateSample lastSample, Map<String, StateCandidate> routeMatchResult) {
        expand(latestStateMemory, lastSample);

        deleteCandidates(); // delete redundant candidates in the chain

        stateMemoryVector.add(latestStateMemory);

        return manageWindSize(lastSample, routeMatchResult);
    }

    public StateMemory lastStateMemory() {
        if (stateMemoryVector.isEmpty()) return null;
        return stateMemoryVector.peekLast();
    }

    public LinkedList<StateMemory> getStateMemoryVector() {
        return stateMemoryVector;
    }
}