package dev.kkorolyov.sqlob.persistence;

/**
 * Provides mapping from Java to SQL types.
 */
@FunctionalInterface
public interface TypeMap {
	/**
	 * Returns the SQL type matching a Java class.
	 * @param c Java class
	 * @return matching SQL type, or {@code null} if unknown or no such type
	 */
	String toSql(Class<?> c);
}
