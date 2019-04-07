package mapupdate.util.object.datastructure;

public class LengthBasedItem {
    private double length;
    private String roadID;

    LengthBasedItem() {
        this.length = 0;
        this.roadID = "";
    }

    LengthBasedItem(double length, String roadID) {
        this.length = length;
        this.roadID = roadID;
    }

    public double getLength() {
        return length;
    }

    public String getID() {
        return this.roadID;
    }

    public void setID(String roadID) {
        this.roadID = roadID;
    }
}
