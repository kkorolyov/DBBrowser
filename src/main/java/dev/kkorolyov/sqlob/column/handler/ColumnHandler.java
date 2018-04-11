package dev.kkorolyov.sqlob.column.handler;

import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.request.CreateRequest;

import java.lang.reflect.Field;
import java.util.stream.Stream;

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

	/**
	 * Expands a primary CREATE request into as many requests as needed to handle all columns of the type provided by this handler.
	 * @param primaryRequest primary request to expand
	 * @return stream over {@code primaryRequest} and any auxiliary requests required to handle all columns of the provided type
	 */
	Stream<CreateRequest<?>> expandCreates(CreateRequest<?> primaryRequest);
}
