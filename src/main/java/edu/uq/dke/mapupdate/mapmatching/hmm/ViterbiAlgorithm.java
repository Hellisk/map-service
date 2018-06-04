/**
 * Copyright (C) 2015-2016, BMW Car IT GmbH and BMW AG
 * Author: Stefan Holder (stefan.holder@bmw.de)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.uq.dke.mapupdate.mapmatching.hmm;

import edu.uq.dke.mapupdate.util.object.datastructure.ItemWithProbability;
import edu.uq.dke.mapupdate.util.object.datastructure.Pair;
import edu.uq.dke.mapupdate.util.object.datastructure.Triplet;

import java.io.Serializable;
import java.util.*;

/**
 * Implementation of the Viterbi algorithm for time-inhomogeneous Markov processes,
 * meaning that the set of states and state transition probabilities are not necessarily fixed
 * for all time steps. The plain Viterbi algorithm for stationary Markov processes is described e.g.
 * in Rabiner, Juang, An introduction to Hidden Markov Models, IEEE ASSP Mag., pp 4-16, June 1986.
 *
 * <p>Generally expects logarithmic probabilities as input to prevent arithmetic underflows for
 * small probability values.
 *
 * <p>This algorithm supports storing transition objects in
 * {@link #nextStep(Object, Collection, Map, Map, Map)}. For instance if a HMM is
 * used for map matching, this could be routes between road position candidates.
 * The transition descriptors of the most likely sequence can be retrieved later in
 * {@link SequenceState#transitionDescriptor} and hence do not need to be stored by the
 * caller. Since the caller does not know in advance which transitions will occur in the most
 * likely sequence, this reduces the number of transitions that need to be kept in memory
 * from t*n² to t*n since only one transition descriptor is stored per back pointer,
 * where t is the number of time steps and n the number of candidates per time step.
 *
 * <p>For long observation sequences, back pointers usually converge to a single path after a
 * certain number of time steps. For instance, when matching GPS coordinates to roads, the last
 * GPS positions in the trace usually do not affect the first road matches anymore.
 * This implementation exploits this fact by letting the Java garbage collector
 * take care of unreachable back pointers. If back pointers converge to a single path after a
 * constant number of time steps, only O(t) back pointers and transition descriptors need to be
 * stored in memory.
 *
 * @param <S> the state type
 * @param <O> the observation type
 * @param <D> the transition descriptor type. Pass {@link Object} if transition descriptors are not
 *            needed.
 */
@SuppressWarnings("serial")
public class ViterbiAlgorithm<S, O, D> implements Serializable {

    public Map<S, ExtendedState<S, O, D>> getLastExtendedStates() {
        return lastExtendedStates;
    }

    private static class ForwardStepResult<S, O, D> {
        final Map<S, Double> newMessage;

        /**
         * Includes back pointers to previous state candidates for retrieving the top-k most likely
         * sequence after the forward pass.
         */
        final Map<S, ExtendedState<S, O, D>> newExtendedStates;

        ForwardStepResult(int numberStates) {
            newMessage = new LinkedHashMap<>(HMMUtils.initialHashMapCapacity(numberStates));
            newExtendedStates = new LinkedHashMap<>(HMMUtils.initialHashMapCapacity(numberStates));
        }
    }

    /**
     * Allows to retrieve the top-k most likely sequence using back pointers.
     */
    private Map<S, ExtendedState<S, O, D>> lastExtendedStates;

    private Collection<S> prevCandidates;

    private int rankLength = 1; // the length of the ranked list, default = 1

    /**
     * For each state s_t of the current time step t, message.get(s_t) contains the log
     * probability of the most likely sequence ending in state s_t with given observations
     * o_1, ..., o_t.
     * <p>
     * Formally, this is max log p(s_1, ..., s_t, o_1, ..., o_t) w.r.t. s_1, ..., s_{t-1}.
     * Note that to compute the most likely state sequence, it is sufficient and more
     * efficient to compute in each time step the joint probability of states and observations
     * instead of computing the conditional probability of states given the observations.
     */
    private Map<S, Double> message;

    private boolean isBroken = false;

    private List<Map<S, Double>> messageHistory; // For debugging only.

    /**
     * Need to construct a new instance for each sequence of observations.
     * Does not keep the message history.
     */
    ViterbiAlgorithm(int rankLength) {
        this(false);
        this.rankLength = rankLength;
    }

    /**
     * Need to construct a new instance for each sequence of observations.
     *
     * @param keepMessageHistory Whether to store intermediate forward messages
     *                           (probabilities of intermediate most likely paths) for debugging.
     */
    private ViterbiAlgorithm(boolean keepMessageHistory) {
        if (keepMessageHistory) {
            messageHistory = new ArrayList<>();
        }
    }

    void setLastExtendedStates(Map<S, ExtendedState<S, O, D>> lastExtendedStates) {
        this.lastExtendedStates = lastExtendedStates;
    }

    public Collection<S> getPrevCandidates() {
        return prevCandidates;
    }

    void setPrevCandidates(Collection<S> prevCandidates) {
        this.prevCandidates = prevCandidates;
    }

    Map<S, Double> getMessage() {
        return message;
    }

    void setMessage(Map<S, Double> message) {
        this.message = message;
    }

//    /**
//     * manually set the states
//     *
//     * @param candidates
//     * @param message
//     * @param extendedStates
//     */
//    public void setPrevState(Collection<S> candidates, Map<S, Double> message, Map<S, ExtendedState<S, O, D>> extendedStates) {
//        this.message = message;
//        this.prevCandidates = candidates;
//        this.lastExtendedStates = extendedStates;
//    }

    /**
     * Lets the HMM computation start with the given initial state probabilities.
     *
     * @param initialStates           Pass a collection with predictable iteration order such as
     *                                {@link ArrayList} to ensure deterministic results.
     * @param initialLogProbabilities Initial log probabilities for each initial state.
     * @throws NullPointerException  if any initial probability is missing
     * @throws IllegalStateException if this method or
     *                               {@link #startWithInitialObservation(Object, Collection, Map)}
     *                               has already been called
     */
    private void startWithInitialStateProbabilities(Collection<S> initialStates,
                                                    Map<S, Double> initialLogProbabilities) {
        initializeStateProbabilities(null, initialStates, initialLogProbabilities);
    }

    /**
     * Lets the HMM computation start at the given first observation and uses the given emission
     * probabilities as the initial state probability for each starting state s.
     *
     * @param candidates               Pass a collection with predictable iteration order such as
     *                                 {@link ArrayList} to ensure deterministic results.
     * @param emissionLogProbabilities Emission log probabilities of the first observation for
     *                                 each of the road position candidates.
     * @throws NullPointerException  if any emission probability is missing
     * @throws IllegalStateException if this method or
     *                               {@link #startWithInitialStateProbabilities(Collection, Map)}} has already been called
     */
    void startWithInitialObservation(O observation, Collection<S> candidates,
                                     Map<S, Double> emissionLogProbabilities) {
        initializeStateProbabilities(observation, candidates, emissionLogProbabilities);
    }

    /**
     * Processes the next time step. Must not be called if the HMM is broken.
     *
     * @param candidates                 Pass a collection with predictable iteration order such as
     *                                   {@link ArrayList} to ensure deterministic results.
     * @param emissionLogProbabilities   Emission log probabilities for each candidate state.
     * @param transitionLogProbabilities Transition log probability between all pairs of candidates.
     *                                   A transition probability of zero is assumed for every missing transition.
     * @param transitionDescriptors      Optional objects that describes the transitions.
     * @throws NullPointerException  if any emission probability is missing
     * @throws IllegalStateException if neither
     *                               {@link #startWithInitialStateProbabilities(Collection, Map)} nor
     *                               {@link #startWithInitialObservation(Object, Collection, Map)}
     *                               has not been called before or if this method is called after an HMM break has occurred
     */
    void nextStep(O observation, Collection<S> candidates,
                  Map<S, Double> emissionLogProbabilities,
                  Map<Transition<S>, Double> transitionLogProbabilities,
                  Map<Transition<S>, D> transitionDescriptors) {
        if (message == null) {
            throw new IllegalStateException(
                    "startWithInitialStateProbabilities() or startWithInitialObservation() "
                            + "must be called first.");
        }
        if (isBroken) {
            throw new IllegalStateException("Method must not be called after an HMM break.");
        }

        // Forward step
        ForwardStepResult<S, O, D> forwardStepResult = forwardStep(observation, prevCandidates,
                candidates, message, emissionLogProbabilities, transitionLogProbabilities,
                transitionDescriptors);
        isBroken = hmmBreak(forwardStepResult.newMessage);
        if (isBroken) return;

        if (messageHistory != null) {
            messageHistory.add(forwardStepResult.newMessage);
        }
        message = forwardStepResult.newMessage;
        lastExtendedStates = forwardStepResult.newExtendedStates;

        prevCandidates = new ArrayList<>(candidates); // Defensive copy.
    }

    void setBroken(boolean broken) {
        this.isBroken = broken;
    }

    /**
     * See {@link #nextStep(Object, Collection, Map, Map, Map)}
     */
    public void nextStep(O observation, Collection<S> candidates,
                         Map<S, Double> emissionLogProbabilities,
                         Map<Transition<S>, Double> transitionLogProbabilities) {
        nextStep(observation, candidates, emissionLogProbabilities, transitionLogProbabilities,
                new LinkedHashMap<Transition<S>, D>());
    }

    /**
     * Returns the most likely sequence of states for all time steps. This includes the initial
     * states / initial observation time step. If an HMM break occurred in the last time step t,
     * then the most likely sequence up to t-1 is returned. See also {@link #isBroken()}.
     *
     * <p>Formally, the most likely sequence is argmax p([s_0,] s_1, ..., s_T | o_1, ..., o_T)
     * with respect to s_1, ..., s_T, where s_t is a state candidate at time step t,
     * o_t is the observation at time step t and T is the number of time steps.
     */
    List<List<SequenceState<S, O, D>>> computeMostLikelySequence(int rankLength) {
        if (message == null) {
            // Return empty most likely sequence if there are no time steps or if initial
            // observations caused an HMM break.
            return new ArrayList<>();
        } else {
            return retrieveRankedSequences(rankLength);
        }
    }

    /**
     * Returns whether an HMM occurred in the last time step.
     * <p>
     * An HMM break means that the probability of all states equals zero.
     */
    boolean isBroken() {
        return isBroken;
    }

    /**
     * Returns the sequence of intermediate forward messages for each time step.
     * Returns null if message history is not kept.
     */
    public List<Map<S, Double>> messageHistory() {
        return messageHistory;
    }

    public String messageHistoryString() {
        if (messageHistory == null) {
            throw new IllegalStateException("Message history was not recorded.");
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Message history with log probabilies\n\n");
        int i = 0;
        for (Map<S, Double> message : messageHistory) {
            sb.append("Time step ").append(i).append("\n");
            i++;
            for (S state : message.keySet()) {
                sb.append(state).append(": ").append(message.get(state)).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Returns whether the specified message is either empty or only contains state candidates
     * with zero probability and thus causes the HMM to break.
     */
    private boolean hmmBreak(Map<S, Double> message) {
        for (double logProbability : message.values()) {
            if (logProbability != Double.NEGATIVE_INFINITY) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param observation Use only if HMM only starts with first observation.
     */
    private void initializeStateProbabilities(O observation, Collection<S> candidates,
                                              Map<S, Double> initialLogProbabilities) {
        if (message != null) {
            System.out.println("Error! The message should be empty");
            message = null;
        }
        isBroken = false;

        // Set initial log probability for each start state candidate based on first observation.
        // Do not assign initialLogProbabilities directly to message to not rely on its iteration
        // order.
        final Map<S, Double> initialMessage = new LinkedHashMap<>();
        for (S candidate : candidates) {
            final Double logProbability = initialLogProbabilities.get(candidate);
            if (logProbability == null) {
                throw new NullPointerException("No initial probability for " + candidate);
            }
            initialMessage.put(candidate, logProbability);
        }

        isBroken = hmmBreak(initialMessage);
        if (isBroken) {
            System.out.println("ERROR! The initial state is broken.");
            return;
        }

        message = initialMessage;
        if (messageHistory != null) {
            messageHistory.add(message);
        }

        lastExtendedStates = new LinkedHashMap<>();
        for (S candidate : candidates) {
            lastExtendedStates.put(candidate,
                    new ExtendedState<S, O, D>(candidate, new ArrayList<>(), observation, new ArrayList<>(), new ArrayList<>()));
        }

        prevCandidates = new ArrayList<>(candidates); // Defensive copy.
    }

    /**
     * Stores additional information for each candidate. Each candidate contains the information of its top k predecessors.
     */
    public static class ExtendedState<S, O, D> {

        S state;

        /**
         * Back pointer to previous top k state candidate in the most likely sequence.
         * Back pointers are chained using plain Java references.
         * This allows garbage collection of unreachable back pointers.
         */
        List<ExtendedState<S, O, D>> backPointer;

        O observation;

        /**
         * The corresponding transition information for top k predecessors.
         */
        List<D> transitionDescriptor;

        List<Double> probabilities;

        ExtendedState(S state,
                      List<ExtendedState<S, O, D>> backPointer,
                      O observation, List<D> transitionDescriptor, List<Double> probabilities) {
            // all three lists should be of the same size, otherwise
            if (backPointer.size() != transitionDescriptor.size() || backPointer.size() != probabilities.size()) throw new
                    IllegalArgumentException("ERROR! The sizes of the lists in ExtendedState are inconsistent.");
            this.state = state;
            this.backPointer = backPointer;
            this.observation = observation;
            this.transitionDescriptor = transitionDescriptor;
            this.probabilities = probabilities;     // the log probability includes the log transition probability to the current state
            // plus the log emission probability of the current state
        }
    }

    /**
     * Computes the new forward message and the back pointers to the previous states.
     *
     * @throws NullPointerException if any emission probability is missing
     */
    private ForwardStepResult<S, O, D> forwardStep(O observation, Collection<S> prevCandidates,
                                                   Collection<S> curCandidates, Map<S, Double> message,
                                                   Map<S, Double> emissionLogProbabilities,
                                                   Map<Transition<S>, Double> transitionLogProbabilities,
                                                   Map<Transition<S>, D> transitionDescriptors) {
        final ForwardStepResult<S, O, D> result = new ForwardStepResult<>(curCandidates.size());
        assert !prevCandidates.isEmpty();

        for (S curState : curCandidates) {

            // use priority queue to sort and pick up the top k probable predecessor
            Queue<ItemWithProbability<S>> topRankedCandidates = new PriorityQueue<>();
            for (S prevState : prevCandidates) {
                final double logProbability = message.get(prevState) + transitionLogProbability(
                        prevState, curState, transitionLogProbabilities);
                if (logProbability > Double.NEGATIVE_INFINITY) {
                    topRankedCandidates.add(new ItemWithProbability<>(prevState, logProbability));
                }
            }
            ItemWithProbability<S> optimalState = topRankedCandidates.poll();
            // Throws NullPointerException if curState is not stored in the map.
            result.newMessage.put(curState, optimalState == null ? Double.NEGATIVE_INFINITY : optimalState.getProbability() + emissionLogProbabilities.get(curState));

            // Note that optimalState == null if there is no transition with non-zero probability.
            // In this case curState has zero probability and will not be part of the most likely
            // sequence, so we don't need an ExtendedState.
            if (optimalState != null) {
                final List<D> transitionDescriptionList = new ArrayList<>();
                final List<ExtendedState<S, O, D>> lastRankedExtendedStateList = new ArrayList<>();
                final List<Double> probabilities = new ArrayList<>();
                // add the optimal state into the list
                Transition<S> transition = new Transition<>(optimalState.getItem(), curState);
                transitionDescriptionList.add(transitionDescriptors.get(transition));
                ExtendedState<S, O, D> rankedExtendedState = lastExtendedStates.get(optimalState.getItem());
                lastRankedExtendedStateList.add(rankedExtendedState);
                probabilities.add(optimalState.getProbability() + emissionLogProbabilities.get(curState));
                int rankListSize = 1;
                ItemWithProbability<S> currItem;
                while ((currItem = topRankedCandidates.poll()) != null && rankListSize < rankLength) {
                    transition = new Transition<>(currItem.getItem(), curState);
                    transitionDescriptionList.add(transitionDescriptors.get(transition));
                    rankedExtendedState = lastExtendedStates.get(currItem.getItem());
                    lastRankedExtendedStateList.add(rankedExtendedState);
                    probabilities.add(currItem.getProbability() + emissionLogProbabilities.get(curState));
                    rankListSize++;
                }

                final ExtendedState<S, O, D> extendedState = new ExtendedState<>(curState,
                        lastRankedExtendedStateList, observation, transitionDescriptionList, probabilities);

                result.newExtendedStates.put(curState, extendedState);
            }
        }
        return result;
    }

    private double transitionLogProbability(S prevState, S curState, Map<Transition<S>,
            Double> transitionLogProbabilities) {
        final Double transitionLogProbability =
                transitionLogProbabilities.get(new Transition<>(prevState, curState));
        if (transitionLogProbability == null) {
            return Double.NEGATIVE_INFINITY; // Transition has zero probability.
        } else {
            return transitionLogProbability;
        }
    }

    /**
     * Retrieves the first state of the current forward message with maximum probability.
     */
    private List<S> topRankedFinalStates(int rankLength) {
        // Otherwise an HMM break would have occurred and message would be null.
        assert !message.isEmpty();

        Queue<ItemWithProbability<S>> sortedItemList = new PriorityQueue<>();
        for (Map.Entry<S, Double> entry : message.entrySet()) {
            if (entry.getValue() > Double.NEGATIVE_INFINITY) {
                sortedItemList.add(new ItemWithProbability<>(entry.getKey(), entry.getValue()));
            }
        }
        List<S> result = new ArrayList<>();
        ItemWithProbability<S> currItem;
        int resultCount = 0;
        while ((currItem = sortedItemList.poll()) != null && resultCount < rankLength) {
            result.add(currItem.getItem());
            resultCount++;
        }

        assert !result.isEmpty(); // Otherwise an HMM break would have occurred.
        return result;
    }

    /**
     * Retrieves top k most likely sequence from the internal back pointer sequence.
     */
    private List<List<SequenceState<S, O, D>>> retrieveRankedSequences(int rankLength) {
        if (rankLength > this.rankLength)
            throw new IndexOutOfBoundsException("ERROR! Incorrect top k. The k selected is larger than what is stored.");

        // Otherwise an HMM break would have occurred and message would be null.
        assert !message.isEmpty();

        final List<S> lastState = topRankedFinalStates(rankLength);

        // Retrieve top k most likely state sequence in reverse order
        final List<List<SequenceState<S, O, D>>> result = new ArrayList<>(rankLength);
        // TODO Consider using Guava's Optional.leastOf/greatestOf instead of priority queue for better performance.
        Queue<ItemWithProbability<Pair<ExtendedState<S, O, D>, Integer>>> topRankedItems = new PriorityQueue<>();   // Triplet<current state
        // and the i-th back pointer>
        List<Pair<ExtendedState<S, O, D>, List<SequenceState<S, O, D>>>> intermediateState = new ArrayList<>(); // each pair contains the
        // ES of one of the top k candidates and its path to the end state
        for (S state : lastState) {
            ExtendedState<S, O, D> es = lastExtendedStates.get(state);
            for (int i = 0; i < es.backPointer.size(); i++) {
                topRankedItems.add(new ItemWithProbability<>(new Pair<>(es, i), es.probabilities.get(i)));
            }
        }
        int candidateCount = 0;
        ItemWithProbability<Pair<ExtendedState<S, O, D>, Integer>> currItem;
        // TODO check correctness
        // extract the top k previous state among all current candidate, stored in intermediateState
        while ((currItem = topRankedItems.poll()) != null && candidateCount < rankLength) {
            ExtendedState<S, O, D> currES = currItem.getItem()._1();
            List<SequenceState<S, O, D>> currRouteSequence = new ArrayList<>();
            SequenceState<S, O, D> currRouteState = new SequenceState<>(currES.state, currES.observation, currES.transitionDescriptor.get
                    (currItem.getItem()._2()));
            currRouteSequence.add(currRouteState);
            intermediateState.add(new Pair<>(currES.backPointer.get(currItem.getItem()._2()), currRouteSequence));
            candidateCount++;
        }
        while (!intermediateState.isEmpty()) {
            Queue<ItemWithProbability<Triplet<ExtendedState<S, O, D>, List<SequenceState<S, O, D>>, Integer>>> topRankedIntermediateResult =
                    new PriorityQueue<>();
            for (Pair<ExtendedState<S, O, D>, List<SequenceState<S, O, D>>> item : intermediateState) {
                List<ExtendedState<S, O, D>> backPointer = item._1().backPointer;
                for (int i = 0; i < backPointer.size(); i++) {
                    ExtendedState<S, O, D> candidate = backPointer.get(i);
                    SequenceState<S, O, D> currRouteState = new SequenceState<>(item._1().state, item._1().observation, item._1()
                            .transitionDescriptor.get(i));
                    List<SequenceState<S, O, D>> currRouteSequence = new ArrayList<>(item._2());
                    currRouteSequence.add(currRouteState);
                    topRankedIntermediateResult.add(new ItemWithProbability<>(new Triplet<>(candidate, currRouteSequence, i), item._1()
                            .probabilities.get(i)));
                }
            }
            if (topRankedIntermediateResult.isEmpty()) {
                // the full path has been retrieved, stop the loop and recover the top k paths
                for (Pair<ExtendedState<S, O, D>, List<SequenceState<S, O, D>>> r : intermediateState) {
                    List<SequenceState<S, O, D>> rankedResult = r._2();
                    rankedResult.add(new SequenceState<>(r._1().state, r._1().observation, null));
                    Collections.reverse(rankedResult);
                    result.add(rankedResult);
                }
                return result;
            }
            int currCandidateCount = 0;
            ItemWithProbability<Triplet<ExtendedState<S, O, D>, List<SequenceState<S, O, D>>, Integer>> currNewItem;
            intermediateState.clear();
            while ((currNewItem = topRankedIntermediateResult.poll()) != null && currCandidateCount < rankLength) {
                intermediateState.add(new Pair<>(currNewItem.getItem()._1(), currNewItem.getItem()._2()));
                currCandidateCount++;
            }
        }
        System.out.println("ERROR! No match result can be extracted");
        return result;
    }
}