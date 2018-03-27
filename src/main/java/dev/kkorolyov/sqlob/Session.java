package dev.kkorolyov.sqlob;

import dev.kkorolyov.simplefuncs.function.ThrowingSupplier;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.request.CreateRequest;
import dev.kkorolyov.sqlob.request.Request;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.util.UncheckedSqlException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static dev.kkorolyov.sqlob.util.UncheckedSqlException.wrapSqlException;

/**
 * Executes {@link Request}s using the current {@link Connection} of the associated {@link DataSource}.
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
