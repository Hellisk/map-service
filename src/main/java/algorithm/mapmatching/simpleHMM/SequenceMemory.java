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
                    throw new RuntimeException("inconsistent updateGoh vector");
                }
                // updateGoh votes of predecessors
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
            StateMemory latestStateMemory, StateSample lastSample, Map<String, StateCandidate> candidateSeq) {
        expand(latestStateMemory, lastSample);
        List<StateMemory> deletes = new ArrayList<>();
        if (maxStateNum < stateMemoryVector.size() && maxStateNum > 0) {
            // force to output all states in window, then empty window
            reverse(candidateSeq, stateMemoryVector.size() - 1);
            while (stateMemoryVector.size() > 0) {
                deletes.add(stateMemoryVector.removeFirst());
            }
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

        assert (maxStateNum + 1 < stateMemoryVector.size());
    }

    public void updateGoh(
            StateMemory latestStateMemory, StateSample lastSample, Map<String, StateCandidate> optimalCandiSeq) {
        expand(latestStateMemory, lastSample);
        shrink(optimalCandiSeq);
    }

    public void updateEddy(StateMemory latestStateMemory,
                           StateSample lastSample, Map<String, StateCandidate> optimalCandiSeq, double gamma) {
        expand(latestStateMemory, lastSample);
        shrinkWisely(optimalCandiSeq, checkUncertainty(gamma));
    }

    public StateMemory lastStateMemory() {
        if (stateMemoryVector.isEmpty()) return null;
        return stateMemoryVector.peekLast();
    }

    public LinkedList<StateMemory> getStateMemoryVector() {
        return stateMemoryVector;
    }


    /**
     * Eddy method to check matching uncertainty at the first state
     *
     * @param gamma to control the severity of latency would like to pay
     * @return Pair<Integer, Map < String, Map < String, Double>>>
     */
    private Pair<Integer, Map<String, Map<StateCandidate, Double>>> checkUncertainty(double gamma) {
        if (stateMemoryVector.size() == 1) return null;
        // t1 is the first state, t^ is the last state
        StateMemory firstState = stateMemoryVector.peekFirst();


        // stateMemoryId -> (stateCandidateId --> uncertainty)
        Map<String, Map<StateCandidate, Double>> stateUncertainties = new HashMap<>();

        Collection<StateCandidate> lastState = stateMemoryVector.peekLast().getStateCandidates().values();

        int outputState = -1;
        for (StateCandidate kEstimate : lastState) {
            double filtProbAtLastState = kEstimate.getFiltProb();
            for (int i = stateMemoryVector.size() - 1; i > 0; --i) {
                // smallest i is 1
                // corresponding state index is i-1, so the last kEstimate in the last is at first state
                kEstimate = kEstimate.getPredecessor();
                if (kEstimate == null) {
                    // hmm break can only happen between the last and second last state in window
                    outputState = i - 1;
                    kEstimate = stateMemoryVector.get(i - 1).getFiltProbCandidate();
                    filtProbAtLastState = kEstimate.getFiltProb();
                }
                String corresStateId = stateMemoryVector.get(i - 1).getId(); // use the predecessor's state id
                stateUncertainties.computeIfAbsent(corresStateId, k -> new HashMap<>());

                if (stateUncertainties.get(corresStateId).get(kEstimate) == null) {
                    stateUncertainties.get(corresStateId).put(kEstimate, filtProbAtLastState);
                } else {
                    stateUncertainties.get(corresStateId).put(
                            kEstimate, stateUncertainties.get(corresStateId).get(kEstimate) + filtProbAtLastState);
                }
            }
        }

        double entropyLoss = 0;
        Map<StateCandidate, Double> firstStateUncertainties = stateUncertainties.get(firstState.getId());
        for (Double score : firstStateUncertainties.values()) {
            entropyLoss += score * Math.log(score);
        }
        double accuracyCost = -entropyLoss / firstStateUncertainties.size();
        double latencyCost = gamma * (stateMemoryVector.peekLast().getSample().getTime()
                - stateMemoryVector.peekFirst().getSample().getTime());

        // when entropy loss equals (or less than) latency penalty, need to output the first state
        // if outputState != -1, need to output results for states before break
        if (accuracyCost <= latencyCost || outputState != -1) {
            outputState = outputState == -1 ? 0 : outputState;
            return new Pair<>(outputState, stateUncertainties);
        }

        return new Pair<>(-1, stateUncertainties);
    }

    /**
     * shrink the sliding window from indicated state index (if matching uncertainty is low or break)
     *
     * @param optimalCandidateSeq store the selected candidate for each state inside the window
     */
    private void shrinkWisely(Map<String, StateCandidate> optimalCandidateSeq,
                              Pair<Integer, Map<String, Map<StateCandidate, Double>>> outputIndexToStateUncertainties) {

        if (stateMemoryVector.size() == 1) return;
        // check if to output any result
        Map<String, Map<StateCandidate, Double>> statesUncertainties = outputIndexToStateUncertainties._2();
        int outputIndex = outputIndexToStateUncertainties._1();
        if (outputIndex == -1) return;

        List<StateMemory> deletes = new ArrayList<>();
        for (int i = 0; i <= outputIndex; ++i) {
            StateCandidate estimate = null;
            double maxProb = -1;
            Map<StateCandidate, Double> stateUncertainties = statesUncertainties.get(stateMemoryVector.peekFirst().getId());
            for (Map.Entry<StateCandidate, Double> candiToProb : stateUncertainties.entrySet()) {
                if (estimate == null || maxProb < candiToProb.getValue()) {
                    estimate = candiToProb.getKey();
                    maxProb = candiToProb.getValue();
                }
            }
            optimalCandidateSeq.put(stateMemoryVector.peekFirst().getId(), estimate);
            deletes.add(stateMemoryVector.removeFirst());
        }

        for (StateMemory delete : deletes) {
            for (StateCandidate stateCandidate : delete.getStateCandidates().values()) {
                sequenceCandidateVotes.remove(stateCandidate.getId());
            }
        }

        // set predecessors of candidates in the first state of new sequence to null
        if (deletes.size() > 0 && stateMemoryVector.size() > 0) {
            StateMemory newFirst = stateMemoryVector.peekFirst();
            for (StateCandidate stateCandidate : newFirst.getStateCandidates().values()) {
                stateCandidate.setPredecessor(null);
            }
        }

    }

    /**
     * Eddy's force output after travel completed
     *
     * @param optimalCandidateSeq to store matching result
     * @param gamma                parameter
     */
    public void forceFinalOutput(Map<String, StateCandidate> optimalCandidateSeq, double gamma) {
        Pair<Integer, Map<String, Map<StateCandidate, Double>>> stateUncertainties = checkUncertainty(gamma);

        // uncertainty of last state will not be checked
        if (stateMemoryVector.size() >= 2) {
            shrinkWisely(optimalCandidateSeq, new Pair<>(stateMemoryVector.size() - 2, stateUncertainties._2()));
        }

        // therefore, the last state need to be removed manually
        StateMemory lastState = stateMemoryVector.removeLast();
        optimalCandidateSeq.put(lastState.getId(), lastState.getFiltProbCandidate());
    }
}