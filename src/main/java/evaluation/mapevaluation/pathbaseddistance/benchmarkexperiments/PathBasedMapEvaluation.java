package evaluation.mapevaluation.pathbaseddistance.benchmarkexperiments;

import evaluation.mapevaluation.pathbaseddistance.generatepaths.GeneratePaths;
import evaluation.mapevaluation.pathbaseddistance.mapmatching.HausdorffDistance;
import evaluation.mapevaluation.pathbaseddistance.mapmatching.MapMatching;
import evaluation.mapevaluation.pathbaseddistance.mapmatchingbasics.PBDEdge;
import evaluation.mapevaluation.pathbaseddistance.mapmatchingbasics.PBDVertex;
import org.apache.log4j.Logger;
import util.io.IOService;
import util.object.roadnetwork.RoadNetworkGraph;
import util.object.roadnetwork.RoadNode;
import util.object.roadnetwork.RoadWay;

import java.io.File;
import java.util.*;

/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Path-based distance map evaluation method proposed by
 * <p>
 * M. Ahmed, K. S. Hickmann, and C. Wenk.
 * Path-based distance for street map comparison.
 * arXiv:1309.6131, 2013.
 *
 * @author Hellisk
 * @since 05/06/2019
 */
public class PathBasedMapEvaluation {
	
	private static final Logger LOG = Logger.getLogger(PathBasedMapEvaluation.class);
	
	/**
	 * Evaluate the map similarity based on their path-based distance. The Frechet distance is used as distance function. These functions
	 * requires input to be on UTM coordination system.
	 *
	 * @param outputMap   The map constructed by algorithm.
	 * @param gtMap       The ground-truth map.
	 * @param linkLength  The length of the link per path.
	 * @param cacheFolder The folder used to temporally store path with different lengths and results.
	 * @return The returning path-based distance.
	 */
	public static String pathBasedFrechetMapEval(RoadNetworkGraph outputMap, RoadNetworkGraph gtMap, String linkLength,
												 String cacheFolder) {
		if (outputMap.getDistanceFunction().getClass() != gtMap.getDistanceFunction().getClass())
			throw new IllegalArgumentException("Input map and ground-truth map has different coordinate system.");
		GeneratePaths gp = new GeneratePaths();
		MapMatching mapMatching = new MapMatching();
		
		HashMap<String, Integer> map1 = new HashMap<>();
		HashMap<String, Integer> map2 = new HashMap<>();
		
		ArrayList<PBDVertex> outputGraph;
		ArrayList<PBDVertex> gtGraph;
		
		LOG.info("Start the path-based distance evaluation using Frechet distance.");
		
		if (outputMap.isDirectedMap() != gtMap.isDirectedMap())
			throw new IllegalArgumentException("Different map type, the constructed map is " + (outputMap.isDirectedMap() ?
					"directed" : "undirected") + " map while the ground-truth is not.");

//		int outputNonPlanarCount = outputMap.nonPlanarNodeCount();
//		if (outputNonPlanarCount != 0) {
//			LOG.info("Constructed map is non-planar map with " + outputNonPlanarCount + " non-planar nodes, convert into planar map.");
//			outputMap = outputMap.toPlanarMap();
//		}
//
//		int gtNonPlanarCount = gtMap.nonPlanarNodeCount();
//		if (gtNonPlanarCount != 0) {
//			LOG.info("Ground-truth map is non-planar map with " + gtNonPlanarCount + " non-planar nodes, convert into planar map.");
//			gtMap.toPlanarMap();
//		}
		
		LOG.info("Convert the two maps into required format.");
		outputGraph = convertMap(map1, outputMap);
		gtGraph = convertMap(map2, gtMap);
		
		String pathFolder = cacheFolder + "FDPath/";
		String resultFolder = cacheFolder + "FDResult/";
		IOService.createFolder(pathFolder);
		IOService.createFolder(resultFolder);
		
		gp.generatePathsLinkLength(outputGraph, pathFolder, linkLength);
		
		File folder = new File(pathFolder + linkLength);
		
		LOG.info("processing linkLength " + linkLength + " paths of ...");
		long start_time = System.currentTimeMillis();
		int count = 0;
		for (int l = 0; l < 5; l++) {
			File file2 = new File(pathFolder + folder.getName() + "/" + l);
			if (file2.exists()) {
				mapMatching.pathSimilarity(gtGraph, file2, resultFolder + linkLength, l);
				count += (Objects.requireNonNull(file2.listFiles())).length;
			} else {
				LOG.warn("Folder doesn't exits..." + pathFolder + linkLength);
			}
		}
		long end_time = System.currentTimeMillis();
		
		LOG.info("Path-based evaluation complete. Total running time: " + (end_time - start_time) / 1000.0);
		LOG.info("Result: " + linkLength + "," + count);
		return linkLength + "," + count;
	}
	
	public static String pathBasedHausdorffMapEval(RoadNetworkGraph outputMap, RoadNetworkGraph gtMap, String linkLength,
												   String cacheFolder) {
		if (outputMap.getDistanceFunction().getClass() != gtMap.getDistanceFunction().getClass())
			throw new IllegalArgumentException("Input map and ground-truth map has different coordinate system.");
		GeneratePaths gp = new GeneratePaths();
		HausdorffDistance hausdorffDistance = new HausdorffDistance();
		
		HashMap<String, Integer> map1 = new HashMap<>();
		HashMap<String, Integer> map2 = new HashMap<>();
		
		ArrayList<PBDVertex> outputGraph;
		ArrayList<PBDVertex> gtGraph;
		
		LOG.info("Start the path-based distance evaluation using Frechet distance.");
		
		if (outputMap.isDirectedMap() != gtMap.isDirectedMap())
			throw new IllegalArgumentException("Different map type, the constructed map is " + (outputMap.isDirectedMap() ?
					"directed" : "undirected") + " map while the ground-truth is not.");

//		int outputNonPlanarCount = outputMap.nonPlanarNodeCount();
//		if (outputNonPlanarCount != 0) {
//			LOG.info("Constructed map is non-planar map with " + outputNonPlanarCount + " non-planar nodes, convert into planar map.");
//			outputMap = outputMap.toPlanarMap();
//		}
//
//		int gtNonPlanarCount = gtMap.nonPlanarNodeCount();
//		if (gtNonPlanarCount != 0) {
//			LOG.info("Ground-truth map is non-planar map with " + gtNonPlanarCount + " non-planar nodes, convert into planar map.");
//			gtMap.toPlanarMap();
//		}
		
		LOG.info("Convert the two maps into required format.");
		outputGraph = convertMap(map1, outputMap);
		gtGraph = convertMap(map2, gtMap);
		
		String pathFolder = cacheFolder + "HDPath/";
		String resultFolder = cacheFolder + "HDResult/";
		IOService.createFolder(pathFolder);
		IOService.createFolder(resultFolder);
		
		gp.generatePathsLinkLength(outputGraph, pathFolder, linkLength);
		
		File folder = new File(pathFolder + linkLength);
		
		LOG.info("processing linkLength " + linkLength + " paths of ...");
		long start_time = System.currentTimeMillis();
		
		ArrayList<PBDEdge> eGraph = hausdorffDistance.getGraphEdge(gtGraph);
		
		int count = 0;
		for (int l = 0; l < 5; l++) {
			File file2 = new File(pathFolder + folder.getName() + "/" + l);
			
			if (file2.exists()) {
				hausdorffDistance.pathSimilarity(eGraph, file2, resultFolder + linkLength, l);
				count += (Objects.requireNonNull(file2.listFiles())).length;
			} else {
				LOG.warn("Folder doesn't exits..." + pathFolder + linkLength);
			}
			
		}
		long end_time = System.currentTimeMillis();
		
		LOG.info("Path-based Hausdorff evaluation complete. Total running time: " + (end_time - start_time) / 1000.0);
		LOG.info("Result: " + linkLength + "," + count);
		return linkLength + "," + count;
	}
	
	public static ArrayList<PBDVertex> convertMap(HashMap<String, Integer> id2VertexIndex, RoadNetworkGraph map) {
		ArrayList<PBDVertex> vList = new ArrayList<>();
		boolean isDirected = map.isDirectedMap();
		Set<String> id2NodeMap = new HashSet<>();
		for (RoadNode node : map.getAllTypeOfNodes()) {
			if (id2NodeMap.contains(node.getID()))
				throw new IllegalArgumentException("Input map contains duplicate node id: " + node.getID());
			id2NodeMap.add(node.getID());
			if (!id2VertexIndex.containsKey(node.getID())) {
				PBDVertex v = new PBDVertex(node.getID(), node.lon(), node.lat());
				vList.add(v);
				int index = vList.size() - 1;
				id2VertexIndex.put(node.getID(), index);
			} else    // should not appear as the id2NodeMap should have covered it
				throw new IllegalArgumentException("Node id: " + node.getID() + " already exists, this error should not occur.");
		}
		
		for (RoadWay way : map.getWays()) {
			for (int i = 0; i < way.getNodes().size() - 1; i++) {
				RoadNode currNode = way.getNode(i);
				RoadNode nextNode = way.getNode(i + 1);
				int index1, index2;
				if (id2VertexIndex.containsKey(currNode.getID()) && id2VertexIndex.containsKey(nextNode.getID())) {
					index1 = id2VertexIndex.get(currNode.getID());
					index2 = id2VertexIndex.get(nextNode.getID());
					vList.get(index1).addElementAdjList(index2);
					if (!isDirected)
						vList.get(index2).addElementAdjList(index1);
				} else
					throw new IllegalArgumentException("At least one endPoint of the edge does not exist: " + currNode.getID() + ","
							+ nextNode.getID());
			}
		}
		return vList;
	}
}
