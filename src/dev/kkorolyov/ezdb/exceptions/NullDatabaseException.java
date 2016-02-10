package dev.kkorolyov.ezdb.exceptions;

public class NullDatabaseException extends Exception {
	private static final long serialVersionUID = 3109425600343611100L;

	public NullDatabaseException(String hostName, String dbName) {
		super(hostName + " does not contain database: " + dbName);
	}
}
