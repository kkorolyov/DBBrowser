package dev.kkorolyov.sqlob.request;

import java.sql.Connection;
import java.sql.Statement;

import static dev.kkorolyov.sqlob.util.UncheckedSqlException.wrapSqlException;

/**
 * Context of execution of a {@link Request} used to share state between it and all its sub-requests.
 */
public class ExecutionContext implements AutoCloseable {
	private final Connection connection;
	private Statement statement;
	private boolean closed;

	/**
	 * Constructs a new request context.
	 * @param connection available connection
	 */
	ExecutionContext(Connection connection) {
		this.connection = connection;
	}

	/** @return available connection */
	public Connection getConnection() {
		verifyNotClosed();
		return connection;
	}
	/** @return available statement */
	public Statement getStatement() {
		verifyNotClosed();

		return wrapSqlException(() ->
				(statement == null || statement.isClosed())
						? statement = connection.createStatement()
						: statement);
	}

	private void verifyNotClosed() {
		if (closed || wrapSqlException(connection::isClosed)) closed = true;
		if (closed) throw new IllegalStateException("Context is closed");
	}

	@Override
	public void close() {
		closed = true;
	}
}
