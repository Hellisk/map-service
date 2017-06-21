package traminer.util.map.matching.stmatching;

/**
 * ST-Matching auxiliary road segment object
 *
 * @author uqdalves
 */
class CandidateSegment {
    private CandidateNode startNode, endNode;
    private boolean isOneWay = false;

    public CandidateSegment(
            CandidateNode startNode,
            CandidateNode endNode,
            boolean isOneWay) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.isOneWay = isOneWay;
    }

    public CandidateNode getStartNode() {
        return startNode;
    }

    public CandidateNode getEndNode() {
        return endNode;
    }

    public boolean isOneWay() {
        return isOneWay;
    }

    public boolean equals(CandidateSegment AnotherSegment) {
        if (this.startNode.equals(AnotherSegment.getStartNode())) {
            if (this.endNode.equals(AnotherSegment.getEndNode())) {
                return true;
            }
        }
        return false;
    }
}