package dev.kkorolyov.sqlob.column.handler;

import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.request.CreateRequest;
import dev.kkorolyov.sqlob.type.SqlobType;
import dev.kkorolyov.sqlob.type.factory.SqlobTypeFactory;

import java.lang.reflect.Field;
import java.util.stream.Stream;

/**
 * Handles fields mappable directly to SQL types.
 * @see SqlobType
 */
public class PrimitiveColumnHandler implements ColumnHandler {
	@Override
	public FieldBackedColumn<?> get(Field f) {
		return new PrimitiveColumn<>(f, SqlobTypeFactory.get(f.getType()));
	}
	@Override
	public boolean accepts(Field f) {
		return SqlobTypeFactory.poll(f.getType()).isPresent();
	}

	@Override
	public Stream<CreateRequest<?>> expandCreates(CreateRequest<?> primaryRequest) {
		return Stream.of(primaryRequest);
	}

	private static class PrimitiveColumn<T> extends FieldBackedColumn<T> {
		PrimitiveColumn(Field f, SqlobType<? super T> sqlobType) {
			super(f, sqlobType);
		}
	}
}
