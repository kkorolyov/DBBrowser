package dev.kkorolyov.dbbrowser.exceptions;

public class NullDBException extends Exception {
	private static final long serialVersionUID = 3109425600343611100L;

	public NullDBException(String hostName, String dbName) {
		super(hostName + " does not contain database: " + dbName);
	}
}
