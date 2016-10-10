package dev.kkorolyov.sqlob.connection;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Types;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import dev.kkorolyov.simpleprops.Properties;

/**
 * Represents unique attributes of a database.
 */
public class DatabaseAttributes {
	private static final String	KEY_DRIVER = "DRIVER",
															KEY_URL = "URL";
	private static final String MARKER_HOST = "HOST",
															MARKER_DATABASE = "DATABASE";
	
	private String 	driverName,
									baseURL;
	private DatabaseTypes types;
	
	/**
	 * Returns a {@code DatabaseAttributes} matching the configuration specified by a sqlobfile.
	 * @param sqlobfilePathname path to sqlobfile
	 * @return {@code DatabaseAttributes} instance configured according to the sqlobfile at {@code sqlobFilePathname}
	 * @throws IllegalArgumentException if the file specified by {@code sqlobfilePathname} is not a valid sqlobfile
	 */
	public static DatabaseAttributes get(String sqlobfilePathname) {
		File sqlobFile = new File(sqlobfilePathname);
		if (!sqlobFile.isFile())
			throw new IllegalArgumentException("Not a file: " + sqlobfilePathname);
		
		Properties sqlobProps = new Properties(sqlobFile);
		String 	driverName = sqlobProps.get(KEY_DRIVER),
						baseUrl = sqlobProps.get(KEY_URL);
		
		if (driverName == null || baseUrl == null)
			throw new IllegalArgumentException("Incomplete sqlobfile: " + sqlobfilePathname);
		
		try {	// Init JDBC driver
			Class.forName(driverName);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Invalid driver: " + driverName);
		}
		
		return new DatabaseAttributes(sqlobFile);
	}
	
	private DatabaseAttributes() {
		// No public instantiation
	}
	@SuppressWarnings("synthetic-access")
	private DatabaseAttributes(File sqlobFile) {
		Properties props = new Properties(sqlobFile);
		
		driverName = props.remove(KEY_DRIVER);	// Remove before parsing types
		baseURL = props.remove(KEY_URL);
		
		types = new DatabaseTypes(props);
	}
	
	/** @return name of the JDBC driver class for the database represented by these attributes */
	public String getDriverName() {
		return driverName;
	}
	/**
	 * Returns a URL specific to the database represented by these attributes.
	 * @param host hostname or address of database host, may be empty
	 * @param database database name
	 * @return appropriate URL
	 */
	public String getURL(String host, String database) {
		return baseURL.replaceFirst(MARKER_HOST, host).replaceFirst(MARKER_DATABASE, database);
	}
	/** @return all types supported by the database represented by these attributes */
	public DatabaseTypes getTypes() {
		return types;
	}
	
	/**
	 * Represents all types mutually supported by both Java and a database.
	 */
	public static class DatabaseTypes implements Iterable<SqlobType> {
		private static final String CLASS_NAME_PREFIX = "java.lang.";
		
		private final Set<SqlobType> types = new HashSet<>();
		
		private DatabaseTypes() {
			// No public instantiation
		}
		private DatabaseTypes(Properties props) {
			for (String key : props.keys()) {
				try {
					types.add(new SqlobType(Class.forName(CLASS_NAME_PREFIX + key), props.get(key), getTypeCode(props.get(key).split("\\s+")[0])));
				} catch (ClassNotFoundException e) {
					throw new IllegalArgumentException("Not a valid Java class: " + key);
				}
			}
		}
		private static Integer getTypeCode(String typeName) {
			for (Field type : Types.class.getFields()) {
				if (type.getName().equals(typeName)) {
					try {
						return type.getInt(null);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new IllegalArgumentException("This should never happen");	// Should never happen
					}
				}
			}
			throw new IllegalArgumentException("Unsupported SQL type: " + typeName);
		}
		
		/**
		 * @param typeClass class to get matching type for
		 * @return {@code true} if this {@code DatabaseTypes} contains a {@code SqlobType} for the specified class
		 */
		public boolean contains(Class<?> typeClass) {
			return get(typeClass) != null;
		}
		/**
		 * @param typeCode JDBC constant to get matching type for
		 * @return {@code true} if this {@code DatabaseTypes} contains a {@code SqlobType} for the specified JDBC constant
		 */
		public boolean contains(int typeCode) {
			return get(typeCode) != null;
		}
		
		/**
		 * @param typeClass class to get matching type for
		 * @return matching {@code SqlobType}, or {@code null} if no such type
		 */
		public SqlobType get(Class<?> typeClass) {
			for (SqlobType type : types) {
				if (type.getTypeClass() == typeClass)
					return type;
			}
			return null;
		}
		/**
		 * @param typeCode JDBC constant to get matching type for
		 * @return matching {@code SqlobType}, or {@code null} if no such type
		 */
		public SqlobType get(int typeCode) {
			for (SqlobType type : types) {
				if (type.getTypeCode() == typeCode)
					return type;
			}
			return null;
		}

		@Override
		public Iterator<SqlobType> iterator() {
			return types.iterator(); 
		}
	}
}
