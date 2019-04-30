package algorithm.mapinference.tracemerge;

import java.util.ArrayList;
import java.util.List;

/**
 * Frechet-based map construction 2.0 Copyright 2013 Mahmuda Ahmed and Carola Wenk
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * ------------------------------------------------------------------------
 * <p>
 * This software is based on the following article. Please cite this article when using this code
 * as part of a research publication:
 * <p>
 * Mahmuda Ahmed and Carola Wenk, "Constructing Street Networks from GPS Trajectories", European
 * Symposium on Algorithms (ESA): 60-71, Ljubljana, Slovenia, 2012
 * <p>
 * ------------------------------------------------------------------------
 * <p>
 * An object that represents an edge from vertex1 to vertex2.
 *
 * @author Mahmuda Ahmed, Hellisk
 */
class EdgeTM implements Comparable<EdgeTM> {
	
	private VertexTM vertex1; // first endpoint of edge
	private VertexTM vertex2; // second endpoint of edge
	private double curveStart; // contains start point of white interval on curve
	private double curveEnd; // contains end point of white interval on curve
	private double edgeStart; // contains corresponding points on that edge
	private double edgeEnd; // contains corresponding points on that edge
	private LineTM line; // line from vertex v1 to v2
	/**
	 * done is marked as true when an edge is done being compared with all segments of a curve.
	 */
	private boolean done;
	/**
	 * Contains start and end index of curves for white intervals corresponding to the edge.
	 */
	private int curveStartIndex;
	private int curveEndIndex;
	
	/**
	 * Contains a sorted list (in descending order) of positions indicating where to split this edge.
	 */
	private List<Double> edgeSplitPositions;
	
	/**
	 * For each split position the corresponding new vertex is saved in this list.
	 */
	private List<Integer> edgeSplitVertices;
	
	// private static final FormattingLogger logger = FormattingLogger.getLogger(EdgeTM.class);
	
	EdgeTM(VertexTM v1, VertexTM v2) {
		this.vertex1 = v1;
		this.vertex2 = v2;
		edgeSplitPositions = new ArrayList<>();
		edgeSplitVertices = new ArrayList<>();
		this.reset();
	}
	
	private void reset() {
		this.curveStart = Double.MAX_VALUE;
		this.curveEnd = -1.0;
		this.edgeStart = Double.MAX_VALUE;
		this.edgeEnd = -1.0;
		this.line = new LineTM(vertex1, vertex2, vertex1.getDistanceFunction());
		this.done = false;
	}
	
	void set(EdgeTM edge) {
		this.curveStart = edge.curveStart;
		this.curveEnd = edge.curveEnd;
		this.edgeStart = edge.edgeStart;
		this.edgeEnd = edge.edgeEnd;
		this.line = new LineTM(vertex1, vertex2, vertex1.getDistanceFunction());
		this.done = edge.done;
	}
	
	VertexTM getVertex1() {
		return this.vertex1;
	}
	
	VertexTM getVertex2() {
		return this.vertex2;
	}
	
	public LineTM getLine() {
		return this.line;
	}
	
	public boolean getDone() {
		return this.done;
	}
	
	public void setDone(boolean done) {
		this.done = done;
	}
	
	double getCurveStart() {
		return this.curveStart;
	}
	
	void setCurveStart(double cStart) {
		this.curveStart = cStart;
	}
	
	double getCurveEnd() {
		return this.curveEnd;
	}
	
	void setCurveEnd(double cEnd) {
		this.curveEnd = cEnd;
	}
	
	double getEdgeStart() {
		return this.edgeStart;
	}
	
	void setEdgeStart(double vStart) {
		this.edgeStart = vStart;
	}
	
	double getEdgeEnd() {
		return this.edgeEnd;
	}
	
	void setEdgeEnd(double vend) {
		this.edgeEnd = vend;
	}
	
	public int getCurveStartIndex() {
		return this.curveStartIndex;
	}
	
	void setCurveStartIndex(int startIndex) {
		if (startIndex >= 0) {
			this.curveStartIndex = startIndex;
		} else {
			// logger.log(Level.SEVERE, "Invalid assignment of EdgeTM.startIndex");
		}
	}
	
	int getCurveEndIndex() {
		return this.curveEndIndex;
	}
	
	void setCurveEndIndex(int endIndex) {
		this.curveEndIndex = endIndex;
	}
	
	List<Integer> getEdgeSplitVertices() {
		return this.edgeSplitVertices;
	}
	
	List<Double> getEdgeSplitPositions() {
		return this.edgeSplitPositions;
	}
	
	/**
	 * Inserts a new split position if the list doesn't have it, otherwise return.
	 *
	 * @param position indicate the new split position
	 * @param vertex   indicates the vertex which should be inserted in this edge.
	 */
	
	void addSplit(double position, int vertex) {
		int i = 0;
		//logger.log(Level.FINEST, "Inside updateSplits");
		for (i = 0; i < this.edgeSplitPositions.size(); i++) {
			if (this.edgeSplitPositions.get(i) == position) {
				return;
			} else if (this.edgeSplitPositions.get(i) > position) {
				this.edgeSplitPositions.add(i, position);
				this.edgeSplitVertices.add(i, vertex);
				return;
			}
		}
		this.edgeSplitPositions.add(position);
		this.edgeSplitVertices.add(vertex);
	}
	
	@Override
	public String toString() {
		return vertex1.toString() + vertex2.toString();
	}
	
	
	/**
	 * Orders edges based on first their curveStart and then curveEnd.
	 */
	@Override
	public int compareTo(EdgeTM edge) {
		
		if (this.getCurveStart() < edge.getCurveStart()) {
			return -1;
		} else if (this.getCurveStart() > edge.getCurveStart()) {
			return 1;
		} else return Double.compare(this.getCurveEnd(), edge.getCurveEnd());
	}
	
	
}
