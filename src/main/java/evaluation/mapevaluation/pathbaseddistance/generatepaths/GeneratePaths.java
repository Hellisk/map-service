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
Filename: GeneratePaths.java
 */
package evaluation.mapevaluation.pathbaseddistance.generatepaths;

import evaluation.mapevaluation.pathbaseddistance.mapmatchingbasics.PBDVertex;
import evaluation.mapevaluation.pathbaseddistance.mapmatchingbasics.PathLabelComparator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class GeneratePaths {
	
	private void generateLineSegmentPaths(ArrayList<PBDVertex> graph, String filepath) {
		try {
			ArrayList<Integer> vList = new ArrayList<>();
			
			int maxDegree = -1, e = 0;
			for (int i = 0; i < graph.size(); i++) {
				graph.get(i).removeDuplicates();
				e = e + graph.get(i).degree;
				vList.add(i);
				if (maxDegree < graph.get(i).degree)
					maxDegree = graph.get(i).degree;
				
			}
			
			@SuppressWarnings("unchecked")
			ArrayList<PBDVertex>[][] curves = new ArrayList[vList.size()][maxDegree];
			double[][] pathLength = new double[vList.size()][maxDegree];
			
			PBDVertex prev, cur;
			int i = 0;
			
			while (i < vList.size()) {
				
				prev = graph.get(i);
				
				for (int j = 0; j < prev.degree; j++) {
					curves[i][j] = new ArrayList<>();
					curves[i][j].add(prev);
					cur = graph.get(prev.adjacencyList[j]);
					pathLength[i][j] += Math
							.sqrt((Math.pow(cur.x - prev.x, 2) + Math.pow(cur.y
									- prev.y, 2)));
					curves[i][j].add(cur);
					//boolean found1=false,found2=false;
					//if(prev.getKeyString().equals("390018.101 5817597.233"))found1=true;
					//if(cur.getKeyString().equals("390294.625 5817341.91"))found2=true;
					//if(found1&&found2)System.out.println("I have found you i="+i+" j="+prev.adjacencyList[j]+" "+prev.toString()+cur.toString());
				}
				
				i++;
				
			}
			System.out.println("PBDVertex set size: " + vList.size()
					+ " max degree: " + maxDegree);
			
			Stack<Integer> stack;
			int no = 0, index1, index2;
			PBDVertex v1;
			System.out.println("I am deleting files....");
			File fin = new File(filepath);
			
			for (File file : Objects.requireNonNull(fin.listFiles())) {
				if (file.isDirectory()) {
					for (File file2 : Objects.requireNonNull(file.listFiles()))
						file2.delete();
				}
			}
			System.out.println("Generating PBDLine Segment paths: ");
			for (int k = 0; k < vList.size(); k++) {
				
				v1 = graph.get(vList.get(k));
				index1 = k;
				for (int j = 0; j < v1.degree; j++) {
					stack = new Stack<>();
					index2 = v1.adjacencyList[j];
					if (index2 != -1) {
						stack.push(index2);
						if (stack.contains(index1)) continue;
						stack.push(index1);
						printPath(graph, stack, vList, curves, filepath, no);
						no++;
					}
					
				}
				
			}
			System.out.println("Total number of line segment paths: " + no);
			
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		
	}
	
	private Object[] generatePaths(ArrayList<PBDVertex> graph) {
		Object[] obj = new Object[3];
		try {
			ArrayList<Integer> vList = new ArrayList<>();
			
			int maxDegree = -1, e = 0;
			for (int i = 0; i < graph.size(); i++) {
				graph.get(i).removeDuplicates();
				e = e + graph.get(i).degree;
				if (graph.get(i).degree != 2) {
					vList.add(i);
					if (maxDegree < graph.get(i).degree)
						maxDegree = graph.get(i).degree;
					
				}
			}
			
			@SuppressWarnings("unchecked")
			ArrayList<PBDVertex>[][] curves = new ArrayList[vList.size()][maxDegree];
			double[][] pathLength = new double[vList.size()][maxDegree];
			
			PBDVertex v1, prev, cur;
			int curVertexIndex, startVertexIndex;
			int i = 0;
			
			while (i < vList.size()) {
				startVertexIndex = vList.get(i);// index of prev
				
				v1 = graph.get(startVertexIndex);
				
				for (int j = 0; j < v1.degree; j++) {
					
					HashMap<Integer, Integer> map = new HashMap<>();
					curves[i][j] = new ArrayList<>();
					map.put(startVertexIndex, startVertexIndex);
					curves[i][j].add(v1);
					prev = v1;
					
					curVertexIndex = v1.adjacencyList[j];// index of cur
					cur = graph.get(curVertexIndex);
					
					while (cur.degree == 2) {
						
						pathLength[i][j] += Math.sqrt((Math.pow(cur.x - prev.x,
								2) + Math.pow(cur.y - prev.y, 2)));
						
						curves[i][j].add(cur);
						map.put(curVertexIndex, curVertexIndex);
						
						if (map.get(cur.adjacencyList[0]) != null
								&& map.get(cur.adjacencyList[1]) != null) {
							if (cur.adjacencyList[0] == startVertexIndex)
								curVertexIndex = cur.adjacencyList[0];
							else if (cur.adjacencyList[1] == startVertexIndex)
								curVertexIndex = cur.adjacencyList[1];
							else {
								System.out.println("Error");
								break;
							}
						} else if (map.get(cur.adjacencyList[0]) != null) {
							curVertexIndex = cur.adjacencyList[1];
						} else {
							curVertexIndex = cur.adjacencyList[0];
						}
						cur = graph.get(curVertexIndex);
						v1.adjacencyList[j] = curVertexIndex;
						
					}
					
					pathLength[i][j] += Math
							.sqrt((Math.pow(cur.x - prev.x, 2) + Math.pow(cur.y
									- prev.y, 2)));
					curves[i][j].add(cur);
					v1.adjacencyList[j] = curVertexIndex;
				}
				
				i++;
				
			}
			/*System.out.println("PBDVertex set size: " + vList.size()
					+ " max degree: " + maxDegree);
*/
			obj[0] = vList;
			obj[1] = curves;
			obj[2] = pathLength;
			return obj;
			
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return obj;
	}
	
	public Object[] generateLengthOnePaths(ArrayList<PBDVertex> graph, String filepath, Object[] obj) {
		//Object[] obj = this.generatePaths(graph, filepath);
		
		@SuppressWarnings("unchecked")
		ArrayList<Integer> vList = (ArrayList<Integer>) obj[0];
		
		@SuppressWarnings("unchecked")
		ArrayList<PBDVertex>[][] curves = (ArrayList<PBDVertex>[][]) obj[1];
		
		Stack<Integer> stack;
		PBDVertex v1;
		int no = 0, index1, index2;
		System.out.println("I am deleting files....");
		File fin = new File(filepath);
		
		for (File file : Objects.requireNonNull(fin.listFiles())) {
			if (file.isDirectory()) {
				for (File file2 : Objects.requireNonNull(file.listFiles()))
					file2.delete();
			}
		}
		System.out.println("Generating length one paths: ");
		for (int i = 0; i < vList.size(); i++) {
			
			v1 = graph.get(vList.get(i));
			index1 = i;
			for (int j = 0; j < v1.degree; j++) {
				stack = new Stack<>();
				index2 = vList.indexOf(v1.adjacencyList[j]);
				if (index2 != -1)// i!=k &&
				{
					stack.push(index2);
					stack.push(index1);
					if (index1 == index2 && curves[i][j].size() == 2) continue;
					printPath(graph, stack, vList, curves, filepath, no);
					no++;
				}
				
			}
			
		}
		System.out.println("Total number length one of paths: " + no);
		return obj;
	}
	
	public void generateLengthTwoPaths(ArrayList<PBDVertex> graph, String filepath, Object[] obj) {
		// Object[] obj = this.generatePaths(graph, filepath);
		
		@SuppressWarnings("unchecked")
		ArrayList<Integer> vList = (ArrayList<Integer>) obj[0];
		
		@SuppressWarnings("unchecked")
		ArrayList<PBDVertex>[][] curves = (ArrayList<PBDVertex>[][]) obj[1];
		
		Stack<Integer> stack;
		PBDVertex v1, v2;
		int index1, index2, index3;
		int no = 0;
		System.out.println("I am deleting files....");
		File fin = new File(filepath);
		
		for (File file : Objects.requireNonNull(fin.listFiles())) {
			if (file.isDirectory()) {
				for (File file2 : Objects.requireNonNull(file.listFiles()))
					file2.delete();
			}
		}
		System.out.println("Generating length Two paths: ");
		for (int i = 0; i < vList.size(); i++) {
			index1 = i;
			v1 = graph.get(vList.get(i));
			for (int j = 0; j < v1.degree; j++) {
				
				index2 = vList.indexOf(v1.adjacencyList[j]);
				v2 = graph.get(v1.adjacencyList[j]);
				for (int k = 0; k < v2.degree; k++) {
					stack = new Stack<>();
					index3 = vList.indexOf(v2.adjacencyList[k]);
					
					if (index2 != -1 && index3 != -1) {
						stack.push(index3);
						if (stack.contains(index2))
							continue;
						stack.push(index2);
						if (stack.contains(index1))
							continue;
						stack.push(index1);
						printPath(graph, stack, vList, curves, filepath, no);
						no++;
					}
				}
			}
			
		}
		
		System.out.println("Total number length two of paths: " + no);
	}
	
	public void generateLengthThreePaths(ArrayList<PBDVertex> graph, String filepath, Object[] obj) {
		// Object[] obj = this.generatePaths(graph, filepath);
		
		@SuppressWarnings("unchecked")
		ArrayList<Integer> vList = (ArrayList<Integer>) obj[0];
		
		@SuppressWarnings("unchecked")
		ArrayList<PBDVertex>[][] curves = (ArrayList<PBDVertex>[][]) obj[1];
		
		Stack<Integer> stack;
		PBDVertex v1, v2, v3;
		int index1, index2, index3, index4;
		int no = 0;
		
		System.out.println("I am deleting files....");
		File fin = new File(filepath);
		
		for (File file : Objects.requireNonNull(fin.listFiles())) {
			if (file.isDirectory()) {
				for (File file2 : Objects.requireNonNull(file.listFiles()))
					file2.delete();
			}
		}
		
		System.out.println("Generating length Three paths: ");
		for (int i = 0; i < vList.size(); i++) {
			index1 = i;
			v1 = graph.get(vList.get(i));
			
			for (int j = 0; j < v1.degree; j++) {
				
				index2 = vList.indexOf(v1.adjacencyList[j]);
				v2 = graph.get(v1.adjacencyList[j]);
				for (int k = 0; k < v2.degree; k++) {
					index3 = vList.indexOf(v2.adjacencyList[k]);
					v3 = graph.get(v2.adjacencyList[k]);
					for (int l = 0; l < v3.degree; l++) {
						stack = new Stack<>();
						index4 = vList.indexOf(v3.adjacencyList[l]);
						if (index1 != -1 && index2 != -1 && index3 != -1
								&& index4 != -1) {
							stack.push(index4);
							if (stack.contains(index3))
								continue;
							stack.push(index3);
							if (stack.contains(index2))
								continue;
							stack.push(index2);
							if (stack.contains(index1))
								continue;
							stack.push(index1);
							
							printPath(graph, stack, vList, curves, filepath, no);
							no++;
						}
					}
				}
			}
			
		}
		System.out.println("Total number length three of paths: " + no);
	}
	
	public void generateLengthFourPaths(ArrayList<PBDVertex> graph, String filepath, Object[] obj) {
		// Object[] obj = this.generatePaths(graph, filepath);
		
		@SuppressWarnings("unchecked")
		ArrayList<Integer> vList = (ArrayList<Integer>) obj[0];
		
		@SuppressWarnings("unchecked")
		ArrayList<PBDVertex>[][] curves = (ArrayList<PBDVertex>[][]) obj[1];
		
		Stack<Integer> stack;
		PBDVertex v1, v2, v3, v4;
		int index1, index2, index3, index4, index5;
		int no = 0;
		
		System.out.println("I am deleting files....");
		File fin = new File(filepath);
		
		for (File file : Objects.requireNonNull(fin.listFiles())) {
			if (file.isDirectory()) {
				for (File file2 : Objects.requireNonNull(file.listFiles()))
					file2.delete();
			}
		}
		
		System.out.println("Generating length four paths: ");
		for (int i = 0; i < vList.size(); i++) {
			index1 = i;
			v1 = graph.get(vList.get(i));
			
			for (int j = 0; j < v1.degree; j++) {
				
				index2 = vList.indexOf(v1.adjacencyList[j]);
				v2 = graph.get(v1.adjacencyList[j]);
				
				for (int k = 0; k < v2.degree; k++) {
					index3 = vList.indexOf(v2.adjacencyList[k]);
					v3 = graph.get(v2.adjacencyList[k]);
					
					for (int l = 0; l < v3.degree; l++) {
						index4 = vList.indexOf(v3.adjacencyList[l]);
						v4 = graph.get(v3.adjacencyList[l]);
						for (int m = 0; m < v4.degree; m++) {
							stack = new Stack<>();
							index5 = vList.indexOf(v4.adjacencyList[m]);
							if (index1 != -1 && index2 != -1 && index3 != -1
									&& index4 != -1 && index5 != -1) {
								stack.push(index5);
								if (stack.contains(index4))
									continue;
								stack.push(index4);
								if (stack.contains(index3))
									continue;
								stack.push(index3);
								if (stack.contains(index2))
									continue;
								stack.push(index2);
								if (stack.contains(index1))
									continue;
								stack.push(index1);
								
								printPath(graph, stack, vList, curves,
										filepath, no);
								no++;
							}
							
						}
					}
				}
			}
			
		}
		
		System.out.println("Total number length four of paths: " + no);
	}
	
	public void generateLengthFivePaths(ArrayList<PBDVertex> graph, String filepath, Object[] obj) {
		// Object[] obj = this.generatePaths(graph, filepath);
		
		@SuppressWarnings("unchecked")
		ArrayList<Integer> vList = (ArrayList<Integer>) obj[0];
		
		@SuppressWarnings("unchecked")
		ArrayList<PBDVertex>[][] curves = (ArrayList<PBDVertex>[][]) obj[1];
		
		Stack<Integer> stack;
		PBDVertex v1, v2, v3, v4, v5;
		int index1, index2, index3, index4, index5, index6;
		int no = 0;
		
		System.out.println("I am deleting files....");
		File fin = new File(filepath);
		
		for (File file : Objects.requireNonNull(fin.listFiles())) {
			if (file.isDirectory()) {
				for (File file2 : Objects.requireNonNull(file.listFiles()))
					file2.delete();
			}
		}
		
		System.out.println("Generating length five paths: ");
		for (int i = 0; i < vList.size(); i++) {
			index1 = i;
			v1 = graph.get(vList.get(i));
			
			for (int j = 0; j < v1.degree; j++) {
				
				index2 = vList.indexOf(v1.adjacencyList[j]);
				v2 = graph.get(v1.adjacencyList[j]);
				
				for (int k = 0; k < v2.degree; k++) {
					index3 = vList.indexOf(v2.adjacencyList[k]);
					v3 = graph.get(v2.adjacencyList[k]);
					
					for (int l = 0; l < v3.degree; l++) {
						index4 = vList.indexOf(v3.adjacencyList[l]);
						v4 = graph.get(v3.adjacencyList[l]);
						for (int m = 0; m < v4.degree; m++) {
							index5 = vList.indexOf(v4.adjacencyList[m]);
							v5 = graph.get(v4.adjacencyList[m]);
							for (int n = 0; n < v5.degree; n++) {
								stack = new Stack<>();
								index6 = vList.indexOf(v5.adjacencyList[n]);
								
								if (index1 != -1 && index2 != -1
										&& index3 != -1 && index4 != -1
										&& index5 != -1 && index6 != -1) {
									
									stack.push(index6);
									if (stack.contains(index5))
										continue;
									stack.push(index5);
									if (stack.contains(index4))
										continue;
									stack.push(index4);
									if (stack.contains(index3))
										continue;
									stack.push(index3);
									if (stack.contains(index2))
										continue;
									stack.push(index2);
									if (stack.contains(index1))
										continue;
									stack.push(index1);
									
									printPath(graph, stack, vList, curves,
											filepath, no);
									no++;
								}
							}
						}
					}
				}
			}
			
		}
		
		System.out.println("Total number length five of paths: " + no);
	}
	
	public void generateLengthSixPaths(ArrayList<PBDVertex> graph, String filepath, Object[] obj) {
		// Object[] obj = this.generatePaths(graph, filepath);
		
		@SuppressWarnings("unchecked")
		ArrayList<Integer> vList = (ArrayList<Integer>) obj[0];
		
		@SuppressWarnings("unchecked")
		ArrayList<PBDVertex>[][] curves = (ArrayList<PBDVertex>[][]) obj[1];
		
		Stack<Integer> stack;
		PBDVertex v1, v2, v3, v4, v5, v6;
		int index1, index2, index3, index4, index5, index6, index7;
		int no = 0;
		
		System.out.println("I am deleting files....");
		File fin = new File(filepath);
		
		for (File file : Objects.requireNonNull(fin.listFiles())) {
			if (file.isDirectory()) {
				for (File file2 : Objects.requireNonNull(file.listFiles()))
					file2.delete();
			}
		}
		
		System.out.println("Generating length six paths: ");
		for (int i = 0; i < vList.size(); i++) {
			index1 = i;
			v1 = graph.get(vList.get(i));
			
			for (int j = 0; j < v1.degree; j++) {
				
				index2 = vList.indexOf(v1.adjacencyList[j]);
				v2 = graph.get(v1.adjacencyList[j]);
				
				for (int k = 0; k < v2.degree; k++) {
					index3 = vList.indexOf(v2.adjacencyList[k]);
					v3 = graph.get(v2.adjacencyList[k]);
					
					for (int l = 0; l < v3.degree; l++) {
						index4 = vList.indexOf(v3.adjacencyList[l]);
						v4 = graph.get(v3.adjacencyList[l]);
						for (int m = 0; m < v4.degree; m++) {
							index5 = vList.indexOf(v4.adjacencyList[m]);
							v5 = graph.get(v4.adjacencyList[m]);
							for (int n = 0; n < v5.degree; n++) {
								
								index6 = vList.indexOf(v5.adjacencyList[n]);
								v6 = graph.get(v5.adjacencyList[n]);
								
								for (int o = 0; o < v6.degree; o++) {
									stack = new Stack<>();
									index7 = vList.indexOf(v6.adjacencyList[o]);
									if (index1 != -1 && index2 != -1
											&& index3 != -1 && index4 != -1
											&& index5 != -1 && index6 != -1
											&& index7 != -1) {
										
										stack.push(index7);
										if (stack.contains(index6))
											continue;
										stack.push(index6);
										if (stack.contains(index5))
											continue;
										stack.push(index5);
										if (stack.contains(index4))
											continue;
										stack.push(index4);
										if (stack.contains(index3))
											continue;
										stack.push(index3);
										if (stack.contains(index2))
											continue;
										stack.push(index2);
										if (stack.contains(index1))
											continue;
										stack.push(index1);
										
										printPath(graph, stack, vList, curves,
												filepath, no);
										no++;
									}
								}
							}
						}
					}
				}
			}
			
		}
		
		System.out.println("Total number length six of paths: " + no);
	}
	
	public void generateLengthSevenPaths(ArrayList<PBDVertex> graph, String filepath, Object[] obj) {
		// Object[] obj = this.generatePaths(graph, filepath);
		
		@SuppressWarnings("unchecked")
		ArrayList<Integer> vList = (ArrayList<Integer>) obj[0];
		
		@SuppressWarnings("unchecked")
		ArrayList<PBDVertex>[][] curves = (ArrayList<PBDVertex>[][]) obj[1];
		
		Stack<Integer> stack;
		PBDVertex v1, v2, v3, v4, v5, v6, v7;
		int index1, index2, index3, index4, index5, index6, index7, index8;
		int no = 0;
		
		System.out.println("I am deleting files....");
		File fin = new File(filepath);
		
		for (File file : Objects.requireNonNull(fin.listFiles())) {
			if (file.isDirectory()) {
				for (File file2 : Objects.requireNonNull(file.listFiles()))
					file2.delete();
			}
		}
		
		System.out.println("Generating length seven paths: ");
		for (int i = 0; i < vList.size(); i++) {
			index1 = i;
			v1 = graph.get(vList.get(i));
			
			for (int j = 0; j < v1.degree; j++) {
				
				index2 = vList.indexOf(v1.adjacencyList[j]);
				v2 = graph.get(v1.adjacencyList[j]);
				
				for (int k = 0; k < v2.degree; k++) {
					index3 = vList.indexOf(v2.adjacencyList[k]);
					v3 = graph.get(v2.adjacencyList[k]);
					
					for (int l = 0; l < v3.degree; l++) {
						index4 = vList.indexOf(v3.adjacencyList[l]);
						v4 = graph.get(v3.adjacencyList[l]);
						for (int m = 0; m < v4.degree; m++) {
							index5 = vList.indexOf(v4.adjacencyList[m]);
							v5 = graph.get(v4.adjacencyList[m]);
							for (int n = 0; n < v5.degree; n++) {
								
								index6 = vList.indexOf(v5.adjacencyList[n]);
								v6 = graph.get(v5.adjacencyList[n]);
								
								for (int o = 0; o < v6.degree; o++) {
									
									index7 = vList.indexOf(v6.adjacencyList[o]);
									v7 = graph.get(v6.adjacencyList[o]);
									for (int p = 0; p < v7.degree; p++) {
										index8 = vList
												.indexOf(v7.adjacencyList[p]);
										stack = new Stack<>();
										if (index1 != -1 && index2 != -1
												&& index3 != -1 && index4 != -1
												&& index5 != -1 && index6 != -1
												&& index7 != -1 && index8 != -1) {
											stack.push(index8);
											if (stack.contains(index7))
												continue;
											stack.push(index7);
											if (stack.contains(index6))
												continue;
											stack.push(index6);
											if (stack.contains(index5))
												continue;
											stack.push(index5);
											if (stack.contains(index4))
												continue;
											stack.push(index4);
											if (stack.contains(index3))
												continue;
											stack.push(index3);
											if (stack.contains(index2))
												continue;
											stack.push(index2);
											if (stack.contains(index1))
												continue;
											stack.push(index1);
											
											printPath(graph, stack, vList,
													curves, filepath, no);
											no++;
										}
									}
								}
							}
						}
					}
				}
			}
			
		}
		
		System.out.println("Total number length seven of paths: " + no);
	}
	
	public void generateLengthEightPaths(ArrayList<PBDVertex> graph, String filepath, Object[] obj) {
		// Object[] obj = this.generatePaths(graph, filepath);
		
		@SuppressWarnings("unchecked")
		ArrayList<Integer> vList = (ArrayList<Integer>) obj[0];
		
		@SuppressWarnings("unchecked")
		ArrayList<PBDVertex>[][] curves = (ArrayList<PBDVertex>[][]) obj[1];
		
		Stack<Integer> stack;
		PBDVertex v1, v2, v3, v4, v5, v6, v7, v8;
		int index1, index2, index3, index4, index5, index6, index7, index8, index9;
		int no = 0;
		
		System.out.println("I am deleting files....");
		File fin = new File(filepath);
		
		for (File file : Objects.requireNonNull(fin.listFiles())) {
			if (file.isDirectory()) {
				for (File file2 : Objects.requireNonNull(file.listFiles()))
					file2.delete();
			}
		}
		
		System.out.println("Generating length eight paths: ");
		for (int i = 0; i < vList.size(); i++) {
			index1 = i;
			v1 = graph.get(vList.get(i));
			
			for (int j = 0; j < v1.degree; j++) {
				
				index2 = vList.indexOf(v1.adjacencyList[j]);
				v2 = graph.get(v1.adjacencyList[j]);
				
				for (int k = 0; k < v2.degree; k++) {
					index3 = vList.indexOf(v2.adjacencyList[k]);
					v3 = graph.get(v2.adjacencyList[k]);
					
					for (int l = 0; l < v3.degree; l++) {
						index4 = vList.indexOf(v3.adjacencyList[l]);
						v4 = graph.get(v3.adjacencyList[l]);
						for (int m = 0; m < v4.degree; m++) {
							index5 = vList.indexOf(v4.adjacencyList[m]);
							v5 = graph.get(v4.adjacencyList[m]);
							for (int n = 0; n < v5.degree; n++) {
								
								index6 = vList.indexOf(v5.adjacencyList[n]);
								v6 = graph.get(v5.adjacencyList[n]);
								
								for (int o = 0; o < v6.degree; o++) {
									
									index7 = vList.indexOf(v6.adjacencyList[o]);
									v7 = graph.get(v6.adjacencyList[o]);
									for (int p = 0; p < v7.degree; p++) {
										index8 = vList
												.indexOf(v7.adjacencyList[p]);
										v8 = graph.get(v7.adjacencyList[p]);
										for (int q = 0; q < v8.degree; q++) {
											index9 = vList
													.indexOf(v8.adjacencyList[q]);
											stack = new Stack<>();
											if (index1 != -1 && index2 != -1
													&& index3 != -1
													&& index4 != -1
													&& index5 != -1
													&& index6 != -1
													&& index7 != -1
													&& index8 != -1
													&& index9 != -1) {
												stack.push(index9);
												if (stack.contains(index8))
													continue;
												stack.push(index8);
												if (stack.contains(index7))
													continue;
												stack.push(index7);
												if (stack.contains(index6))
													continue;
												stack.push(index6);
												if (stack.contains(index5))
													continue;
												stack.push(index5);
												if (stack.contains(index4))
													continue;
												stack.push(index4);
												if (stack.contains(index3))
													continue;
												stack.push(index3);
												if (stack.contains(index2))
													continue;
												stack.push(index2);
												if (stack.contains(index1))
													continue;
												stack.push(index1);
												
												printPath(graph, stack, vList,
														curves, filepath, no);
												no++;
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
		}
		
		System.out.println("Total number length eight of paths: " + no);
	}
	
	public void generateLengthNinePaths(ArrayList<PBDVertex> graph, String filepath, Object... obj) {
		// Object[] obj = this.generatePaths(graph, filepath);
		
		@SuppressWarnings("unchecked")
		ArrayList<Integer> vList = (ArrayList<Integer>) obj[0];
		
		@SuppressWarnings("unchecked")
		ArrayList<PBDVertex>[][] curves = (ArrayList<PBDVertex>[][]) obj[1];
		
		Stack<Integer> stack;
		PBDVertex v1, v2, v3, v4, v5, v6, v7, v8, v9;
		int index1, index2, index3, index4, index5, index6, index7, index8, index9, index10;
		int no = 0;
		
		System.out.println("I am deleting files....");
		File fin = new File(filepath);
		
		for (File file : Objects.requireNonNull(fin.listFiles())) {
			if (file.isDirectory()) {
				for (File file2 : Objects.requireNonNull(file.listFiles()))
					file2.delete();
			}
		}
		
		System.out.println("Generating length nine paths: ");
		for (int i = 0; i < vList.size(); i++) {
			index1 = i;
			v1 = graph.get(vList.get(i));
			
			for (int j = 0; j < v1.degree; j++) {
				
				index2 = vList.indexOf(v1.adjacencyList[j]);
				v2 = graph.get(v1.adjacencyList[j]);
				
				for (int k = 0; k < v2.degree; k++) {
					index3 = vList.indexOf(v2.adjacencyList[k]);
					v3 = graph.get(v2.adjacencyList[k]);
					
					for (int l = 0; l < v3.degree; l++) {
						index4 = vList.indexOf(v3.adjacencyList[l]);
						v4 = graph.get(v3.adjacencyList[l]);
						for (int m = 0; m < v4.degree; m++) {
							index5 = vList.indexOf(v4.adjacencyList[m]);
							v5 = graph.get(v4.adjacencyList[m]);
							for (int n = 0; n < v5.degree; n++) {
								
								index6 = vList.indexOf(v5.adjacencyList[n]);
								v6 = graph.get(v5.adjacencyList[n]);
								
								for (int o = 0; o < v6.degree; o++) {
									
									index7 = vList.indexOf(v6.adjacencyList[o]);
									v7 = graph.get(v6.adjacencyList[o]);
									for (int p = 0; p < v7.degree; p++) {
										index8 = vList
												.indexOf(v7.adjacencyList[p]);
										v8 = graph.get(v7.adjacencyList[p]);
										for (int q = 0; q < v8.degree; q++) {
											index9 = vList
													.indexOf(v8.adjacencyList[q]);
											v9 = graph.get(v8.adjacencyList[q]);
											for (int r = 0; r < v9.degree; r++) {
												index10 = vList
														.indexOf(v9.adjacencyList[r]);
												stack = new Stack<
														Integer>();
												if (index1 != -1
														&& index2 != -1
														&& index3 != -1
														&& index4 != -1
														&& index5 != -1
														&& index6 != -1
														&& index7 != -1
														&& index8 != -1
														&& index9 != -1
														&& index10 != -1) {
													stack.push(index10);
													if (stack.contains(index9))
														continue;
													stack.push(index9);
													if (stack.contains(index8))
														continue;
													stack.push(index8);
													if (stack.contains(index7))
														continue;
													stack.push(index7);
													if (stack.contains(index6))
														continue;
													stack.push(index6);
													if (stack.contains(index5))
														continue;
													stack.push(index5);
													if (stack.contains(index4))
														continue;
													stack.push(index4);
													if (stack.contains(index3))
														continue;
													stack.push(index3);
													if (stack.contains(index2))
														continue;
													stack.push(index2);
													if (stack.contains(index1))
														continue;
													stack.push(index1);
													
													printPath(graph, stack,
															vList, curves,
															filepath, no);
													no++;
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
		}
		
		System.out.println("Total number length nine of paths: " + no);
	}
	
	public void generateLengthTenPaths(ArrayList<PBDVertex> graph, String filepath, Object[] obj) {
		// Object[] obj = this.generatePaths(graph, filepath);
		
		@SuppressWarnings("unchecked")
		ArrayList<Integer> vList = (ArrayList<Integer>) obj[0];
		
		@SuppressWarnings("unchecked")
		ArrayList<PBDVertex>[][] curves = (ArrayList<PBDVertex>[][]) obj[1];
		
		Stack<Integer> stack;
		PBDVertex v1, v2, v3, v4, v5, v6, v7, v8, v9, v10;
		int index1, index2, index3, index4, index5, index6, index7, index8, index9, index10, index11;
		int no = 0;
		
		System.out.println("I am deleting files....");
		File fin = new File(filepath);
		
		for (File file : Objects.requireNonNull(fin.listFiles())) {
			if (file.isDirectory()) {
				for (File file2 : Objects.requireNonNull(file.listFiles()))
					file2.delete();
			}
		}
		
		System.out.println("Generating length ten paths: ");
		for (int i = 0; i < vList.size(); i++) {
			index1 = i;
			v1 = graph.get(vList.get(i));
			
			for (int j = 0; j < v1.degree; j++) {
				
				index2 = vList.indexOf(v1.adjacencyList[j]);
				v2 = graph.get(v1.adjacencyList[j]);
				
				for (int k = 0; k < v2.degree; k++) {
					index3 = vList.indexOf(v2.adjacencyList[k]);
					v3 = graph.get(v2.adjacencyList[k]);
					
					for (int l = 0; l < v3.degree; l++) {
						index4 = vList.indexOf(v3.adjacencyList[l]);
						v4 = graph.get(v3.adjacencyList[l]);
						for (int m = 0; m < v4.degree; m++) {
							index5 = vList.indexOf(v4.adjacencyList[m]);
							v5 = graph.get(v4.adjacencyList[m]);
							for (int n = 0; n < v5.degree; n++) {
								
								index6 = vList.indexOf(v5.adjacencyList[n]);
								v6 = graph.get(v5.adjacencyList[n]);
								
								for (int o = 0; o < v6.degree; o++) {
									
									index7 = vList.indexOf(v6.adjacencyList[o]);
									v7 = graph.get(v6.adjacencyList[o]);
									for (int p = 0; p < v7.degree; p++) {
										index8 = vList
												.indexOf(v7.adjacencyList[p]);
										v8 = graph.get(v7.adjacencyList[p]);
										for (int q = 0; q < v8.degree; q++) {
											index9 = vList
													.indexOf(v8.adjacencyList[q]);
											v9 = graph.get(v8.adjacencyList[q]);
											for (int r = 0; r < v9.degree; r++) {
												index10 = vList
														.indexOf(v9.adjacencyList[r]);
												v10 = graph
														.get(v9.adjacencyList[r]);
												for (int s = 0; s < v10.degree; s++) {
													index11 = vList
															.indexOf(v10.adjacencyList[s]);
													stack = new Stack<>();
													if (index1 != -1
															&& index2 != -1
															&& index3 != -1
															&& index4 != -1
															&& index5 != -1
															&& index6 != -1
															&& index7 != -1
															&& index8 != -1
															&& index9 != -1
															&& index10 != -1
															&& index11 != -1) {
														stack.push(index11);
														if (stack
																.contains(index10))
															continue;
														stack.push(index10);
														if (stack
																.contains(index9))
															continue;
														stack.push(index9);
														if (stack
																.contains(index8))
															continue;
														stack.push(index8);
														if (stack
																.contains(index7))
															continue;
														stack.push(index7);
														if (stack
																.contains(index6))
															continue;
														stack.push(index6);
														if (stack
																.contains(index5))
															continue;
														stack.push(index5);
														if (stack
																.contains(index4))
															continue;
														stack.push(index4);
														if (stack
																.contains(index3))
															continue;
														stack.push(index3);
														if (stack
																.contains(index2))
															continue;
														stack.push(index2);
														if (stack
																.contains(index1))
															continue;
														stack.push(index1);
														
														printPath(graph, stack,
																vList, curves,
																filepath, no);
														no++;
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
		}
		
		System.out.println("Total number length ten of paths: " + no);
	}
	
	public void checkDegree(ArrayList<Integer> vList, ArrayList<PBDVertex> graph) {
		int i = 0;
		int j = 0;
		System.out.println("I am inside check degree.");
		while (i < vList.size()) {
			PBDVertex v = graph.get(vList.get(i));
			
			if (v.degree == 2)
				System.out.println("I am a problem");
			
			if (vList.indexOf(vList.get(i)) == -1)
				System.out.println("I am a problem " + i + " " + v.toString());
			j = 0;
			while (j < v.degree) {
				
				PBDVertex v1 = graph.get(v.adjacencyList[j]);
				if (v1.degree == 2)
					System.out.println("I am a problem " + j + " "
							+ v.toString());
				if (vList.indexOf(v.adjacencyList[j]) == -1)
					System.out.println("I am a problem " + j + " "
							+ v.toString());
				
				/*
				 * if(vList.get(i).intValue() == 1734) {
				 * System.out.println("I am a problem "
				 * +j+" "+v.adjacencyList[j]+" "+v1.toString()); }
				 */
				
				j++;
			}
			i++;
		}
	}
	
	public void getShortestPaths(ArrayList<PBDVertex> graph, String filepath, Object[] obj) {
		
		// Object[] obj = this.generatePaths(graph, filepath);
		
		@SuppressWarnings("unchecked")
		ArrayList<Integer> vList = (ArrayList<Integer>) obj[0];
		
		@SuppressWarnings("unchecked")
		ArrayList<PBDVertex>[][] curves = (ArrayList<PBDVertex>[][]) obj[1];
		
		double[][] pathLength = (double[][]) obj[2];
		
		System.out.println("I am deleting files....");
		File fin = new File(filepath);
		
		for (File file : Objects.requireNonNull(fin.listFiles())) {
			if (file.isDirectory()) {
				for (File file2 : Objects.requireNonNull(file.listFiles()))
					file2.delete();
			}
		}
		int no = 0;
		
		for (int start = 0; start < vList.size(); start++) {
			int[] parent = new int[vList.size()];
			Comparator<Integer> comparator = new PathLabelComparator(graph);
			PriorityQueue<Integer> pq = new PriorityQueue<>(209884,
					comparator);
			
			int i;
			int maxDegree = 0;
			/*
			 * while(i < vList.size()) {
			 *
			 * graph.get(vList.get(i).intValue()).left = Double.MAX_VALUE;
			 * if(graph.get(vList.get(maxDegree)).degree <
			 * graph.get(vList.get(i)).degree) maxDegree = i;
			 *
			 * pq.add(vList.get(i));
			 *
			 *
			 * i++; }
			 */
			
			// checkDegree(vList,graph);
			pq.remove(vList.get(maxDegree));
			graph.get(vList.get(maxDegree)).left = 0;
			pq.add(vList.get(maxDegree));
			// pq has graph indices
			PBDVertex v, v1;
			int index1, index2;
			int j;
			// int count = 0;
			// start = maxDegree;
			
			// System.out.println("I am computing shortest Paths...."+graph.get(vList.get(maxDegree)).degree);
			
			boolean[] reachable = new boolean[vList.size()];
			
			while (!pq.isEmpty()) {
				// count++;
				i = pq.poll();
				if (i == -1) {
					continue;
				}
				index1 = vList.indexOf(i);
				v = graph.get(i);// i contains index of graph and index1
				// contains index of vList
				// System.out.println(i+" "+index1+" "+
				// v.left+" "+graph.get(i).toString());
				
				// if(v.left == Double.MAX_VALUE)v.left = 0;
				v.done = true;
				if (v.left == Double.MAX_VALUE) {
					reachable[index1] = false;
					continue;
				} else {
					reachable[index1] = true;
				}
				
				j = 0;
				
				while (j < v.degree) {
					
					v1 = graph.get(v.adjacencyList[j]);
					
					index2 = vList.indexOf(v.adjacencyList[j]);
					
					if (index2 == -1) {
						j++;
						continue;
					}
					
					if (!v1.done) {
						// index2 = vList.indexOf(new
						// Integer(v.adjacencyList[j]));
						
						if (v1.left > v.left + pathLength[index1][j]) {
							// System.out.println(v.left +
							// pathLength[index1][j]);
							
							pq.remove(vList.get(index2));
							
							v1.left = v.left + pathLength[index1][j];
							
							pq.add(vList.get(index2));
							// if(index2==29498){System.out.println("I was inserted here."
							// +i);}
							parent[index2] = i;
							// if(index1==17)System.out.println(i+" "+index1+"  "+index2+" "+parent[index2]);
						}
					} else {
						// if(parent[index2] ==
						// 0)System.out.println("done "+v1.left+" "+v1.toString());
					}
					j++;
				}
				
			}
			
			// printVList(vList,graph,parent);
			
			// extract paths
			
			i = 0;
			int cur;
			int length;
			Stack<Integer> stack;
			ArrayList<Integer> generatePath = new ArrayList<>();
			
			while (i < vList.size()) {
				if (reachable[i])
					generatePath.add(i);
				i++;
			}
			
			// System.out.println("I am generating Paths....");
			// System.out.println("I am generating shortest Paths...."+maxDegree);
			while (generatePath.size() != 0) {
				stack = new Stack<>();
				cur = generatePath.remove(0);
				// System.out.println(no +"size of generatePath: " +
				// generatePath.size());
				length = 0;
				
				while (cur != start && cur != -1) {
					// if()
					// {
					// System.out.println("size of generatePath: " +
					// generatePath.size());
					// }
					// System.out.println("size of generatePath: " +
					// generatePath.size());
					generatePath.remove(Integer.valueOf(cur));
					if (stack.contains(cur))
						break;
					stack.push(cur);
					// System.out.println(cur);
					cur = vList.indexOf(parent[cur]);
					// if(cur==-1)System.out.println(stack.peek().intValue()+" "+parent[stack.peek().intValue()]+"  "+graph.get(parent[stack.peek().intValue()])+" "+graph.get(vList.get(stack.peek().intValue())));
					length++;
				}
				
				if (!stack.isEmpty())// && length > 1
				{
					if (!stack.contains(cur)) {
						stack.push(cur);
						length++;
					}
					// System.out.println("Write to file: "+ no +"  "+ length);
					if (length > 1)
						printPath(graph, stack, vList, curves, filepath, no);
					
					no++;
				}
				
			}
		}
		System.out.println("Total number shortest of paths: " + no);
	}
	
	public void printPath(ArrayList<PBDVertex> graph, Stack<Integer> stack,
						  ArrayList<Integer> vList, ArrayList<PBDVertex>[][] curves,
						  String filepath, int no) {
		
		try {
			
			int numf = 5;// number of folders to store files
			
			int index1 = stack.pop();// stack has indices of vList
			if (index1 == -1)
				return;
			
			File file = new File(filepath + no % numf + "/");
			if (!file.exists()) {
				if (file.mkdir()) {
					System.out.println("Directory is created!");
				} else {
					System.out.println("Failed to create directory!");
				}
			}
			
			String fileName = filepath
					+ no % numf + "/" + no + ".dat";
			
			BufferedWriter bwWays = new BufferedWriter(new FileWriter(fileName));
			
			int p = vList.get(index1);
			int q;
			
			bwWays.write(graph.get(p).getKeyString() + "\n");
			
			while (!stack.isEmpty()) {
				
				int index3 = stack.pop();
				// System.out.println(index3);
				q = graph.indexOf(graph.get(vList.get(index3)));
				
				int index2 = graph.get(p).getIndexAdjacent(q);
				
				// System.out.println("q = "+q+ " index1="+index1+
				// " index3="+index3);
				
				if (q != -1) {
					int h = 1;
					while (h < curves[index1][index2].size() - 1) {
						bwWays.write(curves[index1][index2].get(h).getKeyString() + "\n");
						h++;
					}
				} else {
					
					System.out.println("I am break.");
					break;
					
				}
				p = q;
				bwWays.write(graph.get(p).getKeyString() + "\n");
				
				index1 = index3;
			}
			//if(found1&&found2)System.out.println(fileName);
			bwWays.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void printVList(ArrayList<Integer> vList, ArrayList<PBDVertex> graph, int[] parent) {
		
		int i = 17;
		int count = 0;
		while (i < 18)// <vList.size())
		{
			// if(parent[i]<1)
			// {
			System.out.println(i + " " + parent[i] + "  "
					+ graph.get(vList.get(i)));
			count++;
			// }
			i = i + 1;
		}
		System.out.println("count = " + count);
	}
	
	public void computeClosestDistance(Object[] obj, ArrayList<PBDVertex> graph, String filePath) {
		@SuppressWarnings("unchecked")
		ArrayList<Integer> vList = (ArrayList<Integer>) obj[0];
		double[] minDist = new double[vList.size()];
		try {
			BufferedWriter bwWays = new BufferedWriter(new FileWriter(filePath));
			for (int i = 0; i < vList.size(); i++) {
				minDist[i] = Double.MAX_VALUE;
				PBDVertex v1 = graph.get(vList.get(i));
				for (int j = 0; j < v1.degree; j++) {
					PBDVertex v2 = graph.get(v1.adjacencyList[j]);
					double dist = v1.dist(v2);
					if (dist != 0 && minDist[i] > dist)
						minDist[i] = dist;
				}
				if (minDist[i] < Double.MAX_VALUE)
					bwWays.write(v1.x + " " + v1.y + " " + v1.degree + " "
							+ minDist[i] + "\n");
			}
			
			bwWays.close();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		
	}
	
	public void angle(Object[] obj, ArrayList<PBDVertex> graph, String filePath) {
		
		@SuppressWarnings("unchecked")
		ArrayList<Integer> vList = (ArrayList<Integer>) obj[0];
		@SuppressWarnings("unchecked")
		ArrayList<PBDVertex>[][] curves = (ArrayList<PBDVertex>[][]) obj[1];
		
		double[] minAngle = new double[vList.size()];
		try {
			BufferedWriter bwWays = new BufferedWriter(new FileWriter(filePath));
			for (int i = 0; i < vList.size(); i++) {
				minAngle[i] = Double.MAX_VALUE;
				PBDVertex v1 = graph.get(vList.get(i));
				for (int j = 0; j < v1.degree; j++) {
					PBDVertex v2 = curves[i][j].get(1);// graph.get(v1.adjacencyList[j]);
					
					for (int k = j + 1; k < v1.degree; k++) {
						
						PBDVertex v3 = curves[i][k].get(1);// graph.get(v1.adjacencyList[k]);
						if (v1.equals(v2) || v1.equals(v3) || v2.equals(v3))
							continue;
						double dx1 = v2.x - v1.x;
						double dx2 = v3.x - v1.x;
						double dy1 = v2.y - v1.y;
						double dy2 = v3.y - v1.y;
						double angle = Math.acos((dx1 * dx2 + dy1 * dy2)
								/ (Math.sqrt(Math.pow(dx1, 2)
								+ Math.pow(dy1, 2)) * Math.sqrt(Math
								.pow(dx2, 2) + Math.pow(dy2, 2))));
						if (minAngle[i] > angle)
							minAngle[i] = angle;
					}
					
				}
				
				if (minAngle[i] < Double.MAX_VALUE && minAngle[i] > 0)
					bwWays.write(v1.x + " " + v1.y + " " + v1.degree + " "
							+ minAngle[i] + "\n");
			}
			
			bwWays.close();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}
	
	public void generatePathsLinkLength(ArrayList<PBDVertex> graph, String pathPath, String linkLength) {
		
		File pathFile = new File(pathPath);
		
		if (!pathFile.exists())
			pathFile.mkdirs();
		
		if (linkLength.equals("LineSegment")) {
			pathFile = new File(pathPath + "LineSegment/");
			if (!pathFile.exists())
				pathFile.mkdirs();
			this.generateLineSegmentPaths(graph, pathPath + linkLength + "/");
			return;
		}
		
		/*pathFile = new File(pathPath + "LinkOne/");
		if (!pathFile.exists())
			pathFile.mkdirs();
		*/
		Object[] obj1 = this.generatePaths(graph);
		
		pathFile = new File(pathPath + linkLength + "/");
		if (!pathFile.exists())
			pathFile.mkdirs();
		switch (linkLength) {
			case "LinkOne":
				this.generateLengthOnePaths(graph, pathPath + linkLength + "/", obj1);
				break;
			case "LinkTwo":
				this.generateLengthTwoPaths(graph, pathPath + linkLength + "/",
						obj1);
				break;
			case "LinkThree":
				this.generateLengthThreePaths(graph, pathPath + linkLength + "/",
						obj1);
				break;
			case "LinkFour":
				this.generateLengthFourPaths(graph, pathPath + linkLength + "/",
						obj1);
				break;
			case "LinkFive":
				this.generateLengthFivePaths(graph, pathPath + linkLength + "/",
						obj1);
				break;
			case "LinkSix":
				this.generateLengthSixPaths(graph, pathPath + linkLength + "/",
						obj1);
				break;
			case "LinkSeven":
				this.generateLengthSevenPaths(graph, pathPath + linkLength + "/",
						obj1);
				break;
			case "LinkEight":
				this.generateLengthEightPaths(graph, pathPath + linkLength + "/",
						obj1);
				break;
			case "LinkNine":
				this.generateLengthNinePaths(graph, pathPath + linkLength + "/",
						obj1);
				break;
			case "LinkTen":
				this.generateLengthTenPaths(graph, pathPath + linkLength + "/",
						obj1);
				break;
			case "ShortestPaths":
				this.getShortestPaths(graph, pathPath + linkLength + "/", obj1);
				break;
		}
		
	}
}
