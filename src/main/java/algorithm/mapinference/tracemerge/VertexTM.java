package algorithm.mapinference.tracemerge;

import util.function.DistanceFunction;

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
 * An object that represents a 2D point.
 *
 * @author Mahmuda Ahmed
 */
public class VertexTM {
	
	private final DistanceFunction distFunc;
	private double x; // x coordinate(meters) after Mercator projection
	private double y; // y coordinate(meters) after Mercator projection
	private double lat; // latitude in WGS84
	private double lng; // longitude in WGS84
	/**
	 * Contains the indices of adjacent vertices.
	 */
	private List<Integer> adjacencyList;
	
	private boolean done = false;
	
	/**
	 * Timestamp in milliseconds, this field is used when a pose is represented as a list of vertices.
	 */
	private double timestamp = -1;
	
	// TODO(Mahmuda): Better to have static factory methods instead of constructor overloading.
	
	public VertexTM(DistanceFunction df) {
		this.adjacencyList = new ArrayList<>();
		this.done = false;
		this.distFunc = df;
	}
	
	public VertexTM(double x, double y, DistanceFunction df) {
		this(df);
		this.x = x;
		this.y = y;
	}
	
	public VertexTM(double x, double y, double timestamp, DistanceFunction df) {
		this(x, y, df);
		this.timestamp = timestamp;
	}
	
	public VertexTM(double lat, double lng, double x, double y, DistanceFunction df) {
		this(x, y, df);
		this.lat = lat;
		this.lng = lng;
	}
	
	public VertexTM(double lat, double lng, double x, double y, double timestamp, DistanceFunction df) {
		this(lat, lng, x, y, df);
		this.timestamp = timestamp;
	}
	
	static double dotProd(VertexTM vector1, VertexTM vector2) {
		return vector1.getX() * vector2.getX() + vector1.getY() * vector2.getY();
	}
	
	public double getX() {
		return this.x;
	}
	
	public double getY() {
		return this.y;
	}
	
	public double getLat() {
		return this.lat;
	}
	
	public double getLon() {
		return this.lng;
	}
	
	public DistanceFunction getDistanceFunction() {
		return distFunc;
	}
	
	public double norm() {
		return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
	}
	
	public int getDegree() {
		return this.adjacencyList.size();
	}
	
	public boolean getDone() {
		return this.done;
	}
	
	public void setDone(boolean done) {
		this.done = done;
	}
	
	public double getTimestamp() {
		return this.timestamp;
	}
	
	List<Integer> getAdjacencyList() {
		return this.adjacencyList;
	}
	
	/**
	 * Adds an element to its adjacency list.
	 *
	 * @param v is the value to be added in adjacency list
	 */
	void addElementAdjList(int v) {
		for (int i = 0; i < this.getDegree(); i++) {
			if (this.adjacencyList.get(i) == v) {
				return;
			}
		}
		
		this.adjacencyList.add(v);
	}
	
	/**
	 * Returns the index of a vertex in the adjacency list.
	 *
	 * @param v the vertex we are looking for
	 * @return an int, the index of vertex k if found or -1 otherwise
	 */
	public int getIndexAdjacent(int v) {
		return this.adjacencyList.indexOf(v);
	}
	
	/**
	 * Returns the value in the adjacency list at index k
	 *
	 * @param k the index
	 * @return an int, the value at index k or -1 otherwise
	 */
	
	int getAdjacentElementAt(int k) {
		return this.adjacencyList.get(k);
	}
	
	/**
	 * Set the adjacent vertex as value at index
	 *
	 * @param index the index to update
	 * @param value the new value at index
	 */
	
	void setAdjacentElementAt(int index, int value) {
		this.adjacencyList.remove(index);
		this.adjacencyList.add(index, value);
	}
	
	/**
	 * Computes distance between two vertices.
	 *
	 * @param v2 the vertex with which we should compute distance from this vertex
	 * @return a double value which is the distance
	 */
	
	public double dist(VertexTM v2) {
		return distFunc.pointToPointDistance(this.x, this.y, v2.x, v2.y);
	}
	
	/**
	 * Resets a vertex's processing state.
	 */
	
	public void reset() {
		done = false;
	}
	
	@Override
	public String toString() {
		return String.format("%f %f", this.x, this.y);
	}
	
	/**
	 * @return a deep copy of this vertex
	 */
	public VertexTM deepCopy() {
		VertexTM vertex = new VertexTM(this.lat, this.lng, this.x, this.y, this.timestamp, this.distFunc);
		vertex.done = this.done;
		vertex.adjacencyList.addAll(this.adjacencyList);
		return vertex;
	}
	
}
