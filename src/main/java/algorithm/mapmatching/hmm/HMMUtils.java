/**
 * Copyright (C) 2015, BMW Car IT GmbH
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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation utilities.
 */
class HMMUtils implements Serializable {

    static int initialHashMapCapacity(int maxElements) {
        // Default load factor of HashMaps is 0.75
        return (int) (maxElements / 0.75) + 1;
    }

    static <S> Map<S, Double> logToNonLogProbabilities(Map<S, Double> logProbabilities) {
        final Map<S, Double> result = new LinkedHashMap<>();
        for (Map.Entry<S, Double> entry : logProbabilities.entrySet()) {
            result.put(entry.getKey(), Math.exp(entry.getValue()));
        }
        return result;
    }

    /**
     * Note that this check must not be used for probability densities.
     */
    static boolean probabilityInRange(double probability, double delta) {
        return probability >= -delta && probability <= 1.0 + delta;
    }

}
