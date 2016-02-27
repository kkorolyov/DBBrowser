package dev.kkorolyov.sqlob.exceptions;

/**
 * Exception thrown when a table cannot be located in a database.
 */
public class NullTableException extends Exception {
	private static final long serialVersionUID = -8282433808355407065L;

	/**
	 * Constructs an instance of this exception.
	 * @param database name of the database being searched
	 * @param table name of the table searching for
	 */
	public NullTableException(String database, String table) {
		super(database + " does not contain table: " + table);
	}
}
