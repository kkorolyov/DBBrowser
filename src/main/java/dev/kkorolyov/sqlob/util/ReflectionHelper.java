package dev.kkorolyov.sqlob.util;

import java.lang.reflect.Field;

/**
 * Provides static utility methods for dealing with reflection operations.
 */
public class ReflectionHelper {

	/**
	 * @param instance instance to extract from
	 * @param f field to extract from
	 * @return value extracted from {@code f} on {@code instance}
	 * @throws IllegalArgumentException if an issue occurs extracting {@code f}'s value on {@code instance}
	 */
	public static Object getValue(Object instance, Field f) {
		try {
			f.setAccessible(true);
			return f.get(instance);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Unable to extract " + f + " value from " + instance, e);
		}
	}
}
