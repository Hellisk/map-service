package algorithm.mapmatching.simpleHMM;


import java.util.ArrayList;
import java.util.List;

/**
 * State transition between matching candidates in Hidden Markov Model (HMM) map matching and
 * contains a route between respective map positions.
 */
public class StateTransition {

    List<String> route = new ArrayList<>();
    double transporb = 0d;

    public StateTransition() {
    }

    public List<String> getRoute() {
        return route;
    }

    public void setRoute(List<String> route) {
        this.route = route;
    }

    public double getTransporb() {
        return transporb;
    }

    public void setTransporb(double transporb) {
        this.transporb = transporb;
    }
}
