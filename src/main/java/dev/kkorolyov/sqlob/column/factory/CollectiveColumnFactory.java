package dev.kkorolyov.sqlob.column.factory;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.type.SqlobType;
import dev.kkorolyov.sqlob.util.PersistenceHelper;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Handles collection fields.
 */
public class CollectiveColumnFactory implements ColumnFactory {
	@Override
	public FieldBackedColumn<?> get(Field f) {
		return new CollectiveColumn(f);
	}

	@Override
	public boolean accepts(Field f) {
		return Collection.class.isAssignableFrom(f.getType());
	}

	// TODO This shouldn't need to be exposed
	public static class CollectiveColumn extends FieldBackedColumn<Collection> {
		private final KeyColumn keyDelegate;

		private CollectiveColumn(Field f) {
			this(f, KeyColumn.foreign(PersistenceHelper.getName(f), PersistenceHelper.getName(f.getType())));
		}
		private CollectiveColumn(Field f, KeyColumn keyDelegate) {
			super(f, (SqlobType<? super Collection>) keyDelegate.getSqlobType());
			this.keyDelegate = keyDelegate;
		}

		@Override
		public Collection toFieldValue(Object instance, ExecutionContext context) {
			return (Collection) super.toFieldValue(instance, context);
		}
	}
}
