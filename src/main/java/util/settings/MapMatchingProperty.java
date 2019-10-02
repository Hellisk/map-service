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

        // updateGoh the properties according to the command line input
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
						case "dy":
							if (arg.substring(3).equals("true") || arg.substring(3).equals("false"))
								super.setProperty("data.IsSyntheticTrajectory", arg.substring(3));
							else
								throw new IllegalArgumentException("The \"data.IsSyntheticTrajectory\" argument incorrect: " + arg.substring(3));
						case "di":
							super.setProperty("data.Sigma", arg.substring(3));
							break;
						case "dr":
							super.setProperty("data.SamplingInterval", arg.substring(3));
							break;
						case "do":
							super.setProperty("data.OutlierPct", arg.substring(3));
							break;
						case "mc":
							super.setProperty("algorithm.mapmatching.CandidateRange", arg.substring(3));
							break;
						case "mb":
							super.setProperty("algorithm.mapmatching.hmm.Beta", arg.substring(3));
							break;
						case "ms":
							super.setProperty("algorithm.mapmatching.Sigma", arg.substring(3));
							break;
						case "mm":
							super.setProperty("algorithm.mapmatching.MatchingMethod", arg.substring(3));
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
				rootPath += dataset + "/";
				break;
			case "Linux":   // performed on server
				rootPath += dataset + "/";
				break;
			default:
				throw new IllegalArgumentException("Wrong property value: OS=" + os);
		}
		
		// set the bounding box property
		if (dataset.contains("Beijing")) {
			String size = dataset.substring(dataset.lastIndexOf('-') + 1);
			super.setProperty("data.BoundingBox", super.getPropertyString("data.BoundingBox" + size));
		}
		
		String dataSpec;        // used for setting the input and log folder
		String rawSpec;        // used to find the corresponding folder for synthetic data generation
		if (dataset.contains("Beijing") && super.getPropertyBoolean("data.IsSyntheticTrajectory")) {
			// folder name for different data specification
			dataSpec = "L" + super.getPropertyString("data.TrajectoryMinimalLengthSec")
					+ "_I" + super.getPropertyString("data.SampleMaximalIntervalSec")
					+ "_N" + super.getPropertyString("data.NumberOfTrajectory")
					+ "_S" + super.getPropertyInteger("data.Sigma")
					+ "_R" + super.getPropertyInteger("data.SamplingInterval")
					+ "_O" + super.getPropertyInteger("data.OutlierPct");
			
			rawSpec = "L" + super.getPropertyString("data.TrajectoryMinimalLengthSec")
					+ "_I" + super.getPropertyString("data.SampleMaximalIntervalSec")
					+ "_N" + super.getPropertyString("data.NumberOfTrajectory");
		} else if (dataset.contains("Beijing")) {
			// folder name for different data specification
			dataSpec = "L" + super.getPropertyString("data.TrajectoryMinimalLengthSec")
					+ "_I" + super.getPropertyString("data.SampleMaximalIntervalSec")
					+ "_N" + super.getPropertyString("data.NumberOfTrajectory");
			rawSpec = dataSpec;
		} else {
			dataSpec = "";
			rawSpec = dataSpec;
		}
		
		// different paths in Beijing dataset
		if (dataset.contains("Beijing")) {
			super.setProperty("path.RawDataFolder", super.getPropertyString("data.RootPath") + "Beijing/raw/");
		} else
			super.setProperty("path.RawDataFolder", rootPath + "raw/");        // apply to Global data only
		super.setProperty("path.InputOriginalTrajectoryFolder", rootPath + "input/trajectory/" + (rawSpec.equals("") ? "" : rawSpec + "/"));
		super.setProperty("path.InputTrajectoryFolder", rootPath + "input/trajectory/" + (dataSpec.equals("") ? "" : dataSpec + "/"));    // doesn't work for Global,
		// use raw data directly
		super.setProperty("path.InputMapFolder", rootPath + "input/map/");
		super.setProperty("path.GroundTruthMapFolder", rootPath + "groundTruth/map/");
		super.setProperty("path.OutputMatchResultFolder", rootPath + "output/matchResult/" + (dataSpec.equals("") ? "" : dataSpec + "/"));
		super.setProperty("path.GroundTruthRouteMatchResultFolder",
				rootPath + "groundTruth/matchResult/route/" + (dataSpec.equals("") ? "" : dataSpec + "/"));
		super.setProperty("path.GroundTruthPointMatchResultFolder",
				rootPath + "groundTruth/matchResult/point/" + (dataSpec.equals("") ? "" : dataSpec + "/"));
		super.setProperty("path.GroundTruthOriginalRouteMatchResultFolder",
				rootPath + "groundTruth/matchResult/route/" + (rawSpec.equals("") ? "" : rawSpec + "/"));
		super.setProperty("path.GroundTruthSyntheticRouteMatchBaseFolder", rootPath + "groundTruth/matchResult/route/" + rawSpec);
		super.setProperty("algorithm.mapmatching.path.CacheFolder", rootPath + "matching/cache/");
		super.setProperty("algorithm.mapmatching.log.LogFolder", rootPath + "matching/log/");
		super.setProperty("data.DataSpec", dataSpec);
		// set the root path for all map-matching processes
		super.setProperty("algorithm.mapmatching.path.RootPath", rootPath);
	}
}