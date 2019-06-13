package util.settings;

import java.io.IOException;
import java.io.InputStream;

/**
 * Property class for map-trajectory co-optimization algorithm.
 *
 * @author Hellisk
 * @since 25/02/2019
 */
public class CoOptimizationProperty extends BaseProperty {
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
						case "dr":
							super.setProperty("data.RoadRemovalPercentage", arg.substring(3));
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
						case "cc":
							super.setProperty("algorithm.cooptimization.CorrectRoadPercentage", arg.substring(3));
							break;
						case "cg":
							super.setProperty("algorithm.cooptimization.GapExtensionDistance", arg.substring(3));
							break;
						case "ci":
							super.setProperty("algorithm.cooptimization.IndexFilter", arg.substring(3));
							break;
						case "mr":
							super.setProperty("algorithm.mapmatching.hmm.RankLength", arg.substring(3));
							break;
						case "mc":
							super.setProperty("algorithm.mapmatching.hmm.CandidateRange", arg.substring(3));
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
				if (dataset.contains("Beijing"))
					rootPath += dataset + "/";
				else
					throw new IllegalArgumentException("Wrong property value: data.Dataset=" + dataset);
				break;
			case "Linux":   // performed on server
				if (dataset.contains("Beijing"))
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
		
		// folder name for different data specification
		String dataSpec = "L" + super.getPropertyString("data.TrajectoryMinimalLengthSec")
				+ "_I" + super.getPropertyString("data.SampleMaximalIntervalSec")
				+ "_N" + super.getPropertyString("data.NumberOfTrajectory") + "/";
		
		// different paths in Beijing dataset
		super.setProperty("path.RawDataFolder", rootPath + "raw/");
		super.setProperty("path.InputTrajectoryFolder", rootPath + "input/trajectory/" + dataSpec);
		super.setProperty("path.InputMapFolder", rootPath + "input/map/");
		super.setProperty("path.OutputMapFolder", rootPath + "output/map/");
		super.setProperty("path.OutputMatchResultFolder", rootPath + "output/matchResult/" + dataSpec);
		super.setProperty("path.GroundTruthMapFolder", rootPath + "groundTruth/map/");
		super.setProperty("path.GroundTruthMatchResultFolder", rootPath + "groundTruth/matchResult/" + dataSpec);
		super.setProperty("path.GroundTruthManualMatchResultFolder", rootPath + "groundTruth/matchResultM/" + dataSpec);
		super.setProperty("algorithm.cooptimization.path.CacheFolder", rootPath + "update/cache/");
		super.setProperty("algorithm.cooptimization.log.LogFolder", rootPath + "update/log/");
	}
}
