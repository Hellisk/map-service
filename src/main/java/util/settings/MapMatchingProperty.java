package util.settings;

import java.io.IOException;
import java.io.InputStream;

/**
 * Property class for map-matching algorithm.
 *
 * @author Hellisk
 * @since 30/03/2019
 */
public class MapMatchingProperty extends BaseProperty {
	@Override
	public void parseProperties(InputStream input, String[] args) {
		
		// load properties into the Properties instance
		try {
			super.pro.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// update the properties according to the command line input
		if (args != null) {
			for (String arg : args) {
				if (arg.charAt(0) == '-') {
					if (arg.length() < 4)
						throw new IllegalArgumentException("Invalid argument: " + arg);
					switch (arg.substring(1, 3)) {
						case "os":
							super.setProperty("OS", arg.substring(3));
							break;
						case "dd":
							super.setProperty("data.Dataset", arg.substring(3));
							break;
						case "dt":
							super.setProperty("data.TrajectoryMinimalLengthSec", arg.substring(3));
							break;
						case "ds":
							super.setProperty("data.SampleMaximalIntervalSec", arg.substring(3));
							break;
						case "dn":
							super.setProperty("data.NumberOfTrajectory", arg.substring(3));
							break;
						case "mc":
							super.setProperty("algorithm.mapmatching.hmm.CandidateRange", arg.substring(3));
							break;
						case "mb":
							super.setProperty("algorithm.mapmatching.hmm.Beta", arg.substring(3));
							break;
						case "ms":
							super.setProperty("algorithm.mapmatching.hmm.Sigma", arg.substring(3));
							break;
						default:
							throw new IllegalArgumentException("Invalid argument: " + arg);
					}
				} else {
					throw new IllegalArgumentException("Invalid argument: " + arg);
				}
			}
		}
		
		// parse the input file folder
		String os = super.getPropertyString("OS");
		String dataset = super.getPropertyString("data.Dataset");
		String rootPath = super.getPropertyString("data.RootPath");
		switch (os) {
			case "Win":     // performed on either school or home computer
				if (dataset.contains("Global"))
					rootPath += dataset + "/";
				else
					throw new IllegalArgumentException("Wrong property value: data.Dataset=" + dataset);
				break;
			case "Linux":   // performed on server
				if (dataset.contains("Global"))
					rootPath = "/home/uqpchao/data/" + dataset + "/";
				else
					throw new IllegalArgumentException("Wrong property value: data.Dataset=" + dataset);
				break;
			default:
				throw new IllegalArgumentException("Wrong property value: OS=" + os);
		}
		
		// set the bounding box property
		if (dataset.contains("Beijing")) {
			String size = dataset.substring(dataset.lastIndexOf('-') + 1);
			super.setProperty("data.BoundingBox", super.getPropertyString("data.BoundingBox" + size));
		}
		
		String dataSpec;
		if (dataset.contains("Beijing")) {
			// folder name for different data specification
			dataSpec = "L" + super.getPropertyString("data.TrajectoryMinimalLengthSec")
					+ "_I" + super.getPropertyString("data.SampleMaximalIntervalSec")
					+ "_N" + super.getPropertyString("data.NumberOfTrajectory");
		} else {
			dataSpec = "";
		}
		// different paths in Beijing dataset
		super.setProperty("path.RawDataFolder", rootPath + "raw/");
		super.setProperty("path.InputTrajectoryFolder", rootPath + "input/trajectory/" + dataSpec + "/");
		super.setProperty("path.InputMapFolder", rootPath + "input/map/");
		super.setProperty("path.OutputMatchResultFolder", rootPath + "output/matchResult/" + dataSpec + "/");
		super.setProperty("path.GroundTruthMatchResultFolder", rootPath + "groundTruth/matchResult/" + dataSpec + "/");
		super.setProperty("algorithm.mapmatching.path.CacheFolder", rootPath + "matching/cache/");
		super.setProperty("algorithm.mapmatching.log.LogFolder", rootPath + "matching/log/");
		super.setProperty("data.DataSpec", dataSpec);
		// set the root path for all map-matching processes
		super.setProperty("algorithm.mapmatching.path.RootPath", rootPath);
	}
}