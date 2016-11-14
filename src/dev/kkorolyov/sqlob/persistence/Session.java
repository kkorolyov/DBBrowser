package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import javax.sql.DataSource;

import dev.kkorolyov.sqlob.annotation.Column;
import dev.kkorolyov.sqlob.annotation.Table;
import dev.kkorolyov.sqlob.annotation.Transient;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.logging.LoggerInterface;

/**
 * Persists objects using an external SQL database.
 * Allows for retrieval of objects by ID or by an arbitrary set of conditions.
 */
public class Session implements AutoCloseable {
	private static final LoggerInterface log = Logger.getLogger(Session.class.getName());
	private static final boolean LOGGING_ENABLED = !(log instanceof dev.kkorolyov.sqlob.logging.LoggerStub);
	private static final String ID_NAME = "uuid",
															ID_TYPE = "CHAR(36)";
	
	private static final Map<Class<?>, String> tables = new HashMap<>();	// Caches shared by all Sessions
	private static final Map<String, String> tableInits = new HashMap<>();
	private static final Map<Class<?>, LinkedList<SqlField>> sqlFields = new HashMap<>();
	
	private final DataSource ds;
	private SessionWorker worker;
	private final int bufferSize;
	private int bufferCounter = 0;

	/**
	 * Constructs a new session with a default buffer size of {@code 100}.
	 * @see #Session(DataSource, int)
	 */
	public Session (DataSource ds) {
		this(ds, 100);
	}
	/**
	 * Constructs a new session with a {@code Datasource}.
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
	
	/**
	 * Equivalent to {@link #flush()}.
	 */
	@Override
	public void close() throws SQLException {
		flush();
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
			Iterable<SqlField> sqlFields = getPersistentFields(o.getClass());
			Condition equals = buildEqualsCondition(o, sqlFields);
			Map<UUID, ?> map = getMap(o.getClass(), equals);
			
			if (!map.isEmpty()) {	// Equivalent object already saved
				UUID uuid = map.keySet().iterator().next();
				
				if (LOGGING_ENABLED)
					log.debug("Found equivalent instance of (" + o.getClass().getName() + ") " + o + " at " + ID_NAME + ": " + uuid);
				return uuid;
			}
			try (PreparedStatement s = conn.prepareStatement(buildPut(getTable(o.getClass()), sqlFields))) {
				int counter = 1;
				s.setString(counter++, UUID.randomUUID().toString());	// Generate new UUID
				
				for (SqlField sqlField : sqlFields) {
					try {
						Object value = sqlField.field.get(o);
						s.setObject(counter++, (sqlField.isReference ? put(value).toString() : value), sqlField.type);
					} catch (IllegalAccessException e) {
						throw new NonPersistableException(sqlField.field.getName() + " is innaccessible");
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
		
		private <T> Map<UUID, T> getMap(Class<T> c, Condition condition) throws SQLException {	// Return UUID mapped to object
			Map<UUID, T> results = new HashMap<>();
			String table = getTable(c);	// Get appropriate table, create if needed
			
			try (PreparedStatement s = conn.prepareStatement(buildGet(table, condition))) {
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
						
						for (SqlField sqlField : getPersistentFields(c)) {
							try {
								Object value = rs.getObject(sqlField.name);
								if (value != null && sqlField.isReference)	// Reference
									value = get(sqlField.field.getType(), UUID.fromString((String) value));
									
								sqlField.field.set(result, value);
							} catch (IllegalAccessException e) {
								throw new NonPersistableException(sqlField.field.getName() + " is innaccessible");
							}
						}
						results.put(uuid, result);
					} catch (InstantiationException | IllegalAccessException e) {
						throw new NonPersistableException(c.getName() + " does not provide an accessible nullary constructor");
					}
				}
			}
			return results;
		}
		
		private String getTable(Class<?> c) throws SQLException {	// TODO un-ickify
			String 	table = null,
							tableInit = null;
			Iterable<SqlField> sqlFields = getPersistentFields(c);
			
			if ((table = tables.get(c)) == null) {	// Get table name from cache
				Table override = c.getAnnotation(Table.class);
				table = (override == null ? c.getSimpleName() : override.value());	// Apply name overridden with Table annotation
				tables.put(c, table);
				
				if (LOGGING_ENABLED)
					log.debug("Added to tables: " + c + ", " + table);
			}
			if ((tableInit = tableInits.get(table)) == null) {	// Get init statement from cache
				tableInit = buildDefaultInit(table, sqlFields);
				tableInits.put(table, tableInit);
				
				if (LOGGING_ENABLED)
					log.debug("Added to tableInits: " + table + ", " + tableInit);
			}
			for (SqlField sqlField : sqlFields) {
				if (sqlField.isReference)
					getTable(sqlField.field.getType());
			}
			conn.createStatement().executeUpdate(tableInit);	// Add table init to transaction
			
			return table;
		}
		private String buildDefaultInit(String table, Iterable<SqlField> sqlFields) throws SQLException {
			StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(table).append(" (").append(ID_NAME).append(" ").append(ID_TYPE).append(" PRIMARY KEY");	// Use type 4 UUID
			
			for (SqlField sqlField : sqlFields)
				builder.append(", ").append(sqlField.name).append(" ").append(sqlField.isReference ? buildReference(sqlField) : sqlField.typeName);
			
			builder.append(")");
			
			String result = builder.toString();
			
			if (LOGGING_ENABLED)
				log.debug("Built default table init statement for " + table + ": " + result);
			return result;
		}
		private String buildReference(SqlField sqlField) throws SQLException {
			return sqlField.typeName + ", FOREIGN KEY (" + sqlField.name + ") REFERENCES " + getTable(sqlField.field.getType()) + " (" + ID_NAME + ")";
		}
		
		private String buildGet(String table, Condition condition) {
			String result = "SELECT * FROM " + table + (condition == null ? "" : " WHERE " + condition);
			
			if (LOGGING_ENABLED)
				log.debug("Built GET for " + table + ": " + result);
			return result;
		}
		private String buildPut(String table, Iterable<SqlField> sqlFields) {
			StringBuilder builder = new StringBuilder("INSERT INTO ").append(table).append("(").append(ID_NAME).append(","),
										values = new StringBuilder("VALUES (?,");
			
			for (SqlField sqlField : sqlFields)  {
				builder.append(sqlField.name).append(",");
				values.append("?,");
			}
			values.replace(values.length() - 1, values.length(), ")");
			builder.replace(builder.length() - 1, builder.length(), ") ").append(values.toString());
			
			String result = builder.toString();
			
			if (LOGGING_ENABLED)
				log.debug("Built PUT for " + table + ": " + result);
			return result;
		}
		
		private Condition buildEqualsCondition(Object o, Iterable<SqlField> sqlFields) throws SQLException {
			Condition cond = null;
			for (SqlField sqlField : sqlFields) {
				try {
					Object value = sqlField.field.get(o);
					if (value != null && sqlField.isReference)	// Reference
						value = put(value).toString();
					
					String 	attribute = sqlField.name,
									operator = (value == null ? "IS" : "=");
					
					if (cond == null)
						cond = new Condition(attribute, operator, value);
					else
						cond.and(attribute, operator, value);
				} catch (IllegalAccessException e) {
					throw new NonPersistableException(sqlField.field.getName() + " is innaccessible");
				}
			}
			if (LOGGING_ENABLED)
				log.debug("Built equals condition for " + o + ": " + cond);
			return cond;
		}
		
		private Iterable<SqlField> getPersistentFields(Class<?> c) {
			LinkedList<SqlField> fields = null;
			if ((fields = sqlFields.get(c)) == null) {
				fields = new LinkedList<>();
				
				for (Field field : c.getDeclaredFields()) {
					if (field.getAnnotation(Transient.class) == null) {	// Not transient
						field.setAccessible(true);	// Allow access
						Column column = field.getAnnotation(Column.class);
						
						fields.add(new SqlField(field, column));
					}
				}
				sqlFields.put(c, fields);
				
				if (LOGGING_ENABLED)
					log.debug("Added to sqlFields: " + c + ", " + fields);
			}
			return fields;
		}
	}
	
	private static class SqlField {
		final Field field;
		final String 	name,
									typeName;
		final int type;
		final boolean isReference;
		
		SqlField(Field field, Column column) {
			this.field = field;
			this.name = (column == null ? field.getName() : column.value());
			this.typeName = (getSql(field.getType()) == null ? ID_TYPE : getSql(field.getType()));
			this.type = getSqlType(typeName).getVendorTypeNumber();
			this.isReference = typeName.equals(ID_TYPE);
		}
		
		private static String getSql(Class<?> c) {
			if (c == Short.class || c == Short.TYPE)
				return "SMALLINT";
			else if (c == Integer.class || c == Integer.TYPE)
				return "INTEGER";
			else if (c == Long.class || c == Long.TYPE)
				return "BIGINT";
			
			else if (c == Float.class || c == Float.TYPE)
				return "DOUBLE";
			else if (c == Double.class || c == Double.TYPE)
				return "DOUBLE";
			else if (c == BigDecimal.class)
				return "DECIMAL";
			
			else if (c == Boolean.class || c == Boolean.TYPE)
				return "INTEGER";
			else if (c == Character.class || c == Character.TYPE)
				return "CHAR(1)";
			else if (c == String.class)
				return "VARCHAR(1024)";
			else
				return null;
		}
		private static JDBCType getSqlType(String typeName) {
			String type = typeName;
			int sizeIndex = type.lastIndexOf('(');
			
			if (sizeIndex >= 0)
				type = type.substring(0, sizeIndex);
			
			return JDBCType.valueOf(type);
		}
	}
}
