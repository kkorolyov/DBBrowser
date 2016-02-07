package dev.kkorolyov.ezdb.exceptions;

public class DuplicateTableException extends Exception {
	private static final long serialVersionUID = -3926307033700084888L;

	public DuplicateTableException(String db, String table) {
		super(db + " already contains table: " + table);
	}
}
