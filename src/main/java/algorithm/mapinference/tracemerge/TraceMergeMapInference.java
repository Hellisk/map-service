package algorithm.mapinference.tracemerge;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;

import java.util.*;

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
 *
 * @author Mahmuda Ahmed, Hellisk
 */
public class TraceMergeMapInference {
	
	private static final Logger LOG = Logger.getLogger(TraceMergeMapInference.class);
	private static int curveId; // counter for pose
	private static String curveName; // file name for the pose
	
	/**
	 * Computes interval on edge e for a line segment consists of
	 * (currentIndex-1)-th and currentIndex-th vertices of traj and return true
	 * if edge e has a part of white interval else false.
	 */
	private boolean isWhiteInterval(EdgeTM edge, List<VertexTM> traj, int currentIndex, double eps) {
		LineTM line = new LineTM(traj.get(currentIndex - 1), traj.get(currentIndex), edge.getVertex1().getDistanceFunction());
		return line.pIntersection(edge, eps);
	}
	
	/**
	 * Sets corresponding interval endpoints on EdgeTM.
	 */
	private void setEndPointsOnEdge(EdgeTM edge, int startIndex, int endIndex, double cStart, double vStart) {
		edge.setCurveStartIndex(startIndex);
		edge.setCurveStart(startIndex + cStart);
		edge.setEdgeStart(vStart);
		
		edge.setCurveEnd(endIndex - 1 + edge.getCurveEnd());
		edge.setCurveEndIndex(endIndex);
	}
	
	/**
	 * Scans for next white interval on an EdgeTM starting from index newStart of
	 * pose.
	 */
	private void computeNextInterval(EdgeTM edge, List<VertexTM> pose, int newStart, double eps) {
		
		// Compute next white interval on edge.
		boolean first = true;
		boolean debug = false;
		
		int startIndex = 0;
		double cStart = 0, vStart = 0;
		
		if (newStart >= pose.size()) {
			edge.setCurveEndIndex(pose.size());
			edge.setDone(true);
			return;
		}
		
		for (int i = newStart; i < pose.size(); i++) {
			boolean result = isWhiteInterval(edge, pose, i, eps);
			
			// first = true means we are still looking for our first interval
			// starting from newStart.
			// !result indicate LineTM(pose.get(i), pose.get(i+1)) doesn't contain
			// white interval.
			// we can just ignore if(first && !result).
			
			if (first && result) {
				// first segment on the white interval
				first = false;
				startIndex = i - 1;
				cStart = edge.getCurveStart();
				vStart = edge.getEdgeStart();
				
				// if the white interval ends within the same segment
				if (edge.getCurveEnd() < 1) {
					this.setEndPointsOnEdge(edge, startIndex, i, cStart, vStart);
					return;
				}
			} else if (!first && result) {
				// not the first segment on the white interval
				if (edge.getCurveEnd() < 1) {
					// if the white interval ends within that segment
					this.setEndPointsOnEdge(edge, startIndex, i, cStart, vStart);
					return;
				}
			} else if (!first) {
				// the white interval ends at 1.0 of previous segment
				this.setEndPointsOnEdge(edge, startIndex, i, cStart, vStart);
				return;
			}
		}
		
		if (first) {
			// if the last segment on the curve is the first segment of that
			// interval
			edge.setCurveEndIndex(pose.size());
			edge.setDone(true);
		} else {
			edge.setCurveStartIndex(startIndex);
			edge.setCurveStart(startIndex + cStart);
			edge.setEdgeStart(vStart);
			
			edge.setCurveEnd(pose.size() - 2 + edge.getCurveEnd());
			edge.setCurveEndIndex(pose.size() - 2);
		}
		
	}
	
	/**
	 * Updates constructedMap by adding an EdgeTM. Detail description of the
	 * algorithm is in the publication.
	 */
	private void updateMap(List<VertexTM> constructedMap, Map<String, Integer> map, EdgeTM edge) {
		
		// update the map by adding a new edge
		VertexTM v;
		int parent = -1;
		int child = -1;
		
		String keyParent = edge.getVertex1().toString();
		String keyChild = edge.getVertex2().toString();
		// find the index of parent node
		if (map.containsKey(keyParent)) {
			parent = map.get(keyParent);
		} else {
			v = edge.getVertex1();
			constructedMap.add(v);
			parent = constructedMap.indexOf(v);
			map.put(keyParent, parent);
		}
		// find the index of child node
		if (map.containsKey(keyChild)) {
			child = map.get(keyChild);
		} else {
			v = edge.getVertex2();
			constructedMap.add(v);
			child = constructedMap.indexOf(v);
			map.put(keyChild, child);
		}
		// update the map
		if (parent == -1 || child == -1) {
			LOG.error("inconsistent graph child, parent :" + child + ", " + parent);
		} else if (parent != child) {
			constructedMap.get(parent).addElementAdjList(child);
			constructedMap.get(child).addElementAdjList(parent);
			
			LOG.debug("child, parent :" + child + ", " + parent);
			LOG.debug("child, parent :" + parent + ", " + child);
		}
	}
	
	/**
	 * Adds a split point on an EdgeTM.
	 *
	 * @param newVertexPosition represents position of a new VertexTM
	 */
	private void edgeSplit(List<VertexTM> constructedMap, Map<String, Integer> map, EdgeTM edge, double newVertexPosition) {
		
		VertexTM v1 = edge.getVertex1();
		VertexTM v2 = edge.getVertex2();
		
		String key1 = v1.toString();
		String key2 = v2.toString();
		
		// call of this method always after updateMap which ensures
		// map.containsKey(key1) is
		// always true.
		int index1 = map.get(key1);
		int index2 = map.get(key2);
		
		VertexTM v = edge.getLine().getVertex(newVertexPosition);
		
		// splitting an edge on split point vertex v
		
		String key = v.toString();
		
		int index = map.get(key);
		
		if (index == index1 || index == index2) {
			return;
		}
		
		LOG.debug("Index = " + index1 + " " + index2 + " " + index);
		
		edge.addSplit(newVertexPosition, index);
	}
	
	/**
	 * Commits edge splitting listed in List<Integer> EdgeTM.edgeSplitVertices.
	 */
	
	private void commitEdgeSplits(List<EdgeTM> edges, Map<String, Integer> map, List<VertexTM> graph) {
		
		if (edges.size() != 2) {
			// logger.log(Level.SEVERE, "created.");
			return;
		}
		
		EdgeTM edge = edges.get(0);
		
		for (int i = 0; i < edges.get(1).getEdgeSplitPositions().size(); i++) {
			double newPosition = 1 - edges.get(1).getEdgeSplitPositions().get(i);
			edge.addSplit(newPosition,
					edges.get(1).getEdgeSplitVertices().get(i));
		}
		
		List<Integer> edgeVertexSplits = edge.getEdgeSplitVertices();
		int splitSize = edgeVertexSplits.size();
		
		if (splitSize == 0) {
			return;
		}
		
		VertexTM v1 = edge.getVertex1();
		VertexTM v2 = edge.getVertex2();
		
		String key1 = v1.toString();
		String key2 = v2.toString();
		
		int index1 = map.get(key1);
		int index2 = map.get(key2);
		
		boolean updateV1 = false, updateV2 = false;
		
		LOG.debug("commitEdgeSplits " + splitSize);
		
		for (int i = 0; i < v1.getDegree(); i++) {
			if (v1.getAdjacentElementAt(i) == index2) {
				v1.setAdjacentElementAt(i, edgeVertexSplits.get(0));
				graph.get(edgeVertexSplits.get(0)).addElementAdjList(index1);
				updateV1 = true;
			}
		}
		
		for (int i = 0; i < v2.getDegree(); i++) {
			if (v2.getAdjacentElementAt(i) == index1) {
				v2.setAdjacentElementAt(i, edgeVertexSplits.get(splitSize - 1));
				graph.get(edgeVertexSplits.get(splitSize - 1)).addElementAdjList(index2);
				updateV2 = true;
			}
		}
		
		for (int i = 0; i < splitSize - 1; i++) {
			int currentVertex = edgeVertexSplits.get(i);
			int nextVertex = edgeVertexSplits.get(i + 1);
			graph.get(currentVertex).addElementAdjList(nextVertex);
			graph.get(nextVertex).addElementAdjList(currentVertex);
		}
		if (!(updateV1 && updateV2)) {
			LOG.error("inconsistent graph: (" + splitSize + ")" + index1 + " " + index2 + " " + v1.getAdjacencyList().toString() + " "
					+ v2.getAdjacencyList().toString());
		}
	}
	
	/**
	 * Commits edge splitting for all edges.
	 */
	
	private void commitEdgeSplitsAll(List<VertexTM> constructedMap, Map<String, Integer> map, Map<String, ArrayList<EdgeTM>> siblingMap,
									 List<EdgeTM> edges) {
		for (EdgeTM edge : edges) {
			String key1 = edge.getVertex1().toString() + " " + edge.getVertex2().toString();
			String key2 = edge.getVertex2().toString() + " " + edge.getVertex1().toString();
			
			ArrayList<EdgeTM> siblings1, siblings2;
			if (siblingMap.containsKey(key1))
				siblings1 = siblingMap.get(key1);
			else {
				siblings1 = new ArrayList<>();
			}
			if (siblingMap.containsKey(key2))
				siblings2 = siblingMap.get(key2);
			else {
				siblings2 = new ArrayList<>();
			}
			if (siblings1.size() != 0) {
				this.commitEdgeSplits(siblings1, map, constructedMap);
				siblingMap.remove(key1);
			} else if (siblings2.size() != 0) {
				this.commitEdgeSplits(siblings2, map, constructedMap);
				siblingMap.remove(key2);
			}
		}
	}
	
	/**
	 * Adds a portion of a pose as edges into constructedMap.
	 */
	private void addToGraph(List<VertexTM> constructedMap, List<VertexTM> pose, Map<String, Integer> map, int startIndex, int endIndex) {
		for (int i = startIndex; i < endIndex; i++) {
			this.updateMap(constructedMap, map, new EdgeTM(pose.get(i), pose.get(i + 1)));
		}
		
	}
	
	/**
	 * Updates siblingHashMap for an edge.
	 */
	
	private void updateSiblingHashMap(Map<String, ArrayList<EdgeTM>> siblingMap, EdgeTM edge) {
		String key1 = edge.getVertex1().toString() + " " + edge.getVertex2().toString();
		String key2 = edge.getVertex2().toString() + " " + edge.getVertex1().toString();
		Collection<EdgeTM> siblings1, siblings2;
		if (siblingMap.containsKey(key1)) {
			siblings1 = siblingMap.get(key1);
		} else {
			siblings1 = new ArrayList<>();
		}
		if (siblingMap.containsKey(key1)) {
			siblings2 = siblingMap.get(key2);
		} else {
			siblings2 = new ArrayList<>();
		}
		
		if (siblings1.size() == 0 && siblings2.size() == 0) {
			siblingMap.put(key1, new ArrayList<>());
			siblingMap.get(key1).add(edge);
		} else if (siblings1.size() != 0) {
			siblings1.add(edge);
		} else {
			siblings2.size();
			siblings2.add(edge);
		}
	}
	
	/**
	 * Update the map for a pose/curve. Definition of black and white interval.
	 */
	// @TODO(mahmuda): extract some shorter well-named methods.
	private void mapConstruction(List<VertexTM> constructedMap, List<EdgeTM> edges, Map<String, Integer> map, Trajectory traj, double eps,
								 DistanceFunction df) {
		
		PriorityQueue<EdgeTM> pq = new PriorityQueue<>();
		
		List<VertexTM> poseList = new ArrayList<>();
		for (TrajectoryPoint p : traj) {
			poseList.add(new VertexTM(p.x(), p.y(), p.time(), p.getDistanceFunction()));
		}
		for (EdgeTM value : edges) {
			this.computeNextInterval(value, poseList, 1, eps);
			if (!value.getDone()) {
				pq.add(value);
			}
		}
		try {
			
			// The whole curve will be added as an edge because no white
			// interval
			
			if (pq.isEmpty()) {
				
				LOG.debug(TraceMergeMapInference.curveName + " inserted as an edge");
				
				this.addToGraph(constructedMap, poseList, map, 0, poseList.size() - 1);
				
				LOG.debug(TraceMergeMapInference.curveName + " inserted as an edge");
				return;
			}
			
			EdgeTM edge = pq.poll();
			
			double cEnd = edge.getCurveEnd();
			EdgeTM cEdge = edge;
			
			// There is a black interval until edge.curveStart
			
			if (edge.getCurveStart() > 0) {
				
				LOG.debug(TraceMergeMapInference.curveName + " inserted as an edge until " + edge.getCurveStart());
				
				int index = (int) Math.floor(edge.getCurveStart());
				
				this.addToGraph(constructedMap, poseList, map, 0, index);
				
				LineTM newLine = new LineTM(poseList.get(index), poseList.get(index + 1), df);
				double t = edge.getCurveStart() - Math.floor(edge.getCurveStart());
				this.updateMap(constructedMap, map, new EdgeTM(poseList.get(index), newLine.getVertex(t)));
				
				this.updateMap(constructedMap, map, new EdgeTM(newLine.getVertex(t), edge.getLine().getVertex(edge.getEdgeStart())));
				this.edgeSplit(constructedMap, map, edge, edge.getEdgeStart());
			}
			
			// the while loop will search through all the intervals until we
			// reach the end of the pose
			
			while (cEnd < poseList.size()) {
				
				LOG.debug(TraceMergeMapInference.curveName + " has white interval " + edge.getCurveStart() + " " + edge.getCurveEnd() +
						" " + cEnd);
				
				if (cEnd < edge.getCurveEnd()) {
					cEnd = edge.getCurveEnd();
					cEdge = edge;
				}
				
				if (edge.getCurveEnd() == poseList.size() - 1) {
					LOG.debug(TraceMergeMapInference.curveName + " processing completed.");
					return;
				}
				
				this.computeNextInterval(edge, poseList, edge.getCurveEndIndex() + 1, eps);
				
				if (!edge.getDone()) {
					pq.add(edge);
				}
				
				if (pq.isEmpty()) {
					LOG.debug(TraceMergeMapInference.curveName + " inserted as an edge from " + cEnd + " to end");
					
					int index = (int) Math.floor(cEnd);
					LineTM newLine = new LineTM(poseList.get(index), poseList.get(index + 1), df);
					double t = cEnd - Math.floor(cEnd);
					this.updateMap(constructedMap, map, new EdgeTM(cEdge.getLine().getVertex(cEdge.getEdgeEnd()), newLine.getVertex(t)));
					this.edgeSplit(constructedMap, map, cEdge, cEdge.getEdgeEnd());
					this.updateMap(constructedMap, map, new EdgeTM(newLine.getVertex(t), poseList.get(index + 1)));
					this.addToGraph(constructedMap, poseList, map, index + 1, poseList.size() - 1);
					return;
				}
				
				edge = pq.poll();
				
				if (edge.getCurveStart() > cEnd) {
					LOG.debug(TraceMergeMapInference.curveName + " inserted as an edge from " + cEnd + " to " + edge.getCurveStart());
					
					// need to add rest of the line segment
					
					int index = (int) Math.floor(cEnd);
					int indexStart = (int) Math.floor(edge.getCurveStart());
					LineTM newLine = new LineTM(poseList.get(index), poseList.get(index + 1), df);
					double t = cEnd - Math.floor(cEnd);
					
					this.updateMap(constructedMap, map, new EdgeTM(cEdge.getLine().getVertex(cEdge.getEdgeEnd()), newLine.getVertex(t)));
					this.edgeSplit(constructedMap, map, cEdge, cEdge.getEdgeEnd());
					
					if (index == indexStart) {
						this.updateMap(constructedMap, map, new EdgeTM(newLine.getVertex(t), newLine.getVertex(edge.getCurveStart() - index)));
						index = (int) Math.floor(edge.getCurveStart());
						newLine = new LineTM(poseList.get(index), poseList.get(index + 1), df);
						t = edge.getCurveStart() - Math.floor(edge.getCurveStart());
					} else {
						this.updateMap(constructedMap, map, new EdgeTM(newLine.getVertex(t), poseList.get(index + 1)));
						
						this.addToGraph(constructedMap, poseList, map, index + 1, (int) Math.floor(edge.getCurveStart()));
						index = (int) Math.floor(edge.getCurveStart());
						newLine = new LineTM(poseList.get(index), poseList.get(index + 1), df);
						t = edge.getCurveStart() - Math.floor(edge.getCurveStart());
						this.updateMap(constructedMap, map, new EdgeTM(poseList.get(index), newLine.getVertex(t)));
						
					}
					this.updateMap(constructedMap, map, new EdgeTM(newLine.getVertex(t), edge.getLine().getVertex(edge.getEdgeStart())));
					this.edgeSplit(constructedMap, map, edge, edge.getEdgeStart());
				}
			}
		} catch (Exception ex) {
			LOG.error(ex.toString());
			ex.printStackTrace();
		}
	}
	
	/**
	 * Constructs map from poses and returns string representation of the map.
	 */
	public RoadNetworkGraph mapInferenceProcess(List<Trajectory> inputTrajList, double eps, DistanceFunction df) {
		
		List<VertexTM> constructedMap = new ArrayList<>();
		// map contains mapping between vertex keys and their indices in
		// constructedMap
		Map<String, Integer> map = new HashMap<>();
		long startTime = System.currentTimeMillis();
		try {
			double length = 0;
			
			// generate list of files in the folder to process
			for (int k = 0; k < inputTrajList.size(); k++) {
				
				TraceMergeMapInference.curveName = inputTrajList.get(k).getID();
				
				length += inputTrajList.get(k).length();
				
				if (inputTrajList.get(k).size() < 2) {
					continue;
				}
				
				List<EdgeTM> edges = new ArrayList<>();
				
				/*
				 * siblingMap contains map of key and sibling edges, sibling
				 * edges are line segments between two vertices but going in
				 * opposite direction.
				 */
				Map<String, ArrayList<EdgeTM>> siblingMap = new HashMap<>();
				
				for (VertexTM v : constructedMap) {
					for (int j = 0; j < v.getDegree(); j++) {
						VertexTM v1 = constructedMap.get(v.getAdjacentElementAt(j));
						if (!v.equals(v1)) {
							EdgeTM newEdge = new EdgeTM(v, v1);
							edges.add(newEdge);
							updateSiblingHashMap(siblingMap, newEdge);
						}
					}
				}
				
				this.mapConstruction(constructedMap, edges, map, inputTrajList.get(k), eps, df);
				this.commitEdgeSplitsAll(constructedMap, map, siblingMap, edges);
				if (inputTrajList.size() > 100 && k % Math.floor(inputTrajList.size() / 100) == 0)
					LOG.info(k / Math.floor(inputTrajList.size() / 100) + " percent of map inference finished. Time spent: "
							+ (System.currentTimeMillis() - startTime) / 1000);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		RoadNetworkGraph resultMap = new RoadNetworkGraph(false, df);
		List<RoadNode> nodeList = new ArrayList<>();
		List<RoadWay> wayList = new ArrayList<>();
		Map<String, RoadNode> id2NodeMapping = new HashMap<>();
		
		int count = 0;
		// insert road nodes
		for (int i = 0; i < constructedMap.size(); i++) {
			RoadNode currNode = new RoadNode(i + "", constructedMap.get(i).getX(), constructedMap.get(i).getY(), df);
			id2NodeMapping.put(i + "", currNode);
			nodeList.add(currNode);
		}
		
		// insert road ways
		for (int i = 0; i < constructedMap.size(); i++) {
			VertexTM v = constructedMap.get(i);
			for (int j = 0; j < v.getDegree(); j++) {
				if (i != v.getAdjacentElementAt(j)) {
					
					RoadWay currWay = new RoadWay(count + "", df);
					if (id2NodeMapping.containsKey(i + "") && id2NodeMapping.containsKey(v.getAdjacentElementAt(j) + "")) {
						currWay.addNode(id2NodeMapping.get(i + ""));
						currWay.addNode(id2NodeMapping.get(v.getAdjacentElementAt(j) + ""));
						wayList.add(currWay);
					} else
						LOG.error("The end point of the road way is not found in the node list: " + i + "," + v.getAdjacentElementAt(j));
					
					count++;
				}
			}
		}
		
		resultMap.setNodes(nodeList);
		resultMap.addWays(wayList);
		
		return resultMap;
	}
}
