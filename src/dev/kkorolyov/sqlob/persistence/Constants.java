package dev.kkorolyov.sqlob.persistence;

import java.util.UUID;

/**
 * Contains common constants utilized by persistence classes.
 */
class Constants {
	static final String ID_NAME = "uuid";
	static final Class<?> ID_CLASS_TYPE = UUID.class;
	static final String ID_SQL_TYPE = "CHAR(36)";
}
