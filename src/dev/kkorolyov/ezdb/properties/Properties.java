package dev.kkorolyov.ezdb.properties;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import dev.kkorolyov.ezdb.logging.DebugLogger;

/**
 * Loads all necessary program properties.
 */
public class Properties {
	/** The HOST property key. */
	public static final String HOST = "HOST";
	/** The USER property key. */
	public static final String USER = "USER";
	/** The PASSWORD property key. */
	public static final String PASSWORD = "PASSWORD";
	
	/** The default HOST property value. */
	public static final String DEFAULT_HOST = "localhost";
	/** The default USER property value. */
	public static final String DEFAULT_USER = "postgres";
	/** The default PASSWORD property value. */
	public static final String DEFAULT_PASSWORD = "";
	
	private static final DebugLogger log = DebugLogger.getLogger(Properties.class.getName());
	private static final String fileName = "EZ-Props.txt";
	private static final Map<String, String> properties = new HashMap<>();
	
	/**
	 * Initializes all necessary properties in this class.
	 * <ol>
	 * <li>Default, hardcoded properties are loaded.</li>
	 * <li>The properties file is read, all current properties are updated with those read from the file.</li>
	 * </ol>
	 */
	public static void init() {
		loadDefaults();
		loadFile();
	}
	
	/**
	 * Clears all current properties and loads the hard-coded defaults.
	 */
	public static void loadDefaults() {
		properties.put(HOST, DEFAULT_HOST);
		properties.put(USER, DEFAULT_USER);
		properties.put(PASSWORD, DEFAULT_PASSWORD);
	}
	/**
	 * Loads all properties specified in the properties file
	 */
	public static void loadFile() {
		FileReader fileIn;
		try {
			fileIn = new FileReader(new File(fileName));
		} catch (FileNotFoundException e) {
			log.exceptionWarning(e);
			return;
		}
		BufferedReader fileReader = new BufferedReader(fileIn);
		
		String nextLine;
		try {
			while ((nextLine = fileReader.readLine()) != null) {
				String[] currentKeyValue = nextLine.split("=");	// Line should be "<KEY>=<VALUE>";
				String currentKey = "", currentValue = "";
				
				if (currentKeyValue.length > 0) {
					currentKey = currentKeyValue[0].trim();
					if (currentKeyValue.length > 1) {
						currentValue = currentKeyValue[1].trim();
					}
				}
				properties.put(currentKey, currentValue);
				
				log.debug("Loaded property '" + currentKey + "=" + currentValue + "' from " + fileName);
			}
		} catch (IOException e) {
			log.exceptionSevere(e);
		}
	}
	
	/**
	 * Writes all properties currently loaded in memory to the properties file.
	 */
	public static void saveToFile() {
		try (	OutputStream fileOut = new FileOutputStream(new File(fileName));
					PrintWriter filePrinter = new PrintWriter(fileOut)) {
			for (String key : properties.keySet()) {
				filePrinter.println(key + "=" + properties.get(key));
			}
		} catch (IOException e) {
			log.exceptionSevere(e);
		}
	}
	
	/**
	 * Retrieves the value of a property of the specified key.
	 * @param key key of property to retrieve
	 * @return property value
	 */
	public static String getValue(String key) {
		return properties.get(key);
	}
	
	/** @return key of every property */
	public static Set<String> getAllKeys() {
		return properties.keySet();
	}
	
	/**
	 * Adds the specified property.
	 * If the key matches an existing property's key, then that preexisting property's value is overriden by the specified value.
	 * @param key key of property to add
	 * @param value value of property to add
	 */
	public static void addProperty(String key, String value) {
		properties.put(key, value);
	}
}
