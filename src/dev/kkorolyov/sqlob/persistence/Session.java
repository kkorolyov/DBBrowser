package dev.kkorolyov.sqlob.persistence;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

import javax.sql.DataSource;

import dev.kkorolyov.sqlob.construct.SqlobClass;
import dev.kkorolyov.sqlob.construct.SqlobClassFactory;
import dev.kkorolyov.sqlob.construct.SqlobField;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.logging.LoggerInterface;
import dev.kkorolyov.sqlob.sql.Condition;
import dev.kkorolyov.sqlob.sql.Selection;

/**
 * Persists objects using an external SQL database.
 * Allows for retrieval of objects by ID or by an arbitrary set of conditions.
 */
public class Session implements AutoCloseable {
	private static final LoggerInterface log = Logger.getLogger(Session.class.getName());
	private static final boolean LOGGING_ENABLED = !(log instanceof dev.kkorolyov.sqlob.logging.LoggerStub);
	
	private final DataSource ds;
	private final int bufferSize;
	private int bufferCounter = 0;
	private SessionWorker worker;
	private SqlGenerator sqlGenerator = new SqlGenerator();
	private SqlobClassFactory scFactory = new SqlobClassFactory(sqlGenerator.getIdType());

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
				log.debug((result == null ? "Failed to find " : "Found ") + c.getName() + " with id: " + uuid);
			
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
			UUID id = getId(o);
			if (id != null) {
				if (LOGGING_ENABLED)
					log.debug("Found equivalent instance of (" + o.getClass() + ") " + o + " at id: " + id);
				return id;
			}
			SqlobClass sc = getSqlobClass(o.getClass());
			try (PreparedStatement s = conn.prepareStatement(sqlGenerator.buildInsert(sc))) {
				int counter = 1;
				s.setString(counter++, UUID.randomUUID().toString());	// Generate new UUID
				
				for (SqlobField sqlField : sc.getFields()) {
					try {
						Object value = sqlField.getType().get(o);
						s.setObject(counter++, (sqlField.isReference() ? put(value).toString() : value), sqlField.getSqlTypeCode());
					} catch (IllegalAccessException e) {
						throw new NonPersistableException(sqlField.getType().getName() + " is innaccessible");
					}
				}
				s.executeUpdate();
			}
			UUID uuid = getId(o);
			
			if (LOGGING_ENABLED)
				log.debug("Saved (" + o.getClass().getName() + ") " + o + " at id: " + uuid);
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
			SqlobClass sc = getSqlobClass(o.getClass());
			
			Map<SqlobField, Object> values = sc.getValues(o);
			try {
				applyReferences(values);
			} catch (ReferenceNotFoundException e) {
				return null;	// Missing references = no ID
			}
			Condition equals = sqlGenerator.buildEquals(values);
			try (PreparedStatement s = conn.prepareStatement(sqlGenerator.buildSelectId(sc, equals))) {
				int counter = 1;
				for (Object value : equals.values())
					s.setObject(counter++, value);
				
				ResultSet rs = s.executeQuery();
				if (rs.next())
					result = UUID.fromString(rs.getString(1));
			}
			return result;
		}
		
		private void applyReferences(Map<SqlobField, Object> rawValues) throws SQLException, ReferenceNotFoundException {
			for (Entry<SqlobField, Object> entry : rawValues.entrySet()) {
				if (entry.getKey().isReference()) {
					Object value = entry.getValue();
					UUID id = getId(value);
					
					if (id == null)
						throw new ReferenceNotFoundException(value);
					
					entry.setValue(id.toString());
				}
			}
		}
		
		void initTable(SqlobClass sc) throws SQLException {
			try (Statement s = conn.createStatement()) {
				s.executeUpdate(sqlGenerator.buildCreate(sc));
			}
		}
	}
	
	private class SqlGenerator {
		SqlGenerator() {
			this("uuid", "CHAR(36)");
		}
		SqlGenerator(String idName, String idType) {
			this.idName = idName;
			this.idType = idType;
		}
		
		String buildCreate(SqlobClass sc) {
			StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(sc.getName());
			builder.append(" (").append(idName).append(" ").append(idType).append(" PRIMARY KEY");	// Use type 4 UUID
			
			for (SqlobField field : sc.getFields()) {
				builder.append(", ").append(field.getName()).append(" ").append(field.getSqlType());
				
				if (field.isReference())
					builder.append(", FOREIGN KEY (").append(field.getName()).append(") REFERENCES ").append(field.getReferencedClass().getName()).append(" (").append(idName).append(")");
			}
			String result = builder.append(")").toString();
			
			if (LOGGING_ENABLED)
				log.debug("Built CREATE statement for " + this + ": " + result); 
			return result;
		}
		
		String buildSelectId(SqlobClass sc, Condition condition) {
			return buildSelect(sc, new Selection(idName), condition);
		}
		String buildSelectFields(SqlobClass sc, Condition condition) {
			Selection selection = new Selection();
			
			for (SqlobField field : sc.getFields())
				selection.append(field.getName());
			
			return buildSelect(sc, selection, condition);
		}
		private String buildSelect(SqlobClass sc, Selection selection, Condition condition) {
			String result = "SELECT " + selection + " FROM " + sc.getName();
			
			if (condition != null)
				result += " WHERE " + condition;
			
			if (LOGGING_ENABLED)
				log.debug("Built GET for " + this + ": " + result);
			return result;
		}
		
		String buildInsert(SqlobClass<?> sc) {
			StringBuilder builder = new StringBuilder("INSERT INTO ").append(sc.getName()).append("(").append(idName).append(","),
										values = new StringBuilder("VALUES (?,");

			for (SqlobField sqlField : sc.getFields())  {
				builder.append(sqlField.getName()).append(",");
				values.append("?,");
			}
			builder.replace(builder.length() - 1, builder.length(), ") ");
			values.replace(values.length() - 1, values.length(), ")");
			
			builder.append(values.toString());
			
			String result = builder.toString();
			
			if (LOGGING_ENABLED)
				log.debug("Built PUT for " + this + ": " + result);
			return result;
		}
		
		Condition buildEquals(Map<SqlobField, Object> values) {
			Condition result = null;
			
			for (Entry<SqlobField, Object> entry : values.entrySet()) {
				String attribute = entry.getKey().getName();
				Object value = entry.getValue();
				Condition currentCondition = new Condition(attribute, (value == null ? "IS" : "="), value);
				
				if (result == null)
					result = currentCondition;
				else
					result.and(currentCondition);
			}
			return result;
		}
	}
}
