package util.settings;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import util.io.IOService;

import java.io.InputStream;

/**
 * Log entry for simple file logs using log4j package
 *
 * @author Hellisk
 * @since 25/02/2019
 */
public class MapServiceLogger {
	
	/**
	 * Initialise the log file of the project given the file path and name
	 *
	 * @param logPath  The file path for storing logs. All the path should end with "/"
	 * @param fileName The file name for the log without extension ".log"
	 */
	public static void logInit(String logPath, String fileName) {
		System.setProperty("logfile.name", logPath + fileName + ".log");
		// create the log folder if not exist, set the log file name
		IOService.createFolder(logPath);
		// read the log settings from property file
		InputStream in = MapServiceLogger.class.getClassLoader().getResourceAsStream("log4j.properties");
		PropertyConfigurator.configure(in); // refresh the log file path property
		System.setProperty("log4j.rootLogger", "INFO, file, stdout");
		final Logger LOG = Logger.getLogger(MapServiceLogger.class);   // log entry
		LOG.debug("Log initialization done.");
	}
}
