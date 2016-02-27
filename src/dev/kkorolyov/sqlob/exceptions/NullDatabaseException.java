package dev.kkorolyov.sqlob.exceptions;

/**
 * Exception thrown when a database cannot be located on a host.
 */
public class NullDatabaseException extends Exception {
	private static final long serialVersionUID = 3109425600343611100L;

	/**
	 * Constructs an instance of this exception.
	 * @param host name of the host being searched
	 * @param database name of the database searching for
	 */
	public NullDatabaseException(String host, String database) {
		super(host + " does not contain database: " + database);
	}
}
