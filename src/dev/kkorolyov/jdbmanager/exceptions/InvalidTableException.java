package dev.kkorolyov.jdbmanager.exceptions;

public class InvalidTableException extends Exception {
	private static final long serialVersionUID = -8282433808355407065L;

	public InvalidTableException(String dbName, String tableName) {
		super(dbName + " does not contain table: " + tableName);
	}
}
