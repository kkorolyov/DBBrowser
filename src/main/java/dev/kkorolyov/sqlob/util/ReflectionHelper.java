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
	/**
	 * @param instance instance to set field value on
	 * @param f field to set value on
	 * @param value value to set
	 * @return {@code instance}
	 * @throws IllegalArgumentException if an issue occurs setting {@code f}'s value on {@code instance}
	 */
	public static Object setValue(Object instance, Field f, Object value) {
		try {
			f.setAccessible(true);
			f.set(instance, value);

			return instance;
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Unable to contribute " + f + " to " + instance, e);
		}
	}
}
