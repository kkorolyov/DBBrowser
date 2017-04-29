package dev.kkorolyov.sqlob.service;

/**
 * Contains common constants utilized by persistence classes.
 * Service class to be used exclusively by the persistence engine.
 */
final class Constants {
	static final String ID_NAME = "uuid";
	static final String ID_TYPE = "CHAR(36)";

	static String sanitize(String sql) {
		return sql.replaceAll("\\s+|;", "");
	}
}
