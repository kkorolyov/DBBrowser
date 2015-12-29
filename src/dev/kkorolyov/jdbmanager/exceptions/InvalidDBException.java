package dev.kkorolyov.jdbmanager.exceptions;

public class InvalidDBException extends Exception {
	private static final long serialVersionUID = 3109425600343611100L;

	public InvalidDBException(String hostName, String dbName) {
		super(hostName + " does not contain database: " + dbName);
	}
}
