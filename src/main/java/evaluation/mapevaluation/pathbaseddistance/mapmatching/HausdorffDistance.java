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
Filename: HausdorffDistance.java
 */
package evaluation.mapevaluation.pathbaseddistance.mapmatching;

import evaluation.mapevaluation.pathbaseddistance.mapmatchingbasics.IntervalComparatorEdge;
import evaluation.mapevaluation.pathbaseddistance.mapmatchingbasics.PBDEdge;
import evaluation.mapevaluation.pathbaseddistance.mapmatchingbasics.PBDLine;
import evaluation.mapevaluation.pathbaseddistance.mapmatchingbasics.PBDVertex;

import java.io.*;
import java.util.*;

public class HausdorffDistance {
	
	public boolean found;
	public double min;
	
	public ArrayList<PBDVertex> readFile(String fileName) {
		
		ArrayList<PBDVertex> curves = new ArrayList<>();
		try {
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			String str;
			while ((str = in.readLine()) != null) {
				StringTokenizer strToken = new StringTokenizer(str);
				double d = Double.parseDouble(strToken.nextToken());
				double p1 = d;
				d = Double.parseDouble(strToken.nextToken());
				double p2 = d;
				
				curves.add(new PBDVertex(p1, p2));
				
			}
			in.close();
			
		} catch (IOException e) {
			System.out.println(e.toString());
			System.exit(0);
		}
		
		return curves;
	}
	
	public boolean computeInterval(PBDEdge e, ArrayList<PBDVertex> curves, int cur, double eps) {
		
		PBDLine line = new PBDLine(curves.get(cur - 1), curves.get(cur));
		return line.pIntersection(e, eps);
	}
	
	public void computeNextInterval(PBDEdge e, ArrayList<PBDVertex> curves, int newstart, double eps) {
		// startintex-----interval--------endindex
		
		boolean debug = false;
		
		boolean first = true;
		
		int startIndex = 0;
		double cstart = 0, vstart = 0;
		
		if (newstart >= curves.size()) {
			e.endIndex = curves.size();
			e.done = true;
			return;
		}
		
		for (int i = newstart; i < curves.size(); i++) {
			boolean result = computeInterval(e, curves, i, eps);
			
			if (debug && first && result)
				System.out.println("i (next interval) = " + i + "  "
						+ (i - 1 + e.cstart) + " " + (i - 1 + e.cend) + " "
						+ first + " " + result);
			if (first && result) {
				startIndex = i - 1;
				cstart = e.cstart;
				vstart = e.vstart;
				
				first = false;
				
				if (e.cend < 1) {
					e.startIndex = startIndex;
					e.cstart = startIndex + cstart;
					e.vstart = vstart;
					e.cend = i - 1 + e.cend;
					e.endIndex = i;
					return;
				}
			} else if (!first && result) {
				if (e.cend < 1) {
					e.startIndex = startIndex;
					e.cstart = startIndex + cstart;
					e.vstart = vstart;
					e.cend = i - 1 + e.cend;
					e.endIndex = i;
					return;
				}
			} else if (!first && !result) {
				e.startIndex = startIndex;
				e.cstart = startIndex + cstart;
				e.vstart = vstart;
				e.cend = i - 1 + e.cend;
				e.endIndex = i;
				return;
			}
			
		}
		
		if (first) {
			e.endIndex = curves.size();
			e.done = true;
		} else {
			e.startIndex = startIndex;
			e.cstart = startIndex + cstart;
			e.vstart = vstart;
			
			e.cend = curves.size() - 2 + e.cend;
			e.endIndex = curves.size() - 2;
		}
		
	}
	
	public boolean mapMatchingHausdorff(ArrayList<PBDEdge> graph, ArrayList<PBDVertex> curves, double eps) {
		boolean debug = false;
		
		Comparator<PBDEdge> comparator = new IntervalComparatorEdge();
		PriorityQueue<PBDEdge> pq = new PriorityQueue<>(21282, comparator);
		
		for (PBDEdge pbdEdge : graph) {
			
			this.computeNextInterval(pbdEdge, curves, 1, eps);
			
			if (!pbdEdge.done) {
				
				pq.add(pbdEdge);
			}
		}
		if (pq.isEmpty())
			return false;
		
		PBDEdge e = pq.poll();
		if (debug)
			System.out.println(curves.size() + "  " + e.cstart + " " + e.cend);
		double cend = e.cend;
		if (e.cstart > 0)
			return false;
		while (cend < curves.size()) {
			if (debug)
				System.out.println("interval " + e.cstart + " " + e.cend + " "
						+ cend);
			
			if (cend < e.cend) {
				cend = e.cend;
			}
			
			if (e.cend == curves.size() - 1)
				return true;
			
			this.computeNextInterval(e, curves, e.endIndex + 1, eps);
			
			if (!e.done)
				pq.add(e);
			
			if (pq.isEmpty()) {
				if (debug)
					System.out.println(cend + " queue empty.");
				return false;
			}
			
			e = pq.poll();
			
			if (e.cstart > cend) {
				if (debug)
					System.out.println("black interval " + e.cstart + " "
							+ cend);
				return false;
			}
			
		}
		
		return true;
	}
	
	public void getEpsilon(ArrayList<PBDEdge> graph, ArrayList<PBDVertex> curves, int start, int end) {
		int i;
		boolean bool1;
		boolean debug = false;
		
		if (start >= end - 2) {
			found = true;
			for (i = start; i <= end; i++) {
				int k = 0;
				while (k < graph.size()) {
					graph.get(k).reset();
					k++;
				}
				bool1 = this.mapMatchingHausdorff(graph, curves, i);
				if (debug)
					System.out.println("here " + start + ", " + end + " " + i + " = " + bool1);
				if (bool1) {
					min = i;
					return;
				}
			}
			min = i;
		} else {
			
			i = 0;
			while (i < graph.size()) {
				graph.get(i).reset();
				i++;
			}
			bool1 = this.mapMatchingHausdorff(graph, curves, (end + start) / 2);
			if (debug)
				System.out.println("here2 " + start + ", " + end + " " + (end + start) / 2 + " = " + bool1 + " found=" + found);
			
			if (!bool1) {
				getEpsilon(graph, curves, (int) Math.ceil((end + start) / 2), end);
			} else {
				
				getEpsilon(graph, curves, start, (int) Math.ceil((end + start) / 2));
			}
		}
	}
	
	public void pathSimilarity(ArrayList<PBDEdge> graph, File fin, String strInput, int fileNo) {
		boolean debug = false;
		ArrayList<PBDVertex> curves;
		int min = 1, max = 1600;
		File file1 = new File(strInput);
		if (!file1.exists())
			file1.mkdirs();
		try {
			
			BufferedWriter bwWays = new BufferedWriter(new FileWriter(strInput + "/outputHausdorff" + fileNo + ".txt"));
			int count = 0;
			for (File file : Objects.requireNonNull(fin.listFiles())) {
				
				curves = this.readFile(file.getAbsolutePath());
				
				if (debug)
					System.out.println(curves.size()
							+ " :"
							+ this.mapMatchingHausdorff(graph, curves, 8));
				else {
					if (curves.size() < 2) {
						continue;
					}
					
					this.found = false;
					this.getEpsilon(graph, curves, min, max);
					
					if (this.min == max + 1) {
						this.found = false;
						int i = 0;
						while (i < graph.size()) {
							graph.get(i).reset();
							i++;
						}
						this.getEpsilon(graph, curves, max, max + 800);
					}
				}
				
				double dist = Math.sqrt(Math.pow(
						curves.get(0).x - curves.get(curves.size() - 1).x, 2)
						+ Math.pow(
						curves.get(0).y
								- curves.get(curves.size() - 1).y, 2));
				bwWays.write(file.getName() + " " + curves.size() + " "
						+ this.min + " " + dist + "\n");
				
				System.out.println(count + "  " + file.getName() + " size = "
						+ curves.size() + " Hausdorff distance = " + this.min
						+ " length of path = " + dist);
				count++;
			}
			bwWays.close();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}
	
	public ArrayList<PBDEdge> getGraphEdge(ArrayList<PBDVertex> vList) {
		ArrayList<PBDEdge> graph = new ArrayList<>();
		
		for (PBDVertex v1 : vList) {
			for (int j = 0; j < v1.degree; j++) {
				PBDVertex v2 = vList.get(v1.adjacencyList[j]);
				if (!(v1.x == v2.x && v1.y == v2.y)) {
					PBDEdge e = new PBDEdge(v1, v2);
					graph.add(e);
				}
			}
		}
		return graph;
	}
	
}
