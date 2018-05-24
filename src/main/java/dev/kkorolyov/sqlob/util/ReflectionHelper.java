package dev.kkorolyov.sqlob.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

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

	/**
	 * Instances a new instance of {@code c}.
	 * @param c class to instantiate
	 * @param <T> instantiated type
	 * @return new {@code T}
	 * @throws IllegalArgumentException if {@code c} has no no-arg constructor
	 */
	public static <T> T newInstance(Class<T> c) {
		try {
			Constructor<T> noArgConstructor = c.getDeclaredConstructor();
			noArgConstructor.setAccessible(true);

			return noArgConstructor.newInstance();
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(c + " does not provide a no-arg constructor");
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Verifies that {@code f}'s type is a subclass of {@code c}.
	 * @param f field to verify
	 * @param c expected field type
	 * @throws IllegalArgumentException if {@code f}'s type is not assignable from {@code c}
	 */
	public static void verifyType(Field f, Class<?> c) {
		if (!c.isAssignableFrom(f.getType())) {
			throw new IllegalArgumentException(f + " must be a assignable from " + c);
		}
	}

	/**
	 * @param f field to get generic parameters from
	 * @return generic parameters of {@code f}'s type as {@code Class}es, with non-{@code Class} parameters replaced with {@code null}
	 * @throws IllegalArgumentException if {@code f}'s type is not parameterized
	 */
	public static Class[] getGenericParameters(Field f) {
		Type type = f.getGenericType();
		if (type instanceof ParameterizedType) {
			return Arrays.stream(((ParameterizedType) type).getActualTypeArguments())
					.map(t ->
							t instanceof Class
									? (Class) t
									: null)
					.toArray(Class[]::new);
		} else {
			throw new IllegalArgumentException(f + " is not of a parameterized type");
		}
	}
}
