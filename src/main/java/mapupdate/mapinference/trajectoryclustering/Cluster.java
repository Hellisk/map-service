package mapupdate.mapinference.trajectoryclustering;

import mapupdate.util.function.HausdorffDistanceFunction;
import mapupdate.util.object.spatialobject.Trajectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Data structure for storing trajectory cluster.
 */
public class Cluster {
    private List<Trajectory> itemList;
    private String id;

    Cluster(String id) {
        this.id = id;
        this.itemList = new ArrayList<>();
    }

    Cluster(String id, Trajectory traj) {
        this.id = id;
        this.itemList = new ArrayList<>();
        itemList.add(traj);
    }

    Cluster(String id, List<Trajectory> trajList) {
        this.id = id;
        this.itemList = trajList;
    }

    public List<Trajectory> getTrajectoryList() {
        return itemList;
    }

    public Trajectory getTraj(int index) {
        return itemList.get(index);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void add(Trajectory traj) {
        this.itemList.add(traj);
    }

    void merge(Cluster cluster) {
        this.itemList.addAll(cluster.itemList);
    }

    double getDistance(Trajectory traj, HausdorffDistanceFunction distFunc) {
        double minDistance = Double.POSITIVE_INFINITY;
        for (Trajectory currTraj : this.itemList) {
            double currDistance = distFunc.distance(currTraj, traj);
            minDistance = minDistance > currDistance ? currDistance : minDistance;
        }
        if (minDistance == Double.POSITIVE_INFINITY)
            throw new NullPointerException("ERROR! The distance between a cluster and a trajectory is infinity: " + this.id);
        return minDistance;
    }

    int size() {
        return itemList.size();
    }

    boolean isEmpty() {
        return itemList.isEmpty();
    }
}
