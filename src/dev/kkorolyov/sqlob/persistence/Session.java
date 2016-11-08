package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
	 * Convenience method for {@link #get(Class, BigInteger)}.
	 */
	public <T> T get(Class<T> c, long id) throws SQLException {
		return get(c, BigInteger.valueOf(id));
	}
	/**
	 * Retrieves an instance of a class matching an ID.
	 * @param c type to retrieve
	 * @param id instance id
	 * @return persisted instance of class {@code c} matching {@code id}, or {@code null} if no such instance
	 * @throws SQLException if a database error occurs
	 */
	public <T> T get(Class<T> c, BigInteger id) throws SQLException {
		String table = getTable(c);	// Get appropriate table, create if needed
		
		return null;	// TODO
	}
	
	private String getTable(Class<?> c) throws SQLException {
		Sql override = c.getAnnotation(Sql.class);
		String table = override != null ? override.value() : c.getSimpleName();
			
		if (!tableExists(table))
			initTable(buildDefaultInit(table, c.getDeclaredFields()));
		
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
	
	private String buildDefaultInit(String table, Field[] fields) throws SQLException {
		StringBuilder builder = new StringBuilder("CREATE TABLE");
		
		builder.append(" ").append(table).append(" (");
		builder.append("id BIGINT UNSIGNED PRIMARY KEY,");
		
		for (Field field : fields) {
			Transient transientAnnotation = field.getAnnotation(Transient.class);
			Reference reference = field.getAnnotation(Reference.class);
			Sql sql = field.getAnnotation(Sql.class);
			
			if (transientAnnotation == null && (reference != null || sql != null)) {	// Not transient
				builder.append(field.getName()).append(" ");
				builder.append(reference != null ? buildReference(field.getType()) : sql.value());
				builder.append(",");
			}
		}
		builder.replace(builder.length() - 1, builder.length(), ")");
		
		String result = builder.toString();
		log.debug("Built default table init statement for " + table + ": " + result);
		
		return result;
	}
	private String buildReference(Class<?> c) throws SQLException {
		return "BIGINT UNSIGNED REFERENCES " + getTable(c) + " (id)";
	}
	
	private Connection getConn() throws SQLException {
		Connection conn = ds.getConnection();
		conn.setAutoCommit(false);
		
		return conn;
	}
}
