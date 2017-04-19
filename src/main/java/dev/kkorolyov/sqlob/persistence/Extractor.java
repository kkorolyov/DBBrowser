package dev.kkorolyov.sqlob.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Extracts a Java {@code Object} from a column in a {@code ResultSet}.
 * @param <T> type extracted by this extractor
 */
@FunctionalInterface
public interface Extractor<T> {
	/**
	 * Extracts an object.
	 * @param rs result set to extract from
	 * @param columnName name of column to extract
	 * @return object
	 * @throws SQLException if a database error occurs
	 */
	T execute(ResultSet rs, String columnName) throws SQLException;
}
