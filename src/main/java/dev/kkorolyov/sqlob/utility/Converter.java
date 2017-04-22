package dev.kkorolyov.sqlob.utility;

/**
 * Converts a Java object to another type.
 */
@FunctionalInterface
public interface Converter<T, R> {
	/**
	 * Converts an object to another type
	 * @param o object to convert
	 * @return type {@code R} representation of {@code o}
	 */
	R execute(T o);
}
