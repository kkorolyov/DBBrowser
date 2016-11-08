package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import dev.kkorolyov.sqlob.annotation.Reference;
import dev.kkorolyov.sqlob.annotation.Sql;
import dev.kkorolyov.sqlob.annotation.Storage;
import dev.kkorolyov.sqlob.annotation.Transient;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.logging.LoggerInterface;

/**
 * Persists objects using an external SQL database.
 * Allows for retrieval of objects by ID or by an arbitrary set of conditions.
 */
public class Connection {
	private static final LoggerInterface log = Logger.getLogger(Connection.class.getName());
	
	private final java.sql.Connection conn;

	/**
	 * Constructs a new connection to a {@code Datasource}.
	 * @param ds datasource to SQL database
	 * @throws SQLException if a database error occurs
	 */
	public Connection(DataSource ds) throws SQLException {
		conn = ds.getConnection();
		conn.setAutoCommit(false);
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
		String table = c.getSimpleName();	// Default to class name
		
		Storage storage = c.getAnnotation(Storage.class);
		if (storage != null) {
			if (storage.table() != null) 
				table = storage.table();
		}
		if (!tableExists(table))
			initTable(table, storage != null && storage.init() != null ? storage.init() : buildDefaultInit(c));
		
		return table;
	}
	
	private boolean tableExists(String table) throws SQLException {
		try (ResultSet rs = conn.getMetaData().getTables(null, null, table, null)) {
			return rs.next();	// Table exists
		} catch (SQLException e) {
			conn.rollback();
			throw e;
		}
	}
	
	private void initTable(String table, String init) throws SQLException {
		System.out.println("Init table: " + table + " with " + init);
		try (Statement s = conn.createStatement()) {
			s.executeUpdate(init);
			conn.commit();
		} catch (SQLException e) {
			conn.rollback();
			throw e;
		}
	}
	
	private String buildDefaultInit(Class<?> c) throws SQLException {
		StringBuilder builder = new StringBuilder("CREATE TABLE");
		
		builder.append(" ").append(c.getSimpleName()).append(" (");
		builder.append("id BIGINT UNSIGNED PRIMARY KEY,");
		
		for (Field field : c.getDeclaredFields()) {
			Sql sql = field.getAnnotation(Sql.class);
			Reference reference = field.getAnnotation(Reference.class);
			
			if (sql != null || reference != null) {	// Not transient
				builder.append(field.getName()).append(" ");
				builder.append(reference != null ? buildReference(field.getType()) : sql.value());
				builder.append(",");
			}
		}
		builder.replace(builder.length() - 1, builder.length(), ")");
		
		return builder.toString();
	}
	private String buildReference(Class<?> c) throws SQLException {
		return "BIGINT UNSIGNED REFERENCES " + getTable(c) + " (id)";
	}
}
