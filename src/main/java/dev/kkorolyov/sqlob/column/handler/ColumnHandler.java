package dev.kkorolyov.sqlob.column.handler;

import dev.kkorolyov.sqlob.column.FieldBackedColumn;

import java.lang.reflect.Field;

/**
 * Handles provisioning columns for specific fields on a persisted object.
 */
public interface ColumnHandler {
	/**
	 * @param f field to wrap
	 * @return column associated with {@code f}
	 */
	FieldBackedColumn<?> get(Field f);
	/**
	 * @param f field to test
	 * @return whether this handler accepts field {@code f}
	 */
	boolean accepts(Field f);
}
