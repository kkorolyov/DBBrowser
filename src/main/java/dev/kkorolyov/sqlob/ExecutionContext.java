package dev.kkorolyov.sqlob;

import dev.kkorolyov.sqlob.request.Request;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static dev.kkorolyov.sqlob.util.UncheckedSqlException.wrapSqlException;

/**
 * Context of execution of a {@link Request}.
 */
public class ExecutionContext implements AutoCloseable {
	private final Connection connection;
	private Statement statement;
	private PreparedStatement preparedStatement;
	private boolean closed;

	/**
	 * Constructs a new request context.
	 * @param connection available connection
	 */
	ExecutionContext(Connection connection) {
		this.connection = connection;
	}

	/** @return available statement if already created, else a new statement */
	public Statement getStatement() {
		verifyNotClosed();

		return wrapSqlException(() ->
				(statement == null || statement.isClosed())
						? statement = connection.createStatement()
						: statement);
	}

	/**
	 * @return current prepared statement
	 * @throws IllegalStateException if no prepared statement generated yet
	 */
	public PreparedStatement prepareStatement() {
		verifyNotClosed();

		if (preparedStatement == null) throw new IllegalStateException("No prepared statement generated yet");
		return preparedStatement;
	}
	/**
	 * Generates and returns a prepared statement.
	 * @param sql statement SQL
	 * @return statement prepared with {@code sql}
	 */
	public PreparedStatement prepareStatement(String sql) {
		verifyNotClosed();

		preparedStatement = wrapSqlException(() -> connection.prepareStatement(sql));
		return preparedStatement;
	}

	/** @return database metadata */
	public DatabaseMetaData getMetadata() {
		verifyNotClosed();

		return wrapSqlException(connection::getMetaData);
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
