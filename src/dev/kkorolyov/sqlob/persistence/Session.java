package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
	
	private final DataSource ds;

	/**
	 * Constructs a new connection to a {@code Datasource}.
	 * @param ds datasource to SQL database
	 */
	public Session(DataSource ds) {
		this.ds = ds;
	}
	
	/**
	 * Retrieves an instance of a class matching an ID.
	 * @param c type to retrieve
	 * @param id instance id
	 * @return persisted instance of class {@code c} matching {@code id}, or {@code null} if no such instance
	 * @throws SQLException if a database error occurs
	 * @throws InstantiationException 
	 * @throws IllegalAccessException 
	 */
	public <T> T get(Class<T> c, long id) throws SQLException, InstantiationException, IllegalAccessException {
		T result = null;		
		String table = getTable(c);	// Get appropriate table, create if needed
		
		try (Connection conn = getConn()) {
			try (PreparedStatement s = conn.prepareStatement(buildGetId(table))) {
				s.setLong(1, id);
				
				ResultSet rs = s.executeQuery();
				if (rs.next()) {	// Found result
					result = c.newInstance();
					
					for (Field field : getPersistentFields(c)) {
						field.set(result, field.getAnnotation(Reference.class) != null ? get(field.getType(), rs.getLong(field.getName())) : rs.getObject(field.getName()));
					}
				}
			} catch (SQLException e) {
				conn.rollback();
				throw e;
			} catch (InstantiationException e) {
				conn.rollback();
				throw e;
			} catch (IllegalAccessException e) {
				conn.rollback();
				throw e;
			}
		}
		return result;
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
		StringBuilder builder = new StringBuilder("CREATE TABLE ").append(table).append(" (id BIGINT UNSIGNED PRIMARY KEY, ");
		for (Field field : fields) {
			Reference reference = field.getAnnotation(Reference.class);
			Sql sql = field.getAnnotation(Sql.class);
			
			builder.append(field.getName()).append(" ");
			builder.append(reference != null ? buildReference(field.getType()) : sql.value());
			builder.append(",");
		}
		builder.replace(builder.length() - 1, builder.length(), ")");
		
		String result = builder.toString();
		log.debug("Built default table init statement for " + table + ": " + result);
		
		return result;
	}
	private String buildReference(Class<?> c) throws SQLException {
		return "BIGINT UNSIGNED REFERENCES " + getTable(c) + " (id)";
	}
	private static String buildGetId(String table) {
		StringBuilder builder = new StringBuilder("SELECT * FROM ");
		builder.append(table).append(" WHERE id=? LIMIT 1");
		
		return builder.toString();
	}
	
	private static Iterable<Field> getPersistentFields(Class<?> c) {
		List<Field> fields = new ArrayList<>();
		
		for (Field field : c.getDeclaredFields()) {
			System.out.println(field);
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
