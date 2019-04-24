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

import util.object.structure.PointMatch;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Represents the road path between two consecutive road positions.
 */
@SuppressWarnings("serial")
public class RoadPath implements Serializable {
    // The following members are used to check whether the correct road paths are retrieved
    // from the most likely sequence
    public final PointMatch from;
    public final PointMatch to;
    public final List<String> passingRoadID;

    /**
     * Creates a new road path between the two given road node.
     *
     * @param from Origin node.
     * @param to   destiny node.
     */
    RoadPath(PointMatch from, PointMatch to, List<String> roadIdList) {
        this.from = from;
        this.to = to;
        this.passingRoadID = roadIdList;
    }

    public void setPassingRoads(List<String> ids) {
        this.passingRoadID.addAll(ids);
    }

    public List<String> getPassingRoadID() {
        return this.passingRoadID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RoadPath other = (RoadPath) obj;
        if (from == null) {
            if (other.from != null)
                return false;
        } else if (!from.equals(other.from))
            return false;
        if (to == null) {
            return other.to == null;
        } else return to.equals(other.to);
    }
}