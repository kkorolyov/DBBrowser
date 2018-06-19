package dev.kkorolyov.sqlob.type;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Collection;

/**
 * A database-specific direct mapping between SQL and Java types.
 * @param <T> associated base Java type
 */
public interface SqlobType<T> {
	/** @return all associated Java types */
	Collection<Class<T>> getTypes();
	/**
	 * @param metaData metadata for which to get SQL type
	 * @return SQL type of this SQLOb type specific to the provided database
	 */
	String getSqlType(DatabaseMetaData metaData);

	/**
	 * Gets the converted representation of a Java value within the context of a specific database.
	 * @param metaData metadata containing relevant database type
	 * @param value value to convert to database-specific type
	 * @return {@code value} converted to database-specific type
	 */
	Object get(DatabaseMetaData metaData, T value);
	/**
	 * Gets an instance of this SQLOb type's Java type from a result set within the context of a specific database.
	 * @param metaData metadata containing relevant database type
	 * @param rs result set to get from
	 * @param column name of column to get from
	 * @return Java type retrieved from {@code column} in {@code rs} within the context of {@code metaData}
	 */
	T get(DatabaseMetaData metaData, ResultSet rs, String column);
}
