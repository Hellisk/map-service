package traminer.util.trajectory.segmentation;

import traminer.util.exceptions.EmptyTrajectoryException;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.spatial.structures.SpatialIndexModel;
import traminer.util.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Segments a trajectory based on spatial partition boundaries,
 * e.g. grid partitions, quad-tree, kd-tree, etc.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class BySpaceBoundarySegmentation extends TrajectorySegmentation {
    private final SpatialIndexModel spatialIndex;
    private final boolean relicateBoundary;

    /**
     * @param spatialIndex      The spatial index/partitioning
     *                          structure to perform the segmentation with.
     * @param replicateBoundary Whether or not to replicate
     *                          boundary segments.
     * @throws IllegalArgumentException
     */
    public BySpaceBoundarySegmentation(
            SpatialIndexModel spatialIndex,
            boolean replicateBoundary) {
        if (spatialIndex.isEmpty()) {
            throw new IllegalArgumentException(
                    "Segmentation error. Spatial Index must not be empty.");
        }
        this.spatialIndex = spatialIndex;
        this.relicateBoundary = replicateBoundary;
    }

    @Override
    public List<Trajectory> doSegmentation(Trajectory trajectory) {
        if (trajectory.isEmpty()) {
            throw new EmptyTrajectoryException(
                    "Segmentation error. Trajectory must not be empty.");
        }
        List<Trajectory> result =
                new ArrayList<Trajectory>();
        Trajectory sub = new Trajectory();
        sub.setParentId(trajectory.getId());
        STPoint prev = null;
        String partitionId = "";
        String prevPartitionId = "";
        for (STPoint p : trajectory) {
            // the partition containing p
            partitionId = spatialIndex.search(p.x(), p.y());
            if (prev == null) {
                sub.add(p);
                sub.putAttribute("partitionId", partitionId);
            } else if (partitionId == null) {
                prev = p;
                continue;
            } else if (partitionId.equals(prevPartitionId)) {
                sub.add(p);
            } else { // boundary
                if (relicateBoundary) {
                    sub.add(p);
                }
                result.add(sub);
                sub = new Trajectory();
                sub.setParentId(trajectory.getId());
                sub.putAttribute("partitionId", partitionId);
                if (relicateBoundary) {
                    sub.add(prev);
                }
                sub.add(p);
            }
            prevPartitionId = partitionId;
            prev = p;
        }
        // add last sub-trajectory
        result.add(sub);

        return result;
    }

    /**
     * Segments a trajectory based on space boundaries. Return a
     * HashMap (key,value) containing the sub-trajectory as key,
     * and the partition index as value.
     *
     * @author uqdalves
     */
    public HashMap<Trajectory, String> doSegmentationByPartition(Trajectory t) {
        HashMap<Trajectory, String> result = new HashMap<>();

        int subCount = 1;
        Trajectory sub = new Trajectory(t.getId() + "_" + subCount++);
        sub.setParentId(t.getId());

        STPoint prev = null;
        String partitionId = "";
        String prevPartitionId = "";
        for (STPoint p : t) {
            // the partition containing p
            partitionId = spatialIndex.search(p.x(), p.y());
            if (prev == null) {
                sub.add(p);
            } else if (partitionId.equals(prevPartitionId)) {
                sub.add(p);
            } else { // boundary
                if (relicateBoundary) {
                    sub.add(p);
                }
                result.put(sub, prevPartitionId);
                sub = new Trajectory(t.getId() + "_" + subCount++);
                sub.setParentId(t.getId());
                if (relicateBoundary) {
                    sub.add(prev);
                }
                sub.add(p);
            }
            prevPartitionId = partitionId;
            prev = p;
        }
        // add last sub-trajectory
        result.put(sub, partitionId);

        return result;
    }
}
