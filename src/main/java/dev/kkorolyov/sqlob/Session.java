package dev.kkorolyov.sqlob;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.persistence.Extractor;
import dev.kkorolyov.sqlob.persistence.NonPersistableException;
import dev.kkorolyov.sqlob.persistence.SqlobCache;
import dev.kkorolyov.sqlob.service.Mapper;
import dev.kkorolyov.sqlob.service.StatementExecutor;
import dev.kkorolyov.sqlob.utility.Condition;

/**
 * Persists objects using an external SQL database.
 * Allows for retrieval of objects by ID or by an arbitrary set of conditions.
 */
public class Session implements AutoCloseable {
	private static final Logger log = Logger.getLogger(Session.class.getName());
	
	private final DataSource ds;
	private final StatementExecutor executor;
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
	 * Constructs a new session.
	 * @param ds datasource to SQL database
	 * @param bufferSize maximum number of actions done before committing to database
	 */
	public Session(DataSource ds, int bufferSize) {
		this.ds = ds;
		this.bufferSize = bufferSize <= 1 ? 0 : bufferSize;

		log.info(() -> "Constructed new " + this);
	}
	
	/**
	 * Retrieves the ID of an instance of a class.
	 * @param o stored instance
	 * @return id of stored instance, or {@code null} if no such instance stored
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public UUID getId(Object o) {
		assertNotNull(o);

		Connection conn = getConn();
		try {
			return cache.get(o.getClass(), conn)
									.getId(o, conn);
		} catch (SQLException e) {
			conn.rollback();

			log.exception(e);
			throw e;
		}
	}
	
	/**
	 * Retrieves the instance of a class matching an ID.
	 * @param c type to retrieve
	 * @param id instance ID
	 * @return instance matching {@code id}, or {@code null} if no such instance
	 * @throws SQLException if a database error occurs
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public <T> T get(Class<T> c, UUID id) throws SQLException {
		assertNotNull(c, id);

		Connection conn = getConn();
		try {
			return cache.get(c, conn)
									.get(id, conn);
		} catch (SQLException e) {
			conn.rollback();

			log.exception(e);
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
		assertNotNull(c);

		Connection conn = getConn();
		try {
			return cache.get(c, conn)
									.get(condition, conn);
		} catch (SQLException e) {
			conn.rollback();

			log.exception(e);
			throw e;
		}
	}
	
	/**
	 * Stores an instance of a class and returns its ID.
	 * If an equivalent instance is already stored, no additional storage is performed and the ID of that instance is returned.
	 * @param o instance to store
	 * @return UUID of stored instance
	 * @throws SQLException if a database error occurs
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public UUID put(Object o) throws SQLException {
		assertNotNull(o);

		Connection conn = getConn();
		try {
			UUID result = cache.get(o.getClass(), conn).put(o, conn);
			
			finalizeAction();
			return result;
		} catch (SQLException e) {
			conn.rollback();

			log.exception(e);
			throw e;
		}
	}
	/**
	 * Stores an instance of a class using a predetermined ID.
	 * If the ID is reserved by another instance, that instance is replaced with {@code o}.
	 * @param id instance ID
	 * @param o instance to store
	 * @return {@code true} if a previous instance was overwritten by {@code o}
	 * @throws SQLException if a database error occurs
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public boolean put(UUID id, Object o) throws SQLException {
		assertNotNull(id, o);

		Connection conn = getConn();
		try {
			boolean result = cache.get(o.getClass(), conn).put(id, o, conn);
			
			finalizeAction();
			return result;
		} catch (SQLException e) {
			conn.rollback();

			log.exception(e);
			throw e;
		}
	}
	
	/**
	 * Deletes an instance of a class.
	 * @param c type to delete
	 * @param id instance ID
	 * @return {@code true} if instance deleted
	 * @throws SQLException if a database error occurs
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public boolean drop(Class<?> c, UUID id) throws SQLException {
		assertNotNull(c, id);

		Connection conn = getConn();
		try {
			boolean result = cache.get(c, conn).drop(id, conn);
			
			finalizeAction();
			return result;
		} catch (SQLException e) {
			conn.rollback();

			log.exception(e);
			throw e;
		}
	}
	/**
	 * Deletes all instances of a class matching a condition.
	 * @param c type to delete
	 * @param condition condition to match
	 * @return number of deleted instances
	 * @throws SQLException if a database error occurs
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public int drop(Class<?> c, Condition condition) throws SQLException {
		assertNotNull(c);

		Connection conn = getConn();
		try {
			int result = cache.get(c, conn).drop(condition, conn);
			
			finalizeAction();
			return result;
		} catch (SQLException e) {
			conn.rollback();

			log.exception(e);
			throw e;
		}
	}

	/**
	 * Commits all actions.
	 * @throws SQLException if a database error occurs
	 */
	public void flush() throws SQLException {
		if (conn != null) {
			try {
				conn.commit();

				log.info(() -> "Flushed " + bufferCounter + " statements");

				bufferCounter = 0;
			} catch (SQLException e) {
				conn.rollback();

				log.exception(e);
				throw e;
			}
		}
	}
	
	private void finalizeAction() throws SQLException {
		if (++bufferCounter >= bufferSize) flush();
	}

	/**
	 * Commits all actions and closes the underlying {@link Connection}.
	 * @throws SQLException if a database error occurs
	 */
	@Override
	public void close() throws SQLException {
		flush();

		if (conn != null) {
			conn.close();

			log.info(() -> "Closed " + conn);

			conn = null;
		}
	}
	
	/**
	 * Maps a {@code Class} to a {@code SQL} type.
	 * @param c Java class
	 * @param sql mapped SQL type
	 */
	public void mapType(Class<?> c, String sql) {
		cache.mapType(c, sql);
	}
	/**
	 * Maps a {@code Class} to an extractor.
	 * @param c Java class
	 * @param extractor extractor invoked when this retrieving a field of type {@code c}
	 */
	public void mapExtractor(Class<?> c, Extractor extractor) {
		cache.mapExtractor(c, extractor);
	}

	private Connection getConn() throws SQLException {
		if (conn == null) {
			conn = ds.getConnection();
			conn.setAutoCommit(false);	// Disable to reduce overhead
			
			log.debug(() -> "Retrieved new Connection from " + ds);
		}
		return conn;
	}
	
	private static void assertNotNull(Object... args) throws IllegalArgumentException {
		for (Object arg : args) {
			if (arg == null) throw new IllegalArgumentException();
		}
	}

	@Override
	public String toString() {
		return "Session{" +
					 "ds=" + ds +
					 ", bufferSize=" + bufferSize +
					 '}';
	}
}
