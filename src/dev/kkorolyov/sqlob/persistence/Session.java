package dev.kkorolyov.sqlob.persistence;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import javax.sql.DataSource;

import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.logging.LoggerInterface;

/**
 * Persists objects using an external SQL database.
 * Allows for retrieval of objects by ID or by an arbitrary set of conditions.
 */
public class Session implements AutoCloseable {
	private static final LoggerInterface log = Logger.getLogger(Session.class.getName());
	private static final boolean LOGGING_ENABLED = !(log instanceof dev.kkorolyov.sqlob.logging.LoggerStub);
	static final String ID_NAME = "uuid",
															ID_TYPE = "CHAR(36)";
	
	private final DataSource ds;
	private final int bufferSize;
	private int bufferCounter = 0;
	private Map<Class<?>, String> typeMap = getDefaultTypeMap();
	private Map<Class<?>, SqlobClass> classes = new HashMap<>();
	private SessionWorker worker;
	
	/**
	 * Constructs a new session with a default buffer size of {@code 100}.
	 * @see #Session(DataSource, int)
	 */
	public Session (DataSource ds) {
		this(ds, 100);
	}
	/**
	 * Constructs a new session.
	 * @param ds datasource to SQL database
	 * @param bufferSize number of requests to cache before committing to database
	 */
	public Session(DataSource ds, int bufferSize) {
		this.ds = ds;
		this.bufferSize = bufferSize;
	}
	
	/**
	 * Retrieves the instance of a class matching an UUID.
	 * @param c type to retrieve
	 * @param uuid instance uuid
	 * @return instance matching {@code id}, or {@code null} if no such instance
	 * @throws SQLException if a database error occurs
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public <T> T get(Class<T> c, UUID uuid) throws SQLException {
		try {
			T result = getWorker().get(c, uuid);
			bufferCounter++;
			
			if (bufferSize == 0)
				flush();
			
			if (LOGGING_ENABLED)
				log.debug((result == null ? "Failed to find " : "Found ") + c.getName() + " with " + ID_NAME + ": " + uuid);
			
			return result;
		} catch (SQLException e) {
			getWorker().rollback();
			throw e;
		}
	}
	/**
	 * Retrieves all instances of a class matching a condition.
	 * @param c type to retrieve
	 * @param condition condition to match
	 * @return all matching instances
	 * @throws SQLException if a database error occurs
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public <T> Set<T> get(Class<T> c, Condition condition) throws SQLException {
		try {
			Set<T> results = getWorker().get(c, condition);
			bufferCounter++;
			
			if (bufferSize == 0)
				flush();
			
			if (LOGGING_ENABLED)
				log.debug("Found " + results.size() + " results for " + c.getName() + " matching condition: " + condition);
			return results;
		} catch (SQLException e) {
			getWorker().rollback();
			throw e;
		}
	}
	
	/**
	 * Stores an instance of a class and returns its UUID.
	 * If an equivalent instance is already stored, no additional storage is performed and the UUID of that instance is returned.
	 * @param o instance to store
	 * @return UUID of stored instance
	 * @throws SQLException if a database error occurs
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public UUID put(Object o) throws SQLException {
		try {
			UUID result = getWorker().put(o);
			bufferCounter++;
			
			if (bufferSize == 0)
				flush();
			
			return result;
		} catch (SQLException e) {
			worker.rollback();
			throw e;
		}
	}
	
	/**
	 * Commits all active requests and closes any database connections this session is holding.
	 * @throws SQLException if a database error occurs
	 */
	public void flush() throws SQLException {
		if (worker != null) {
			worker.close();
			worker = null;
			
			bufferCounter = 0;
		}
	}
	
	/** @return type map used by this session for mapping Java classes to SQL types */
	public Map<Class<?>, String> getTypeMap() {
		return new HashMap<>(typeMap);
	}
	Map<Class<?>, String> getRawTypeMap() {	// For overhead reduction
		return typeMap;
	}
	/** @param typeMap new Java-to-SQL type map, if {@code null}, resets to default type map */
	public void setTypeMap(Map<Class<?>, String> typeMap) {
		this.typeMap = (typeMap == null ? getDefaultTypeMap() : typeMap);
	}
	private static Map<Class<?>, String> getDefaultTypeMap() {
		Map<Class<?>, String> map = new HashMap<>();
		
		String shortSql = "SMALLINT";
		map.put(Short.class, shortSql);
		map.put(Short.TYPE, shortSql);
		
		String intSql = "INTEGER";
		map.put(Integer.class, intSql);
		map.put(Integer.TYPE, intSql);
		
		String longSql = "BIGINT";
		map.put(Long.class, longSql);
		map.put(Long.TYPE, longSql);
		
		String doubleSql = "DOUBLE PRECISION";
		map.put(Float.class, doubleSql);
		map.put(Float.TYPE, doubleSql);
		map.put(Double.class, doubleSql);
		map.put(Double.TYPE, doubleSql);
		
		String bigDecimalSql = "DECIMAL";
		map.put(BigDecimal.class, bigDecimalSql);
		
		String booleanSql = "BIT";
		map.put(Boolean.class, booleanSql);
		map.put(Boolean.TYPE, booleanSql);
		
		String charSql = "CHAR(1)";
		map.put(Character.class, charSql);
		map.put(Character.TYPE, charSql);
		
		String stringSql = "VARCHAR(1024)";
		map.put(String.class, stringSql);
		
		return map;
	}
	
	/**
	 * Equivalent to {@link #flush()}.
	 */
	@Override
	public void close() throws SQLException {
		flush();
	}
	
	SqlobClass getSqlobClass(Class<?> c) throws SQLException {
		SqlobClass sc = null;
		
		if ((sc = classes.get(c)) == null) {
			sc = new SqlobClass(c, this);
			classes.put(c, sc);
			
			getWorker().initTable(sc);	// Initialize when caching
			
			if (LOGGING_ENABLED)
				log.info("Cached new " + sc);
		}
		return sc;
	}
	
	private SessionWorker getWorker() throws SQLException {
		if (worker != null && (bufferCounter >= bufferSize))
			flush();
		
		if (worker == null)
			worker = new SessionWorker(getConn());
		
		return worker;
	}
	
	private Connection getConn() throws SQLException {
		Connection conn = ds.getConnection();
		conn.setAutoCommit(false);	// Disable to reduce overhead
		
		if (LOGGING_ENABLED)
			log.debug("Retrieved new Connection from " + ds);
		return conn;
	}
	
	@SuppressWarnings("synthetic-access")
	private class SessionWorker {
		private final Connection conn;
		
		SessionWorker(Connection conn) {
			this.conn = conn;
		}
		
		<T> T get(Class<T> c, UUID uuid) throws SQLException {
			Set<T> matches = get(c, new Condition(ID_NAME, "=", uuid.toString()));
			return matches.isEmpty() ? null : matches.iterator().next();
		}
		<T> Set<T> get(Class<T> c, Condition condition) throws SQLException {
			return new HashSet<>(getMap(c, condition).values());	// Discard IDs and duplicates
		}
		
		UUID put(Object o) throws SQLException {
			SqlobClass sc = getSqlobClass(o.getClass());
			Condition equals = buildEquals(o);
			
			if (equals != null) { 
				Map<UUID, ?> map = getMap(o.getClass(), equals);
				
				if (!map.isEmpty()) {	// Equivalent object already saved
					UUID uuid = map.keySet().iterator().next();
					
					if (LOGGING_ENABLED)
						log.debug("Found equivalent instance of (" + sc + ") " + o + " at " + ID_NAME + ": " + uuid);
					return uuid;
				}
			}
			try (PreparedStatement s = conn.prepareStatement(sc.getPut())) {
				int counter = 1;
				s.setString(counter++, UUID.randomUUID().toString());	// Generate new UUID
				
				for (SqlobField sqlField : sc.getFields()) {
					try {
						Object value = sqlField.getField().get(o);
						s.setObject(counter++, (sqlField.isReference() ? put(value).toString() : value), sqlField.getTypeCode());
					} catch (IllegalAccessException e) {
						throw new NonPersistableException(sqlField.getField().getName() + " is innaccessible");
					}
				}
				s.executeUpdate();
			}
			UUID uuid = getMap(o.getClass(), equals).keySet().iterator().next();
			
			if (LOGGING_ENABLED)
				log.debug("Saved (" + o.getClass().getName() + ") " + o + " at " + ID_NAME + ": " + uuid);
			return uuid;
		}
		
		void close() throws SQLException {	// Commit and close worker
			conn.commit();
			conn.close();
		}
		void rollback() throws SQLException {
			conn.rollback();
			conn.close();
		}
		
		UUID getId(Object o) throws SQLException {
			UUID result = null;
			Condition equals = buildEquals(o);
			
			if (equals != null) {
				Map<UUID, ?> map = getMap(o.getClass(), equals);
				
				if (!map.isEmpty())
					result = map.entrySet().iterator().next().getKey();
			}
			return result;
		}
		
		private <T> Map<UUID, T> getMap(Class<T> c, Condition condition) throws SQLException {	// Return UUID mapped to object
			SqlobClass sc = getSqlobClass(c);
			try (PreparedStatement s = conn.prepareStatement(sc.getGet(condition))) {
				Map<UUID, T> results = new HashMap<>();

				if (condition != null) {
					int counter = 1;
					for (Object value : condition.values())
						s.setObject(counter++, value);
				}
				ResultSet rs = s.executeQuery();
				while (rs.next()) {	// Found result
					try {
						UUID uuid = UUID.fromString(rs.getString(ID_NAME));
						T result = c.newInstance();
						
						for (SqlobField sqlField : sc.getFields()) {
							try {
								Object value = rs.getObject(sqlField.getName());
								if (value != null && sqlField.isReference())	// Reference
									value = get(sqlField.getReferencedClass().getType(), UUID.fromString((String) value));
									
								sqlField.getField().set(result, value);
							} catch (IllegalAccessException e) {
								throw new NonPersistableException(sqlField.getField().getName() + " is innaccessible");
							}
						}
						results.put(uuid, result);
					} catch (InstantiationException | IllegalAccessException e) {
						throw new NonPersistableException(c.getName() + " does not provide an accessible nullary constructor");
					}
				}
				return results;
			}
		}
		
		private Condition buildEquals(Object o) throws SQLException {
			Condition result = null;
			
			for (SqlobField field : getSqlobClass(o.getClass()).getFields()) {
				try {
					Object value = field.getField().get(o);
					if (value != null && field.isReference()) {	// Reference
						UUID valueId = getId(value);
						if (valueId == null) {	// No such referenced object -> no such this object
							result = null;
							break;
						} else
							value = valueId.toString();
					}
					String 	attribute = field.getName(),
									operator = (value == null ? "IS" : "=");
			
					if (result == null)
						result = new Condition(attribute, operator, value);
					else
						result.and(attribute, operator, value);
				} catch (IllegalAccessException e) {
					throw new NonPersistableException(field.getField().getName() + " is inaccessible");
				}
			}
			return result;
		}
		
		void initTable(SqlobClass sqlobClass) throws SQLException {
			Statement s = conn.createStatement();
			for (String init : sqlobClass.getInits())
				s.executeUpdate(init);
		}
	}
}
