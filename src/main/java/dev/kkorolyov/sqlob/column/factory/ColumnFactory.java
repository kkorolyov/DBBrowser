package dev.kkorolyov.sqlob.column.factory;

import dev.kkorolyov.sqlob.column.Column;

import java.lang.reflect.Field;

/**
 * Provides columns for individual fields on a persisted object.
 */
public interface ColumnFactory {
	/**
	 * @param f field to wrap
	 * @return column associated with {@code f}
	 */
	Column<?> get(Field f);

	/**
	 * @param f field to test
	 * @return whether this handler accepts field {@code f}
	 */
	boolean accepts(Field f);
}
