package algorithm.mapmatching.simpleHMM;

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
     * Expand window when new state is generated
     * The actions include:
     * 1. updates votes of candidates in last state after processing a new state
     * 2. delete redundant old candidates from both vote map and chain
     * 3. add new candidates to vote map and chain
     */
    private void expand(StateMemory stateMemory, StateSample lastSample) {
        Set<StateCandidate> candidates = new LinkedHashSet<>(stateMemory.getStateCandidates().values());

        if (!stateMemoryVector.isEmpty()
                && stateMemoryVector.peekLast().getSample().getTime() > lastSample.getTime()) {
            throw new RuntimeException("inconsistent time sequence");
        }

//        StateCandidate estimate = optimalPredecessor();

        for (StateCandidate candidate : candidates) {
            // add latest candidates to vote count map
            sequenceCandidateVotes.put(candidate.getId(), 0);


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
        deleteCandidates(); // delete redundant candidates in the chain
        stateMemoryVector.add(stateMemory);
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
    private void shrink(Map<String, StateCandidate> candidateSeq) {
        if (stateMemoryVector.size() == 1) return; // just finished initial mm

        StateMemory last = stateMemoryVector.get(stateMemoryVector.size() - 2);
        List<StateMemory> deletes = new LinkedList<>();

        if (last.getStateCandidates().size() == 1) {
            /* if the second last state only has one candidate, this candidate is a convergence state
             * so backtrack all predecessor candidates of this convergence point */
            reverse(candidateSeq, stateMemoryVector.size() - 2);

            // new sequence contains two states: converging state -> current state
            while (stateMemoryVector.size() > 2) {
                deletes.add(stateMemoryVector.removeFirst());
            }

        } else if ((maxStateNum < stateMemoryVector.size() && maxStateNum > 0)
                || (maxWaitingTime >= 0 &&
                stateMemoryVector.getLast().getSample().getTime() -
                        stateMemoryVector.peekFirst().getSample().getTime() > maxWaitingTime)) {
            // reach maximum bound, force to output the most likely candidate of the first state
            StateMemory firstState = stateMemoryVector.peekFirst();
            if (!candidateSeq.containsKey(firstState.getId())) {
                StateCandidate estimate = firstState.getFiltProbCandidate();
                candidateSeq.put(firstState.getId(), estimate);
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
    }

    /**
     * Gets the most likely sequence of state candidates
     */
    public void reverse(Map<String, StateCandidate> candidateSeq, int index) {

        StateCandidate kEstimate = stateMemoryVector.get(index).getFiltProbCandidate();

        for (int i = index; i >= 0; --i) {
            String stateID = stateMemoryVector.get(i).getId();
            if (candidateSeq.containsKey(stateID)) continue;
            if (kEstimate != null) {
                candidateSeq.put(stateID, kEstimate);
                kEstimate = kEstimate.getPredecessor();

            } else {
                // HMM break
                StateMemory breakState = stateMemoryVector.get(i);
                StateCandidate estimate = breakState.getFiltProbCandidate();
                kEstimate = estimate.getPredecessor();
                candidateSeq.put(stateID, estimate);
            }
        }
    }


    public void update(
            StateMemory latestStateMemory, StateSample lastSample, Map<String, StateCandidate> routeMatchResult) {
        expand(latestStateMemory, lastSample);
        shrink(routeMatchResult);
        // if gamma > 0
        // shrinkWisely()
    }

    public StateMemory lastStateMemory() {
        if (stateMemoryVector.isEmpty()) return null;
        return stateMemoryVector.peekLast();
    }

    public LinkedList<StateMemory> getStateMemoryVector() {
        return stateMemoryVector;
    }


    /**
     * Eddy method to detect convergence
     *
     * @param gamma to control the severity of latency would like to pay
     * @return -1 if no need to back track; or the index of state where backtracking should be initialized
     * the index is statevector.size()-1; or the index where HMM break happened
     */
    private int backTrackAtState(double gamma) {
        // t1 is the first state, t^ is the last state
        StateMemory firstState = stateMemoryVector.peekFirst();
        Map<String, Double> firstCandiScoreFromFuture = new HashMap<>();

        // first state is empty if hmm break happened
        for (String candidateID : firstState.getStateCandidates().keySet()) {
            firstCandiScoreFromFuture.put(candidateID, 0d);
        }

        Collection<StateCandidate> lastState = stateMemoryVector.peekLast().getStateCandidates().values();

        int breakStateIndex = -1;
        for (StateCandidate kEstimate : lastState) {
            double filtProbAtLastState = kEstimate.getFiltProb();
            for (int i = stateMemoryVector.size() - 1; i >= 1; --i) {
                if (kEstimate != null) {
                    kEstimate = kEstimate.getPredecessor();
                } else {
                    // hmm break
                    breakStateIndex = i;
                    break;
                }
            }
            if (breakStateIndex > 0) break;
            StateCandidate firstStateCandidate = kEstimate.getPredecessor();
            firstCandiScoreFromFuture.put(firstStateCandidate.getId(),
                    firstCandiScoreFromFuture.get(firstStateCandidate.getId()) + filtProbAtLastState);
        }

        // if hmm break happened, return its state index
        if (breakStateIndex > 0) return breakStateIndex;

        double entropyLoss = 0;
        for (Double score : firstCandiScoreFromFuture.values()) {
            entropyLoss += score * Math.log(score);
        }
        double accuracyCost = -entropyLoss / firstCandiScoreFromFuture.size();
        double latencyCost = gamma * (stateMemoryVector.peekLast().getSample().getTime()
                - stateMemoryVector.peekFirst().getSample().getTime());

        // when entropy loss equals (or less than) latency penalty, output result
        if (accuracyCost <= latencyCost) {
            return stateMemoryVector.size() - 1;
        }
        return -1;
    }

    /**
     * shrink the sliding window from indicated state index (if converged or break)
     *
     * @param candidateSeq store the selected candidate for each state inside the window
     */
    private void shrinkWisely(Map<String, StateCandidate> candidateSeq, double gamma) {
        if (stateMemoryVector.size() == 1) return; // initial mm

        //
        int backTrackingStateIndex = backTrackAtState(gamma);
        if (backTrackingStateIndex == -1) return;

        reverse(candidateSeq, backTrackingStateIndex);
        // new sequence contains two states: converging state -> current state
        List<StateMemory> deletes = new ArrayList<>();
        while (stateMemoryVector.size() > backTrackingStateIndex) {
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

    }
}