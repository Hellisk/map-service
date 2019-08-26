package algorithm.mapmatching.simpleHMM;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * State transition between matching candidates in Hidden Markov Model (HMM) map matching and
 * contains a route between respective map positions.
 */
public class StateTransition {

    private List<String> route = new ArrayList<>();

    public StateTransition() {
    }

    public StateTransition(List<String> route) {
        this.route = route;
    }

    public List<String> getRoute() {
        return route;
    }

//    public void setRoute(List<String> route) {
//        this.route = route;
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateTransition that = (StateTransition) o;
        return route.equals(that.getRoute());
    }

    @Override
    public int hashCode() {
        return Objects.hash(route);
    }
}
