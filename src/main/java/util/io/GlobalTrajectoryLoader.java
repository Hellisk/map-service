package util.io;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.object.spatialobject.Trajectory;
import util.object.spatialobject.TrajectoryPoint;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Map-matching reader from the raw data of <tt>Global</tt> dataset.
 *
 * @author Hellisk
 * @since 23/05/2017
 */
public class GlobalTrajectoryLoader {
	
	private static final Logger LOG = Logger.getLogger(GlobalTrajectoryLoader.class);
	private String inputFolder;
	private boolean isInitialized = false;  // initialization of trajectory feature map
	private Map<Integer, Map<String, String>> trajectoryFeature = new HashMap<>();
	private int numOfTrajectory;
	private DistanceFunction distFunc = new GreatCircleDistanceFunction();
	
	public GlobalTrajectoryLoader(String inputPath) {
		this.inputFolder = inputPath;
		init();
	}
	
	static String stringFormatter(int trajNum) {
		String numberAsString = String.valueOf(trajNum);
		StringBuilder sb = new StringBuilder();
		while (sb.length() + numberAsString.length() < 8) {
			sb.append('0');
		}
		sb.append(numberAsString);
		return sb.toString();
	}
	
	public Trajectory readInputTrajectory(int trajNum) throws IOException {
		if (!isInitialized)
			throw new RuntimeException("XML reader must be initialized before reading.");
		if (trajNum >= numOfTrajectory)
			throw new IndexOutOfBoundsException("trajectory ID exceeds the current count.");
		
		BufferedReader brTrajectory = new BufferedReader(new FileReader(this.inputFolder + stringFormatter
				(trajNum) + File.separator + stringFormatter(trajNum) + ".track"));
		Trajectory newTrajectory = new Trajectory(trajNum + "", distFunc);
		String line;
		while ((line = brTrajectory.readLine()) != null) {
			String[] pointInfo = line.split("\t");
			TrajectoryPoint newTrajectoryPoint = new TrajectoryPoint(Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1]),
					(long) Double.parseDouble(pointInfo[2]), distFunc);
			newTrajectory.add(newTrajectoryPoint);
		}
		brTrajectory.close();
		return newTrajectory;
	}
	
	public void init() {
		if (isInitialized)
			throw new RuntimeException("The Global dataset reader has been initialized twice.");
		// read metadata file
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			File metadata = new File(this.inputFolder + "metadata.xml");
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(metadata);
			doc.getDocumentElement().normalize();
			
			NodeList nList = doc.getElementsByTagName("record");
			this.numOfTrajectory = nList.getLength();
			
			for (int r = 0; r < nList.getLength(); r++) {
				
				Node nNode = nList.item(r);
				
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					
					Element eElement = (Element) nNode;
					
					int recordID = Integer.parseInt(eElement.getAttribute("id"));
					if (recordID != r)
						throw new RuntimeException("The extracted record information doesn't correspond to the record ID");
					Map<String, String> currFeatures = new HashMap<>();
					currFeatures.put("state", eElement.getAttribute("id"));
					Element spatialMismatch = (Element) eElement.getElementsByTagName("spatial-mismatch").item(0);
					currFeatures.put("mad", spatialMismatch.getAttribute("mad"));
					currFeatures.put("max", spatialMismatch.getAttribute("max"));
					
					NodeList featureList = ((Element) nNode).getElementsByTagName("tag");
					for (int f = 0; f < featureList.getLength(); f++) {
						Element feature = (Element) featureList.item(f);
						if (feature.getAttribute("key").equals("feature")) {
							currFeatures.put(feature.getAttribute("value"), "");
						} else
							LOG.info(feature.getAttribute("key"));
					}
					trajectoryFeature.put(r, currFeatures);
				} else
					LOG.info(nNode.getNodeName() + " is not a record");
			}
			LOG.info("Initialization finished.");
			isInitialized = true;
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Read the ground-truth route matching result of a trajectory with given ID.
	 *
	 * @param trajNum The trajectory ID.
	 * @return The ground-truth route match of the given trajectory.
	 * @throws IOException Fail to read file.
	 */
	public List<String> readGTRouteMatchResult(int trajNum) throws IOException {
		if (!isInitialized)
			throw new RuntimeException("The Global dataset loader must be initialized before reading.");
		if (trajNum >= numOfTrajectory)
			throw new IndexOutOfBoundsException("Trajectory ID exceeds the current count.");
		
		BufferedReader brMatchResult = new BufferedReader(new FileReader(this.inputFolder + stringFormatter
				(trajNum) + File.separator + stringFormatter(trajNum) + ".route"));
		List<String> groundTruthMatchResult = new ArrayList<>();
		String line;
		while ((line = brMatchResult.readLine()) != null) {
			groundTruthMatchResult.add(line);
		}
		brMatchResult.close();
		return groundTruthMatchResult;
	}
	
	public Map<String, String> findTrajectoryInfo(int trajNum) {
		if (trajectoryFeature.containsKey(trajNum))
			return trajectoryFeature.get(trajNum);
		else throw new NullPointerException("The requested trajectory is not found");
	}
	
	public int getNumOfTrajectory() {
		return this.numOfTrajectory;
	}
}
