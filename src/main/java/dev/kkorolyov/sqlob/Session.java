package dev.kkorolyov.sqlob;

import dev.kkorolyov.simplefuncs.function.ThrowingRunnable;
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
	private static final Logger LOG = Logger.getLogger(Session.class.getName());

	private final DataSource dataSource;
	private Connection connection;

	private int bufferCounter = 0;

	private final Set<Class<?>> prepared = new HashSet<>();

	/**
	 * Constructs a new session.
	 * @param dataSource datasource to SQL database
	 */
	public Session(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Executes a request using an available connection and returns its result.
	 * Because a session is auto-closeable but infinitely reusable,
	 * this method should be called with the session instance in a try-with-resources block to group related transactions and avoid unhandled commit failures.
	 * <pre>
	 *   Session session = new Session(ds);
	 *   try (session) {
	 *     session.execute(new InsertRequest(...));
	 *     session.execute(new DeleteRequest(...));
	 *   } catch (UncheckedSqlException e) {
	 *     session.rollback();
	 *   }
	 * </pre>
	 * @param request database request to execute
	 * @param <T> request target type
	 * @return result containing all changed records
	 * @throws UncheckedSqlException if a SQL issue occurs
	 */
	public <T> Result<T> execute(Request<T> request) {
		try (ExecutionContext context = startTransaction()) {
			if (!prepared.contains(request.getType())) {
				create(request.getType()).execute(context);
				prepared.add(request.getType());
			}
			Result<T> result = request.execute(context);

			endTransaction();

			return result;
		}
	}
	private ExecutionContext startTransaction() {
		if (connection == null) {
			connection = wrapSqlException(() -> {
				Connection conn = dataSource.getConnection();
				conn.setAutoCommit(false);
				return conn;
			});
		}
		return new ExecutionContext(connection);
	}
	private void endTransaction() {
		bufferCounter++;
	}

	/**
	 * Rolls back the current transaction.
	 * @throws UncheckedSqlException if a SQL issue occurs
	 */
	public void rollback() {
		if (connection != null) {
			wrapSqlException((ThrowingRunnable<SQLException>) connection::rollback);

			LOG.info("Rolled back {} transactions", bufferCounter);

			bufferCounter = 0;
		}
	}

	/**
	 * Commits buffered transactions and resets the buffer counter.
	 * @throws UncheckedSqlException if a SQL issue occurs
	 */
	@Override
	public void close() {
		if (connection != null) {
			wrapSqlException(() -> {
				connection.commit();
				connection.close();
				connection = null;

				LOG.info("Committed {} transactions", bufferCounter);

				bufferCounter = 0;
			});
		}
	}

	@Override
	public String toString() {
		return "Session{" +
				"dataSource=" + dataSource +
				", connection=" + connection +
				", bufferCounter=" + bufferCounter +
				", prepared=" + prepared +
				'}';
	}

	<T> CreateRequest<T> create(Class<T> c) {
		return new CreateRequest<>(c);
	}
}
