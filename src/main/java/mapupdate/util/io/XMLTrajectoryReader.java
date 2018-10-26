package mapupdate.util.io;

import mapupdate.util.object.spatialobject.STPoint;
import mapupdate.util.object.spatialobject.Trajectory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

import static mapupdate.Main.LOGGER;

/**
 * Created by uqpchao on 23/05/2017.
 */
public class XMLTrajectoryReader {
    private String inputFolder;
    private boolean isInitialized = false;  // initialization of trajectory feature map
    private Map<Integer, Map<String, String>> trajectoryFeature = new HashMap<>();
    private int numOfTrajectory;

    public XMLTrajectoryReader(String inputPath) {
        this.inputFolder = inputPath;
        init();
    }

    public Trajectory readInputTrajectory(int trajNum) throws IOException {
        if (!isInitialized)
            throw new RuntimeException("XML reader must be initialized before reading.");
        if (trajNum >= numOfTrajectory)
            throw new IndexOutOfBoundsException("trajectory ID exceeds the current count.");

        BufferedReader brTrajectory = new BufferedReader(new FileReader(this.inputFolder + stringFormatter
                (trajNum) + File.separator + stringFormatter
                (trajNum) +
                ".track"));
        Trajectory newTrajectory = new Trajectory(trajNum + "");
        String line;
        while ((line = brTrajectory.readLine()) != null) {
            String[] pointInfo = line.split("\t");
            STPoint newSTPoint = new STPoint(Double.parseDouble(pointInfo[0]), Double.parseDouble(pointInfo[1]), (long) Double.parseDouble(pointInfo[2]));
            newTrajectory.add(newSTPoint);
        }
        brTrajectory.close();
        return newTrajectory;
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

    public void init() {
        if (isInitialized)
            throw new RuntimeException("XML reader has been initialized twice.");
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
                            LOGGER.info(feature.getAttribute("key"));
                    }
                    trajectoryFeature.put(r, currFeatures);
                } else
                    LOGGER.info(nNode.getNodeName() + " is not a record");
            }
            LOGGER.info("Initialization finished.");
            isInitialized = true;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

    }

    public List<String> readGroundTruthMatchResult(int trajNum) throws IOException {
        if (!isInitialized)
            throw new RuntimeException("XML reader must be initialized before reading.");
        if (trajNum >= numOfTrajectory)
            throw new IndexOutOfBoundsException("trajectory ID exceeds the current count.");

        BufferedReader brMatchResult = new BufferedReader(new FileReader(this.inputFolder + stringFormatter
                (trajNum) + File.separator + stringFormatter
                (trajNum) + ".route"));
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
