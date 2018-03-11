package traminer.util.graph.path;

public class Vertex implements Comparable<Vertex> {

    private Integer id;
    private Double distance;
    public double x;
    public double y;

    public Vertex(int id, double distance) {
        super();
        this.id = id;
        this.distance = distance;
        this.x = 0;
        this.y = 0;
    }

    public Vertex(int id, double distance, double x, double y) {
        super();
        this.id = id;
        this.distance = distance;
        this.x = x;
        this.y = y;
    }


    public int getId() {
        return id;
    }

    public double getDistance() {
        return distance;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((distance == null) ? 0 : distance.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Vertex other = (Vertex) obj;
        if (distance == null) {
            if (other.distance != null)
                return false;
        } else if (!distance.equals(other.distance))
            return false;
        if (id == null) {
            return other.id == null;
        } else return id.equals(other.id);
    }

    @Override
    public String toString() {
        return "Vertex [id=" + id + ", distance=" + distance + "]";
    }

    @Override
    public int compareTo(Vertex o) {
        if (this.distance < o.distance)
            return -1;
        else if (this.distance > o.distance)
            return 1;
        else
            return this.getId() > o.getId() ? 1 : -1;
    }

}
