package dev.kkorolyov.sqlob.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.sql.Condition;

/**
 * Persists objects using an external SQL database.
 * Allows for retrieval of objects by ID or by an arbitrary set of conditions.
 */
public class Session implements AutoCloseable {
	private static final Logger log = Logger.getLogger(Session.class.getName());
	
	private final DataSource ds;
	private final int bufferSize;
	private int bufferCounter = 0;
	private Connection conn;
	private SqlobCache cache = new SqlobCache();

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
			T result = cache.get(c, getConn()).getInstance(uuid, getConn());
			bufferCounter++;
			
			if (bufferSize == 0)
				flush();
			
			log.debug(() -> (result == null ? "Failed to find " : "Found ") + c.getName() + " with id: " + uuid);
			return result;
		} catch (SQLException e) {
			getConn().rollback();
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
			Set<T> results = cache.get(c, getConn()).getInstances(condition, getConn());
			bufferCounter++;
			
			if (bufferSize == 0)
				flush();
			
			log.debug(() -> "Found " + results.size() + " results for " + c.getName() + " matching condition: " + condition);
			return results;
		} catch (SQLException e) {
			getConn().rollback();
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
			UUID result = cache.get(o.getClass(), getConn()).put(o, getConn());
			bufferCounter++;
			
			if (bufferSize == 0)
				flush();
			
			return result;
		} catch (SQLException e) {
			getConn().rollback();
			throw e;
		}
	}
	
	/**
	 * Commits all active requests and closes any database connections this session is holding.
	 * @throws SQLException if a database error occurs
	 */
	public void flush() throws SQLException {
		if (conn != null) {
			conn.close();
			conn = null;
			
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
	
	/** @return type map used by this session for mapping Java classes to SQL types */
	public Map<Class<?>, String> getTypeMap() {
		return cache.getTypeMap();
	}
	/** @param typeMap new Java-to-SQL type map, if {@code null}, resets to default type map */
	public void setTypeMap(Map<Class<?>, String> typeMap) {
		cache.setTypeMap(typeMap);
	}
	
	private Connection getConn() throws SQLException {
		if (conn != null && (bufferCounter >= bufferSize))
			flush();
		
		if (conn == null) {
			conn = ds.getConnection();
			conn.setAutoCommit(false);	// Disable to reduce overhead
			
			log.debug(() -> "Retrieved new Connection from " + ds);
		}
		return conn;
	}
}
