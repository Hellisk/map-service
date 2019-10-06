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
package algorithm.mapmatching.hmm;

import algorithm.mapmatching.weightBased.Utilities;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Segment;

import java.io.Serializable;
import java.util.List;

/**
 * Based on Newson, Paul, and John Krumm. "Hidden Markov map matching through noise and sparseness."
 * Proceedings of the 17th ACM SIGSPATIAL International Conference on Advances in Geographic
 * Information Systems. ACM, 2009.
 */
public class HMMProbabilities implements Serializable {
    private final double sigma;
    private final double beta;

    /**
     * @param sigma standard deviation of the normal distribution [m] used for modeling the
     *              GPS error
     * @param beta  beta parameter of the exponential distribution for 1 s sampling interval, used
     *              for modeling transition probabilities
     */
    public HMMProbabilities(double sigma, double beta) {
        this.sigma = sigma;
        this.beta = beta;
    }

    /**
     * Returns the logarithmic emission probability density.
     *
     * @param distance Absolute distance [m] between GPS measurement and map matching candidate.
     */
    public double emissionLogProbability(double distance) {
        return Distributions.logNormalDistribution(sigma, distance);
    }

    public double emissionProbability(double distance) {
        return Distributions.normalDistribution(sigma, distance);
    }

    /**
     * Emission probability proposed by Wei Hong 2013
     *
     * @param distance distance between segment and GPS point
     * @param timeDiff t_i is time interval between t_i - t_i-1
     * @return emission probability
     */
    public double emissionProbabilityWithTime(double distance, double timeDiff) {
        return Distributions.normalDistribution(sigma, distance * Math.sqrt(timeDiff));
    }

    /**
     * Returns the logarithmic transition probability density for the given transition
     * parameters.
     *
     * @param routeLength    Length of the shortest route [m] between two consecutive map matching
     *                       candidates.
     * @param linearDistance Linear distance [m] between two consecutive GPS measurements.
     * @param timeDiff       time difference [s] between two consecutive GPS measurements.
     */
    public double transitionLogProbability(double routeLength, double linearDistance, double timeDiff) {
        double transitionMetric = normalizedTransitionMetric(routeLength, linearDistance, timeDiff);
        return Distributions.logExponentialDistribution(beta, transitionMetric);
    }

    public double transitionProbability(double routeLength, double linearDistance, double timeDiff) {
        double transitionMetric = normalizedTransitionMetric(routeLength, linearDistance, timeDiff);
        return Distributions.normalDistribution(beta, transitionMetric);
    }

    /**
     * Transition probability proposed by Takayuki Osogami (2014)
     *
     * @param routeLength
     * @param linearDistance
     * @param timeDiff
     * @param roadIDs
     * @return
     */
    public double transitionProbabilityWithTurn(double routeLength, double linearDistance, double timeDiff,
                                                List<String> roadIDs, RoadNetworkGraph roadMap, double turnWeight) {
        if (roadIDs.size() <= 1) return 0d;
        int turnCost = 0;
        RoadWay prevRoadWay = roadMap.getWayByID(roadIDs.get(0));
        for (int i = 1; i < roadIDs.size() - 1; i++) {
            Segment prevEdge = prevRoadWay.getEdges().get(prevRoadWay.getEdges().size() - 1);
            Segment curSegment = roadMap.getWayByID(roadIDs.get(i)).getEdges().get(0);
            double heading = Math.abs(Utilities.computeHeading(prevEdge.x1(), prevEdge.y1(), prevEdge.x2(),
                    prevEdge.y2()));
            double heading1 = Math.abs(Utilities.computeHeading(curSegment.x1(), curSegment.y1(), curSegment.x2(),
                    curSegment.y2()));
            double headingDiff = Math.abs(heading - heading1);
            if (headingDiff >= 45 && headingDiff <= 135) {
                turnCost += 1;
            } else if (headingDiff > 135 && headingDiff < 180) {
                turnCost += 2;
            } else if (headingDiff == 180) {
                turnCost += 10;
            }
        }
        double transitionMetric = normalizedTransitionMetric(routeLength + turnWeight * turnCost,
                linearDistance, timeDiff);
        return Distributions.normalDistribution(beta, transitionMetric);
    }

    /**
     * Returns the maximum transition probability so as to fill the breaking gap.
     *
     * @param linearDistance Linear distance [m] between two consecutive GPS measurements.
     * @param timeDiff       time difference [s] between two consecutive GPS measurements.
     */
    public double maxTransitionLogProbability(double linearDistance, double timeDiff) {
        double maxRouteLength = Math.min(50 * timeDiff, linearDistance * 8);    // limit the maximum speed to
//        double maxRouteLength = 50 * timeDiff;
        double transitionMetric = normalizedTransitionMetric(maxRouteLength, linearDistance, timeDiff);
        return Distributions.logExponentialDistribution(beta, transitionMetric);
    }

    public double getSigma() {
        return sigma;
    }

    /**
     * Returns a transition metric for the transition between two consecutive map matching
     * candidates.
     * <p>
     * In contrast to Newson & Krumm the absolute distance difference is divided by the quadratic
     * time difference to make the beta parameter of the exponential distribution independent of the
     * sampling interval.
     */
    private double normalizedTransitionMetric(double routeLength, double linearDistance, double timeDiff) {
        if (timeDiff <= 0.0) {
            throw new IllegalStateException("Time difference between subsequent location measurements must be >= 0:" + timeDiff);
        }
        return Math.abs(linearDistance - routeLength) / (timeDiff * timeDiff);
//        return Math.abs(linearDistance - routeLength) / timeDiff;
//        return Math.abs(linearDistance - routeLength);
    }
}