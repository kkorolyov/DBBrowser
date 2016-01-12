package dev.kkorolyov.dbbrowser.exceptions;

public class NullTableException extends Exception {
	private static final long serialVersionUID = -8282433808355407065L;

	public NullTableException(String dbName, String tableName) {
		super(dbName + " does not contain table: " + tableName);
	}
}
