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
Filename: PBDEdge.java
 */
package evaluation.mapevaluation.pathbaseddistance.mapmatchingbasics;

public class PBDEdge {
	
	public PBDVertex v1, v2;
	public double cstart, cend;
	public double vstart, vend;
	public PBDLine line;
	public boolean done;
	public int startIndex, endIndex;
	
	public PBDEdge(PBDVertex v1, PBDVertex v2) {
		this.v1 = v1;
		this.v2 = v2;
		this.cstart = Double.MAX_VALUE;
		this.cend = -1;
		this.vstart = Double.MAX_VALUE;
		this.vend = -1;
		this.line = new PBDLine(v1, v2);
		this.done = false;
	}
	
	public void reset() {
		this.cstart = Double.MAX_VALUE;
		this.cend = -1;
		this.vstart = Double.MAX_VALUE;
		this.vend = -1;
		this.line = new PBDLine(v1, v2);
		this.done = false;
	}
	
	public String toString() {
		return this.line.toString();
	}
	
	public String getKeyString() {
		return v1.x + " " + v2.x + " " + v1.y + " " + v2.y;
	}
}
