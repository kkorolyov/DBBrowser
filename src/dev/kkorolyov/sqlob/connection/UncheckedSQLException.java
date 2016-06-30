package dev.kkorolyov.sqlob.connection;

import java.sql.SQLException;

/**
 * Wraps a {@code SQLException} in an unchecked exception.
 */
public class UncheckedSQLException extends RuntimeException {
	private static final long serialVersionUID = 4087325880633443011L;
	
	/**
	 * Constructs a new instance of this exception.
	 * @param e {@code SQLException} to wrap
	 */
	public UncheckedSQLException(SQLException e) {
		super(e);
	}
}
