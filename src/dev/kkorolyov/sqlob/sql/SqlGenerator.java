package dev.kkorolyov.sqlob.sql;

/**
 * Generates SQL statements.
 */
public class SqlGenerator {
	private final String 	idName,
												idType;

	public SqlGenerator(String idName, String idType) {
		this.idName = idName;
		this.idType = idType;
	}
}
