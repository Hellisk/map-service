package algorithm.mapmatching.simpleHMM;


import java.util.ArrayList;
import java.util.List;

/**
 * State transition between matching candidates in Hidden Markov Model (HMM) map matching and
 * contains a route between respective map positions.
 */
public class StateTransition {

    List<String> route = new ArrayList<>();

    public StateTransition() {
    }

    public StateTransition(List<String> route) {
        this.route = route;
    }

    public List<String> getRoute() {
        return route;
    }

    public void setRoute(List<String> route) {
        this.route = route;
    }
}
