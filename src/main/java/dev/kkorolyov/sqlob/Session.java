package dev.kkorolyov.sqlob;

import dev.kkorolyov.simplefuncs.function.ThrowingSupplier;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.request.CreateRequest;
import dev.kkorolyov.sqlob.request.DeleteRequest;
import dev.kkorolyov.sqlob.request.InsertRequest;
import dev.kkorolyov.sqlob.request.Request;
import dev.kkorolyov.sqlob.request.SelectRequest;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.util.UncheckedSqlException;
import dev.kkorolyov.sqlob.util.Where;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static dev.kkorolyov.sqlob.util.UncheckedSqlException.wrapSqlException;

/**
 * Persists objects using an external SQL database.
 * Allows for retrieval of objects by ID or by an arbitrary set of conditions.
 * Throws {@link UncheckedSqlException} on any SQL-related errors.
 */
public class Session implements AutoCloseable {
	public static final int DEFAULT_BUFFER_SIZE = 100;

	private static final Logger LOG = Logger.getLogger(Session.class.getName());

	private final DataSource dataSource;
	private Connection connection;

	private final int bufferSize;
	private int bufferCounter = 0;

	private final Set<Class<?>> prepared = new HashSet<>();

	/**
	 * Constructs a new session with a default transaction buffer size of {@value #DEFAULT_BUFFER_SIZE}.
	 * @see #Session(DataSource, int)
	 */
	public Session(DataSource dataSource) {
		this(dataSource, DEFAULT_BUFFER_SIZE);
	}
	/**
	 * Constructs a new session.
	 * @param dataSource datasource to SQL database
	 * @param bufferSize maximum number of transactions queued before committing to database, constrained {@code > 0}
	 */
	public Session(DataSource dataSource, int bufferSize) {
		this.dataSource = dataSource;
		this.bufferSize = Math.max(1, bufferSize);
	}

	/**
	 * Retrieves the instance of a class matching an ID.
	 * @param c type to retrieve
	 * @param id instance ID
	 * @return result containing instance matching {@code id}, or empty result if no such instance
	 */
	public <T> Result<T> get(Class<T> c, UUID id) {
		return executeRequest(new SelectRequest<>(c, id));
	}
	/**
	 * Retrieves all instances of a class matching a condition.
	 * @param c type to retrieve
	 * @param where condition to match
	 * @return result containing all matching instances
	 */
	public <T> Result<T> get(Class<T> c, Where where) {
		return executeRequest(new SelectRequest<>(c, where));
	}
	/**
	 * Retrieves the result containing an instance.
	 * @param instance instance to search for
	 * @return result containing {@code instance}, or empty result if no such instance
	 */
	public <T> Result<T> get(T instance) {
		return executeRequest(new SelectRequest<>(instance));
	}

	/**
	 * Stores an instance of a class.
	 * If an equivalent instance is already stored, no additional storage is performed and the result containing that instance is returned.
	 * @param instance instance to store
	 * @return result containing stored instance
	 */
	public <T> Result<T> put(T instance) {
		return get(instance).asOptional()
				.orElseGet(() -> executeRequest(new InsertRequest<>(instance)));
	}
	/**
	 * Stores an instance of a class using a predetermined ID.
	 * If {@code id} is already mapped to an instance, that instance is replaced with {@code instance}.
	 * @param id instance ID
	 * @param instance instance to store
	 * @return result containing previous version of instance
	 */
	public <T> Result<T> put(UUID id, T instance) {
		Result<T> previous = get(instance);

		executeRequest(new InsertRequest<>(id, instance));

		return previous;
	}
	/**
	 * Stores instances of a class in bulk.
	 * @param instances instances to store
	 * @return result containing stored instances
	 */
	public <T> Result<T> put(Collection<T> instances) {
		return executeRequest(new InsertRequest<T>(instances));
	}
	/**
	 * Stores records in bulk.
	 * @param records instances to store mapped by their IDs
	 * @return result containing stored instances
	 */
	public <T> Result<T> put(Map<UUID, T> records) {
		return executeRequest(new InsertRequest<T>(records));
	}

	/**
	 * Deletes an instance.
	 * @param instance instance to delete
	 * @return whether instance existed and was deleted
	 */
	public <T> boolean drop(T instance) {
		return executeRequest(new DeleteRequest<>(instance)).size() > 0;
	}
	/**
	 * Deletes an instance of a class.
	 * @param c type to delete
	 * @param id ID of instance to delete
	 * @return whether instance existed and was deleted
	 */
	public <T> boolean drop(Class<T> c, UUID id) {
		return executeRequest(new DeleteRequest<>(c, id)).size() > 0;
	}
	/**
	 * Deletes all instances of a class matching a condition.
	 * @param c type to delete
	 * @param where condition to match
	 * @return result containing number of deleted instances
	 */
	public <T> Result<T> drop(Class<T> c, Where where) {
		return executeRequest(new DeleteRequest<>(c, where));
	}

	/**
	 * Executes a request and returns its result.
	 * @param request database request to execute
	 * @param <T> request target type
	 * @return result containing all changed records
	 */
	public <T> Result<T> executeRequest(Request<T> request) {
		Connection connection = startTransaction();

		if (!prepared.contains(request.getType())) {
			new CreateRequest<>(request.getType()).execute(connection);
			prepared.add(request.getType());
		}
		Result<T> result = request.execute(connection);

		endTransaction();

		return result;
	}

	private Connection startTransaction() {
		if (connection == null) {
				connection = wrapSqlException((ThrowingSupplier<Connection, SQLException>) dataSource::getConnection);
		}
		return connection;
	}
	private void endTransaction() {
		if (++bufferCounter >= bufferSize) close();
	}

	/**
	 * Commits buffered transactions and resets the buffer counter.
	 */
	@Override
	public void close() {
		try {
			connection.close();
			connection = null;

			LOG.info("Committed {} transactions", bufferCounter);

			bufferCounter = 0;
		} catch (SQLException e) {
			LOG.exception(e);
			throw new UncheckedSqlException(e);
		}
	}

	@Override
	public String toString() {
		return "Session{" +
				"dataSource=" + dataSource +
				", connection=" + connection +
				", bufferSize=" + bufferSize +
				", bufferCounter=" + bufferCounter +
				", prepared=" + prepared +
				'}';
	}
}
