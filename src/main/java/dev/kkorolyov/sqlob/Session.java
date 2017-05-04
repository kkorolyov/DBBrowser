package dev.kkorolyov.sqlob;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import dev.kkorolyov.sqlob.logging.Logger;
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
	private final Mapper mapper;
	private final StatementExecutor executor;

	private final int bufferSize;
	private int bufferCounter = 0;

	private final Set<Class<?>> prepared = new HashSet<>();

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

		mapper = new Mapper();
		executor = new StatementExecutor(mapper);
	}

	/**
	 * Retrieves the instance of a class matching an ID.
	 * @param c type to retrieve
	 * @param id instance ID
	 * @return instance matching {@code id}, or {@code null} if no such instance
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public <T> T get(Class<T> c, UUID id) {
		assertNotNull(c, id);

		prepareAction(c);
		T result = executor.select(c, id);
		finalizeAction();

		return result;
	}
	/**
	 * Retrieves all instances of a class matching a condition.
	 * @param c type to retrieve
	 * @param where condition to match
	 * @return all matching instances mapped to their respective IDs
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public <T> Map<UUID, T> get(Class<T> c, Condition where) {
		assertNotNull(c);

		prepareAction(c);
		Map<UUID, T> result = executor.select(c, where);
		finalizeAction();

		return result;
	}

	/**
	 * Retrieves the ID of an instance of a class.
	 * @param o stored instance
	 * @return id of stored instance, or {@code null} if no such instance stored
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public UUID getId(Object o) {
		assertNotNull(o);

		prepareAction(o.getClass());
		UUID result = executor.selectId(o);
		finalizeAction();

		return result;
	}

	/**
	 * Stores an instance of a class and returns its ID.
	 * If an equivalent instance is already stored, no additional storage is performed and the ID of that instance is returned.
	 * @param o instance to store
	 * @return UUID of stored instance
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public UUID put(Object o) {
		assertNotNull(o);

		prepareAction(o.getClass());

		UUID result = getId(o);
		if (result == null) result = executor.insert(o);

		finalizeAction();

		return result;
	}
	/**
	 * Stores an instance of a class using a predetermined ID.
	 * If {@code id} is already mapped to an instance, that instance is updated to match {@code o}.
	 * @param id instance ID
	 * @param o instance to store
	 * @return {@code true} if a previous instance was overwritten by {@code o}
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public boolean put(UUID id, Object o) {
		assertNotNull(id, o);

		prepareAction(o.getClass());
		boolean result = executor.update(id, o);
		finalizeAction();

		return result;
	}
	
	/**
	 * Deletes an instance of a class.
	 * @param c type to delete
	 * @param id instance ID
	 * @return {@code true} if instance deleted
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public boolean drop(Class<?> c, UUID id) {
		assertNotNull(c, id);

		prepareAction(c);
		boolean result = executor.delete(c, id);
		finalizeAction();

		return result;
	}
	/**
	 * Deletes all instances of a class matching a condition.
	 * @param c type to delete
	 * @param where condition to match
	 * @return number of deleted instances
	 * @throws SQLException if a database error occurs
	 * @throws NonPersistableException if the class does not follow persistence requirements
	 */
	public int drop(Class<?> c, Condition where) throws SQLException {
		assertNotNull(c);

		prepareAction(c);
		int result = executor.delete(c, where);
		finalizeAction();

		return result;
	}

	private static void assertNotNull(Object... args) throws IllegalArgumentException {
		for (Object arg : args) {
			if (arg == null) throw new IllegalArgumentException();
		}
	}

	private void finalizeAction() {
		if (++bufferCounter >= bufferSize) flush();
	}

	private void prepareAction(Class<?> c) {
		try {
			if (executor.isClosed()) executor.setConnection(ds.getConnection());

			if (!prepared.contains(c)) {
				executor.create(c);
				prepared.add(c);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Commits all active actions.
	 */
	public void flush() {
		executor.flush();
		log.info("Flushed {} statements", bufferCounter);

		bufferCounter = 0;
	}
	/**
	 * Flushes and closes the underlying {@link StatementExecutor}.
	 */
	@Override
	public void close() throws Exception {
		executor.close();
		log.info("Closed");
	}

	/** @return mapper used by this session */
	public Mapper getMapper() {
		return mapper;
	}

	@Override
	public String toString() {
		return "Session{" +
					 "ds=" + ds +
					 ", mapper=" + mapper +
					 ", executor=" + executor +
					 ", bufferSize=" + bufferSize +
					 ", bufferCounter=" + bufferCounter +
					 ", prepared=" + prepared +
					 '}';
	}
}
