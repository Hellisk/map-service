package util.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Properties;

/**
 * The abstract of the properties used in storing arguments for the program.
 *
 * @author Hellisk
 * @since 25/02/2019
 */
public abstract class BaseProperty {
	// properties class used to store all properties, including system properties.
	Properties pro = System.getProperties();
	
	/**
	 * Return the value of given property key.
	 *
	 * @param key Given property key.
	 * @return Corresponding String value.
	 */
	public String getPropertyString(String key) {
		if (pro.getProperty(key) == null)
			throw new IllegalArgumentException("Missing property: " + key);
		return pro.getProperty(key);
	}
	
	/**
	 * Parse the value of given property key into integer.
	 *
	 * @param key Given property key.
	 * @return Corresponding integer value.
	 */
	public int getPropertyInteger(String key) {
		if (pro.getProperty(key) == null)
			throw new IllegalArgumentException("Missing property: " + key);
		return Integer.parseInt(pro.getProperty(key));
	}
	
	/**
	 * Parse the value of given property key into long.
	 *
	 * @param key Given property key.
	 * @return Corresponding long value.
	 */
	public long getPropertyLong(String key) {
		if (pro.getProperty(key) == null)
			throw new IllegalArgumentException("Missing property: " + key);
		return Long.parseLong(pro.getProperty(key));
	}
	
	/**
	 * Parse the value of given property key into double.
	 *
	 * @param key Given property key.
	 * @return Corresponding double value.
	 */
	public double getPropertyDouble(String key) {
		if (pro.getProperty(key) == null)
			throw new IllegalArgumentException("Missing property: " + key);
		return Double.parseDouble(pro.getProperty(key));
	}
	
	/**
	 * Parse the value of given property key into integer.
	 *
	 * @param key Given property key.
	 * @return Corresponding integer value.
	 */
	public boolean getPropertyBoolean(String key) {
		String s = pro.getProperty(key);
		if (s == null)
			throw new IllegalArgumentException("Missing property: " + key);
		if (s.equals("true"))
			return true;
		else if (s.equals("false"))
			return false;
		throw new NumberFormatException("Invalid boolean value: " + s);
	}
	
	/**
	 * Read and parse properties from input stream and command line. Property lists are different for different applications.
	 *
	 * @param input Input file stream. File format: *.properties.
	 * @param args  Input argument list from command line.
	 */
	abstract void parseProperties(InputStream input, String[] args);
	
	/**
	 * Put the current property <tt>(key,value)</tt> into the <tt>Hashtable</tt>
	 *
	 * @param key   Input key.
	 * @param value Input value.
	 */
	public void setProperty(String key, String value) {
		pro.setProperty(key, value);
	}
	
	/**
	 * Put the current property <tt>(key,value)</tt> into the <tt>Hashtable</tt> if no <tt>key</tt> exists previously.
	 *
	 * @param key   Input key.
	 * @param value Input value.
	 */
	public void setPropertyIfAbsence(String key, String value) {
		if (pro.getProperty(key) == null)
			setProperty(key, value);
	}
	
	/**
	 * Output the properties into a file that is loadable through <tt>loadPropertiesFromResourceFile()</tt> method.
	 *
	 * @param writer   Output file writer.
	 * @param comments Comments used as the first line of the output file. Pass <tt>null</tt> if not needed.
	 */
	public void storeProperties(Writer writer, String comments) {
		try {
			pro.store(writer, comments);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean contains(String name) {
		return pro.containsKey(name);
	}
	
	/**
	 * Read property file from resource folder, call parseProperties() later to parse the loaded properties.
	 *
	 * @param filePath Path of the ".properties" file.
	 */
	public void loadPropertiesFromResourceFile(String filePath, String[] args) {
		InputStream in = getClass().getClassLoader().getResourceAsStream(filePath);
		parseProperties(in, args);
	}
}