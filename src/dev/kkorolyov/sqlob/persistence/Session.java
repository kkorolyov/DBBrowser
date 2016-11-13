package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

import javax.sql.DataSource;

import dev.kkorolyov.sqlob.annotation.Reference;
import dev.kkorolyov.sqlob.annotation.Sql;
import dev.kkorolyov.sqlob.annotation.Transient;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.logging.LoggerInterface;

/**
 * Persists objects using an external SQL database.
 * Allows for retrieval of objects by ID or by an arbitrary set of conditions.
 */
public class Session {
	private static final LoggerInterface log = Logger.getLogger(Session.class.getName());
	private static final boolean LOGGING_ENABLED = !(log instanceof dev.kkorolyov.sqlob.logging.LoggerStub);
	private static final String ID_NAME = "uuid",
															ID_TYPE = "CHAR(36)";
	
	private static final Map<Class<?>, String> tables = new HashMap<>();	// Caches shared by all Sessions
	private static final Map<String, String> tableInits = new HashMap<>();
	private static final Map<Class<?>, LinkedList<SqlField>> sqlFields = new HashMap<>();
	
	private final DataSource ds;

	/**
	 * Constructs a new connection to a {@code Datasource}.
	 * @param ds datasource to SQL database
	 */
	public Session(DataSource ds) {
		this.ds = ds;
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
		try (Connection conn = getConn()) {
			T result = get(c, uuid, conn);
			conn.commit();
			
			if (LOGGING_ENABLED)
				log.debug((result == null ? "Failed to find " : "Found ") + c.getName() + " with " + ID_NAME + ": " + uuid);
			
			return result;
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
		try (Connection conn = getConn()) {
			Set<T> results = get(c, condition, conn);
			conn.commit();
			
			if (LOGGING_ENABLED)
				log.debug("Found " + results.size() + " results for " + c.getName() + " matching condition: " + condition);
			return results;
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
		try (Connection conn = getConn()) {
			UUID result = put(o, conn);
			conn.commit();
			
			return result;
		}
	}
	
	private static <T> T get(Class<T> c, UUID uuid, Connection conn) throws SQLException {
		Set<T> matches = get(c, new Condition(ID_NAME, "=", uuid.toString()), conn);
		return matches.isEmpty() ? null : matches.iterator().next();
	}
	private static <T> Set<T> get(Class<T> c, Condition condition, Connection conn) throws SQLException {
		return new HashSet<>(getMap(c, condition, conn).values());	// Discard IDs and duplicates
	}
	
	private static UUID put(Object o, Connection conn) throws SQLException {
		Iterable<SqlField> sqlFields = getPersistentFields(o.getClass());
		Condition equals = buildEqualsCondition(o, sqlFields, conn);
		Map<UUID, ?> map = getMap(o.getClass(), equals, conn);
		
		if (!map.isEmpty()) {	// Equivalent object already saved
			UUID uuid = map.keySet().iterator().next();
			
			if (LOGGING_ENABLED)
				log.debug("Found equivalent instance of (" + o.getClass().getName() + ") " + o + " at " + ID_NAME + ": " + uuid);
			return uuid;
		}
		try (PreparedStatement s = conn.prepareStatement(buildPut(getTable(o.getClass(), conn), sqlFields))) {
			int counter = 1;
			s.setString(counter++, UUID.randomUUID().toString());	// Generate new UUID
			
			for (SqlField sqlField : sqlFields) {
				try {
					Object value = sqlField.field.get(o);
					if (value != null) {
						if (sqlField.isReference)	// Reference
							s.setString(counter++, put(value, conn).toString());
						else	// Primitive	
							s.setObject(counter++, value);
					}
				} catch (IllegalAccessException e) {
					throw new NonPersistableException(sqlField.field.getName() + " is innaccessible");
				}
			}
			s.executeUpdate();
			conn.commit();
		} catch (SQLException e) {
			conn.rollback();
			throw e;
		}
		UUID uuid = getMap(o.getClass(), equals, conn).keySet().iterator().next();
		
		if (LOGGING_ENABLED)
			log.debug("Saved (" + o.getClass().getName() + ") " + o + " at " + ID_NAME + ": " + uuid);
		return uuid;
	}
	
	private static <T> Map<UUID, T> getMap(Class<T> c, Condition condition, Connection conn) throws SQLException {	// Return UUID mapped to object
		Map<UUID, T> results = new HashMap<>();
		String table = getTable(c, conn);	// Get appropriate table, create if needed
		
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
							Object value = rs.getObject(sqlField.field.getName());
							if (value != null && sqlField.isReference)	// Reference
								value = get(sqlField.field.getType(), UUID.fromString(rs.getString(sqlField.field.getName())), conn);
								
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
		} catch (SQLException e) {
			throw e;
		} finally {
			conn.rollback();
		}
		return results;
	}
	
	private static String getTable(Class<?> c, Connection conn) throws SQLException {
		String 	table = null,
						tableInit = null;
		if ((table = tables.get(c)) == null) {	// Get table name from cache
			Sql override = c.getAnnotation(Sql.class);
			table = (override == null ? c.getSimpleName() : override.value());
			tables.put(c, table);
			
			if (LOGGING_ENABLED)
				log.debug("Added to tables: " + c + ", " + table);
		}
		if ((tableInit = tableInits.get(table)) == null) {	// Get init statement from cache
			tableInit = buildDefaultInit(table, getPersistentFields(c), conn);
			tableInits.put(table, tableInit);
			
			if (LOGGING_ENABLED)
				log.debug("Added to tableInits: " + table + ", " + tableInit);
		}
		conn.createStatement().executeUpdate(tableInit);	// Add table init to transaction
		conn.commit();
		
		return table;
	}
	private static String buildDefaultInit(String table, Iterable<SqlField> sqlFields, Connection conn) throws SQLException {
		StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(table).append(" (").append(ID_NAME).append(" ").append(ID_TYPE).append(" PRIMARY KEY");	// Use type 4 UUID
		
		for (SqlField sqlField : sqlFields)
			builder.append(", ").append(sqlField.field.getName()).append(" ").append(sqlField.isReference ? buildReference(sqlField.field, conn) : sqlField.sql.value());
		
		builder.append(")");
		
		String result = builder.toString();
		
		if (LOGGING_ENABLED)
			log.debug("Built default table init statement for " + table + ": " + result);
		return result;
	}
	private static String buildReference(Field f, Connection conn) throws SQLException {
		return ID_TYPE + ", FOREIGN KEY (" + f.getName() + ") REFERENCES " + getTable(f.getType(), conn) + " (" + ID_NAME + ")";
	}
	
	private static String buildGet(String table, Condition condition) {
		String result = "SELECT * FROM " + table + (condition == null ? "" : " WHERE " + condition);
		
		if (LOGGING_ENABLED)
			log.debug("Built GET for " + table + ": " + result);
		return result;
	}
	private static String buildPut(String table, Iterable<SqlField> sqlFields) {
		StringBuilder builder = new StringBuilder("INSERT INTO ").append(table).append("(").append(ID_NAME).append(","),
									values = new StringBuilder("VALUES (?,");
		
		for (SqlField sqlField : sqlFields)  {
			builder.append(sqlField.field.getName()).append(",");
			values.append("?,");
		}
		values.replace(values.length() - 1, values.length(), ")");
		builder.replace(builder.length() - 1, builder.length(), ") ").append(values.toString());
		
		String result = builder.toString();
		
		if (LOGGING_ENABLED)
			log.debug("Built PUT for " + table + ": " + result);
		return result;
	}
	
	private static Condition buildEqualsCondition(Object o, Iterable<SqlField> sqlFields, Connection conn) throws SQLException {
		Condition cond = null;
		for (SqlField sqlField : sqlFields) {
			try {
				Object value = sqlField.field.get(o);
				if (value != null && sqlField.isReference)	// Reference
					value = put(value, conn).toString();
				
				String 	attribute = sqlField.field.getName(),
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
	
	private static Iterable<SqlField> getPersistentFields(Class<?> c) {
		LinkedList<SqlField> fields = null;
		if ((fields = sqlFields.get(c)) == null) {
			fields = new LinkedList<>();
			
			for (Field field : c.getDeclaredFields()) {
				Transient transientAnnotation = field.getAnnotation(Transient.class);
				Reference reference = field.getAnnotation(Reference.class);
				Sql sql = field.getAnnotation(Sql.class);
				
				if (transientAnnotation == null && (reference != null || sql != null))	// Not transient
					fields.add(new SqlField(field, reference, sql));
			}
			sqlFields.put(c, fields);
			
			if (LOGGING_ENABLED)
				log.debug("Added to sqlFields: " + c + ", " + fields);
		}
		return fields;
	}
	
	private Connection getConn() throws SQLException {
		Connection conn = ds.getConnection();
		conn.setAutoCommit(false);	// Disable to reduce overhead
		
		if (LOGGING_ENABLED)
			log.debug("Retrieved new Connection from " + ds);
		return conn;
	}
	
	private static class SqlField {
		final Field field;
		final Sql sql;
		final boolean isReference;
		
		SqlField(Field field, Reference r, Sql s) {
			this.field = field;
			this.sql = s;
			this.isReference = r != null;
		}
	}
}
