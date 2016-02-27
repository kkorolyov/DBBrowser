package dev.kkorolyov.sqlob.exceptions;

/**
 * Exception thrown when attempting to create a table of the same name as a pre-existing table in a database.
 */
public class DuplicateTableException extends Exception {
	private static final long serialVersionUID = -3926307033700084888L;

	/**
	 * Constructs an instance of this exception
	 * @param database name of the database on which the table creation is attempted
	 * @param table name of the table in the creation attempt
	 */
	public DuplicateTableException(String database, String table) {
		super(database + " already contains table: " + table);
	}
}
