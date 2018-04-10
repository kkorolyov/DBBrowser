package dev.kkorolyov.sqlob.column.factory;

import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.type.SqlobType;
import dev.kkorolyov.sqlob.type.factory.SqlobTypeFactory;

import java.lang.reflect.Field;

/**
 * Handles fields mappable directly to SQL types.
 * @see SqlobType
 */
public class PrimitiveColumnFactory implements ColumnFactory {
	@Override
	public FieldBackedColumn<?> get(Field f) {
		return new PrimitiveColumn<>(f, SqlobTypeFactory.get(f.getType()));
	}

	@Override
	public boolean accepts(Field f) {
		return SqlobTypeFactory.poll(f.getType()).isPresent();
	}

	private static class PrimitiveColumn<T> extends FieldBackedColumn<T> {
		PrimitiveColumn(Field f, SqlobType<? super T> sqlobType) {
			super(f, sqlobType);
		}
	}
}
