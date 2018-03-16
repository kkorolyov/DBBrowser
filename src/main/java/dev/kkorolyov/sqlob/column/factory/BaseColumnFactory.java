package dev.kkorolyov.sqlob.column.factory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * Column factory which accepts fields based on type.
 */
public abstract class BaseColumnFactory implements ColumnFactory {
	private final Collection<Class<?>> acceptedTypes = new HashSet<>();

	/** @see #BaseColumnFactory(Iterable)*/
	protected BaseColumnFactory(Class<?>... acceptedTypes) {
		this(Arrays.asList(acceptedTypes));
	}
	/**
	 * Constructs a new column factory.
	 * @param acceptedTypes all accepted top-level types of fields
	 */
	protected BaseColumnFactory(Iterable<Class<?>> acceptedTypes) {
		addAll(acceptedTypes);
	}
	/**
	 * Adds all {@code types} to accepted types.
	 * @param types accepted types to add
	 */
	protected final void addAll(Iterable<Class<?>> types) {
		types.forEach(acceptedTypes::add);
	}

	@Override
	public final boolean accepts(Field f) {
		return acceptedTypes.stream()
				.anyMatch(acceptedType -> acceptedType.isAssignableFrom(f.getType()));
	}
}
