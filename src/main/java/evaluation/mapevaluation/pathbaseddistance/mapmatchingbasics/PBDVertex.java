/*
Path-based graph distance 1.0
Copyright 2014 Mahmuda Ahmed, K. S. Hickmann and Carola Wenk

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

------------------------------------------------------------------------

This software is based on the following article. Please cite this
article when using this code as part of a research publication:

M. Ahmed, K. S. Hickmann, and C. Wenk.
Path-based distance for street map comparison.
arXiv:1309.6131, 2013.
------------------------------------------------------------------------

Author: Mahmuda Ahmed
Filename: PBDVertex.java
 */
package evaluation.mapevaluation.pathbaseddistance.mapmatchingbasics;

import java.io.Serializable;

public class PBDVertex implements Serializable {
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public String vertexIDOnFile;
	public int vertexID;
	public double x, y, lat, lon;
	public int degree = 0;
	public int[] adjacencyList = new int[25];
	public double left = Double.MAX_VALUE, right = -1;//left and right endpoint of reachability interval or left is used as path label while
	// computing shortest Paths.
	public int startindex = -1;
	public int endindex = 0;
	public boolean done = false;
	public boolean reachable = false;
	//	int paths[] = new int[10];
	
	public PBDVertex() {
		
	}
	
	public PBDVertex(String vertexIDOnFile, double lat, double lon, double x, double y) {
		this.vertexIDOnFile = vertexIDOnFile;
		this.lat = lat;
		this.lon = lon;
		this.x = x;
		this.y = y;
		reachable = false;
	}
	
	public PBDVertex(String vertexIDOnFile, double x, double y) {
		this.vertexIDOnFile = vertexIDOnFile;
		this.x = x;
		this.y = y;
	}
	
	
	public PBDVertex(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public void addElementAdjList(int v) {
		for (int i = 0; i < this.degree; i++)
			if (adjacencyList[i] == v) return;
		
		adjacencyList[this.degree] = v;
		this.degree++;
	}
	
	public String toString() {
		//return this.vertexID+" "+this.x+" "+this.y+" "+this.lat+" "+this.lon+" ";
		return this.vertexIDOnFile + " " + this.degree + " " + this.x + " " + this.y + " " + this.adjacencyListToString() + "\n";
	}
	
	public int getIndexAdjacent(int k) {
		for (int i = 0; i < this.degree; i++) {
			if (this.adjacencyList[i] == k) return i;
			
		}
		return -1;
	}
	
	/*public boolean checkDone()
	{
		for(int i=0;i<degree;i++)if( paths[i] == 0 )return false;
		return true;
	}
	public int nextVertex()
	{
		for(int i=0;i<degree;i++)if( paths[i] == 0 ){ return i;}
		return -1;
	}*/
	public int getAdjacentVertexAt(int index) {
		return this.adjacencyList[index];
	}
	
	public void setAdjacentVertexAt(int index, int adjacentVertex) {
		this.adjacencyList[index] = adjacentVertex;
	}
	
	public double dist(PBDVertex v2) {
		return Math.sqrt(Math.pow(this.x - v2.x, 2) + Math.pow(this.y - v2.y, 2));
	}
	
	public void removeDuplicates() {
		for (int i = 0; i < this.degree; i++) {
			for (int j = i + 1; j < this.degree; j++) {
				if (this.adjacencyList[i] == this.adjacencyList[j]) {
					if (this.degree - 1 - j >= 0)
						System.arraycopy(this.adjacencyList, j + 1, this.adjacencyList, j, this.degree - 1 - j);
					this.degree--;
					//System.out.println("duplicate removed"+this.degree);
				}
				
			}
		}
		
	}
	
	private String adjacencyListToString() {
		StringBuilder data = new StringBuilder();
		for (int i = 0; i < this.degree; i++) {
			data.append(this.adjacencyList[i]).append(" ");
		}
		return data.toString();
	}
	
	public void reset() {
		left = Double.MAX_VALUE;
		right = -1;
		startindex = -1;
		endindex = 0;
		//paths = new int[10];
		done = false;
		reachable = false;
	}
	
	public String getKeyString() {
		return this.x + " " + this.y;
	}
	
	public void mergeAdjacencyList(PBDVertex vertex) {
		for (int i = 0; i < vertex.degree; i++) {
			this.addElementAdjList(vertex.adjacencyList[i]);
		}
	}
	
	public PBDVertex deepCopy() {
		PBDVertex newVertex = new PBDVertex(this.vertexIDOnFile, this.lat, this.lon, this.x, this.y);
		newVertex.reset();
		newVertex.mergeAdjacencyList(this);
		return newVertex;
	}
	
}
