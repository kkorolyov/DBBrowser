package dev.kkorolyov.sqlob.column.handler;

import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.type.SqlobType;
import dev.kkorolyov.sqlob.type.factory.SqlobTypeFactory;

import java.lang.reflect.Field;

/**
 * Handles fields mappable directly to SQL types.
 * @see SqlobType
 */
public class PrimitiveColumnHandler implements ColumnHandler {
	@Override
	public FieldBackedColumn<?> get(Field f) {
		return new FieldBackedColumn<>(f, SqlobTypeFactory.get(f.getType()));
	}
	@Override
	public boolean accepts(Field f) {
		return SqlobTypeFactory.poll(f.getType()).isPresent();
	}
}
