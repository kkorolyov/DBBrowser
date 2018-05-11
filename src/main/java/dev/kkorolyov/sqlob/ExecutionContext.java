package dev.kkorolyov.sqlob;

import dev.kkorolyov.simplefuncs.function.ThrowingSupplier;
import dev.kkorolyov.sqlob.request.Request;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static dev.kkorolyov.sqlob.util.UncheckedSqlException.wrapSqlException;

/**
 * Context of execution of a {@link Request}.
 */
public class ExecutionContext implements AutoCloseable {
	private final Connection connection;
	private boolean closed;

	/**
	 * Constructs a new request context.
	 * @param connection available connection
	 */
	ExecutionContext(Connection connection) {
		this.connection = connection;
	}

	/**
	 * Generates and returns a statement.
	 * @return new statement
	 */
	public Statement generateStatement() {
		verifyNotClosed();

		return wrapSqlException((ThrowingSupplier<Statement, SQLException>) connection::createStatement);
	}
	/**
	 * Generates and returns a prepared statement.
	 * @param sql statement SQL
	 * @return statement prepared with {@code sql}
	 */
	public PreparedStatement generateStatement(String sql) {
		verifyNotClosed();

		return wrapSqlException(() -> connection.prepareStatement(sql));
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
