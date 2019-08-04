package util.settings;

import java.io.IOException;
import java.io.InputStream;

/**
 * Property class for map-matching algorithm.
 *
 * @author Hellisk
 * @since 30/03/2019
 */
public class MapInferenceProperty extends BaseProperty {
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
						case "dc":
							super.setProperty("data.Coverage", arg.substring(3));
							break;
						case "ic":
							super.setProperty("algorithm.mapinference.kde.CellSize", arg.substring(3));
							break;
						case "ig":
							super.setProperty("algorithm.mapinference.kde.GaussianBlur", arg.substring(3));
							break;
						case "ie":
							super.setProperty("algorithm.mapinference.lineclustering.DPEpsilon", arg.substring(3));
							break;
						case "ii":
							super.setProperty("algorithm.mapinference.InferenceMethod", arg.substring(3));
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
		} else {
			super.setProperty("data.BoundingBox", super.getPropertyString("data.BoundingBox" + dataset));
		}
		String dataSpec;    // used for setting the input and log folder
		String rawSpec;        // used to find the corresponding folder for synthetic data generation
		if (dataset.contains("Beijing") && super.getPropertyBoolean("data.IsSyntheticTrajectory")) {
			// folder name for different data specification
			dataSpec = "L" + super.getPropertyString("data.TrajectoryMinimalLengthSec")
					+ "_I" + super.getPropertyString("data.SampleMaximalIntervalSec")
					+ "_N" + super.getPropertyString("data.NumberOfTrajectory")
					+ "_S" + super.getPropertyInteger("data.Sigma")
					+ "_R" + super.getPropertyInteger("data.SamplingInterval")
					+ "_C" + super.getPropertyInteger("data.Coverage");
			
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
		super.setProperty("path.RawDataFolder", rootPath + "raw/");
		if (dataset.contains("Beijing")) {
			super.setProperty("path.InputOriginalTrajectoryFolder", rootPath + "input/trajectory/" + rawSpec + "/");
			super.setProperty("path.InputTrajectoryFolder", rootPath + "input/trajectory/" + dataSpec + "/");
//			super.setProperty("path.GroundTruthRouteMatchResultFolder", rootPath + "groundTruth/matchResult/route/" + dataSpec + "/");
//			super.setProperty("path.GroundTruthPointMatchResultFolder", rootPath + "groundTruth/matchResult/point/" + dataSpec + "/");
//			super.setProperty("path.GroundTruthOriginalRouteMatchResultFolder", rootPath + "groundTruth/matchResult/route/" + rawSpec + "/");
		} else {
			super.setProperty("path.InputTrajectoryFolder", rootPath + "raw/trajectory/");
		}
		// synthetic data only usable in Beijing dataset
		super.setProperty("path.OutputMapFolder", rootPath + "output/map/");
		super.setProperty("path.GroundTruthMapFolder", rootPath + "groundTruth/map/");
		super.setProperty("algorithm.mapinference.path.CacheFolder", rootPath + "inference/cache/");
		super.setProperty("algorithm.mapinference.log.LogFolder", rootPath + "inference/log/");
		super.setProperty("data.DataSpec", dataSpec);
	}
}