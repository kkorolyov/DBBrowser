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
															ID_TYPE = "CHAR(32)";
	
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
		Set<T> matches = get(c, new Condition(ID_NAME, "=", uuid.toString()));
		
		if (LOGGING_ENABLED)
			log.debug((matches.isEmpty() ? "Failed to find " : "Found ") + c.getName() + " with " + ID_NAME + ": " + uuid);
		return matches.isEmpty() ? null : matches.iterator().next();
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
		Set<T> results = new HashSet<>(getMap(c, condition).values());	// Discard IDs and duplicates
		
		if (LOGGING_ENABLED)
			log.debug("Found " + results.size() + " results for " + c.getName() + " matching condition: " + condition);
		return results;
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
		Iterable<Field> fields = getPersistentFields(o.getClass());
		Condition equals = buildEqualsCondition(o, fields);
		Map<UUID, ?> map = getMap(o.getClass(), equals);
		
		if (!map.isEmpty()) {	// Equivalent object already saved
			UUID uuid = map.keySet().iterator().next();
			
			if (LOGGING_ENABLED)
				log.debug("Found equivalent instance of (" + o.getClass().getName() + ") " + o + " at " + ID_NAME + ": " + uuid);
			return uuid;
		}
		try (Connection conn = getConn()) {
			try (PreparedStatement s = conn.prepareStatement(buildPut(getTable(o.getClass()), fields))) {
				int counter = 1;
				s.setString(counter++, UUID.randomUUID().toString());	// Generate new UUID
				
				for (Field field : fields) {
					try {
						Object value = field.get(o);
						if (value != null) {
							if (field.getAnnotation(Reference.class) == null)	// Primitive
								s.setObject(counter++, value);
							else	// Reference
								s.setString(counter++, put(value).toString());
						}
					} catch (IllegalAccessException e) {
						throw new NonPersistableException(field.getName() + " is innaccessible");
					}
				}
				s.executeUpdate();
				conn.commit();
			} catch (SQLException e) {
				conn.rollback();
				throw e;
			}
		}
		UUID uuid = getMap(o.getClass(), equals).keySet().iterator().next();
		
		if (LOGGING_ENABLED)
			log.debug("Saved (" + o.getClass().getName() + ") " + o + " at " + ID_NAME + ": " + uuid);
		return uuid;
	}
	
	private <T> Map<UUID, T> getMap(Class<T> c, Condition condition) throws SQLException {	// Return UUID mapped to object
		Map<UUID, T> results = new HashMap<>();
		String table = getTable(c);	// Get appropriate table, create if needed
		
		try (Connection conn = getConn()) {
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
						
						for (Field field : getPersistentFields(c)) {
							try {
								Object value = rs.getObject(field.getName());
								if (value != null && field.getAnnotation(Reference.class) != null)	// Reference
									value = get(field.getType(), UUID.fromString(rs.getString(field.getName())));
									
								field.set(result, value);
							} catch (IllegalAccessException e) {
								throw new NonPersistableException(field.getName() + " is innaccessible");
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
		}
		return results;
	}
	
	private String getTable(Class<?> c) throws SQLException {
		Sql override = c.getAnnotation(Sql.class);
		String table = override != null ? override.value() : c.getSimpleName();
			
		if (!tableExists(table))
			initTable(buildDefaultInit(table, getPersistentFields(c)));
		
		return table;
	}
	private boolean tableExists(String table) throws SQLException {
		try (Connection conn = getConn()) {
			try (ResultSet rs = conn.getMetaData().getTables(null, null, table, null)) {
				return rs.next();	// Table exists
			} catch (SQLException e) {
				conn.rollback();
				throw e;
			}
		}
	}
	private void initTable(String init) throws SQLException {
		try (Connection conn = getConn()) {
			try (Statement s = conn.createStatement()) {
				s.executeUpdate(init);
				conn.commit();
			} catch (SQLException e) {
				conn.rollback();
				throw e;
			}
		}
	}
	
	private String buildDefaultInit(String table, Iterable<Field> fields) throws SQLException {
		StringBuilder builder = new StringBuilder("CREATE TABLE ").append(table).append(" (").append(ID_NAME).append(" ").append(ID_TYPE).append(" PRIMARY KEY");	// Use type 4 UUID
		for (Field field : fields) {
			Reference reference = field.getAnnotation(Reference.class);
			Sql sql = field.getAnnotation(Sql.class);
			
			builder.append(", ").append(field.getName()).append(" ");
			builder.append(reference != null ? buildReference(field.getType()) : sql.value());
		}
		builder.append(")");
		
		String result = builder.toString();
		
		if (LOGGING_ENABLED)
			log.debug("Built default table init statement for " + table + ": " + result);
		return result;
	}
	private String buildReference(Class<?> c) throws SQLException {
		return ID_TYPE + " REFERENCES " + getTable(c) + " (" + ID_NAME + ")";
	}
	
	private static String buildGet(String table, Condition condition) {
		StringBuilder builder = new StringBuilder("SELECT * FROM ");
		builder.append(table);
		
		if (condition != null)
			builder.append(" WHERE ").append(condition);
		
		String result = builder.toString();
		
		if (LOGGING_ENABLED)
			log.debug("Built GET for " + table + ": " + result);
		return result;
	}
	private static String buildPut(String table, Iterable<Field> fields) {
		StringBuilder builder = new StringBuilder("INSERT INTO ").append(table).append("(").append(ID_NAME).append(","),
									values = new StringBuilder("VALUES (?,");
		
		for (Field field : fields)  {
			builder.append(field.getName()).append(",");
			values.append("?,");
		}
		values.replace(values.length() - 1, values.length(), ")");
		builder.replace(builder.length() - 1, builder.length(), ") ").append(values.toString());
		
		String result = builder.toString();
		
		if (LOGGING_ENABLED)
			log.debug("Built PUT for " + table + ": " + result);
		return result;
	}
	
	private Condition buildEqualsCondition(Object o, Iterable<Field> fields) throws SQLException {
		Condition cond = null;
		for (Field field : fields) {
			try {
				Object value = field.get(o);
				if (value != null && field.getAnnotation(Reference.class) != null)	// Reference
					value = put(value).toString();
				
				String 	attribute = field.getName(),
								operator = (value == null ? "IS" : "=");
				
				if (cond == null)
					cond = new Condition(attribute, operator, value);
				else
					cond.and(attribute, operator, value);
			} catch (IllegalAccessException e) {
				throw new NonPersistableException(field.getName() + " is innaccessible");
			}
		}
		if (LOGGING_ENABLED)
			log.debug("Built equals condition for " + o + ": " + cond);
		return cond;
	}
	
	private static Iterable<Field> getPersistentFields(Class<?> c) {
		List<Field> fields = new ArrayList<>();
		
		for (Field field : c.getDeclaredFields()) {
			Transient transientAnnotation = field.getAnnotation(Transient.class);
			Reference reference = field.getAnnotation(Reference.class);
			Sql sql = field.getAnnotation(Sql.class);
			
			if (transientAnnotation == null && (reference != null || sql != null))	// Not transient
				fields.add(field);
		}
		return fields;
	}
	
	private Connection getConn() throws SQLException {
		Connection conn = ds.getConnection();
		conn.setAutoCommit(false);
		
		return conn;
	}
}
