package dev.kkorolyov.sqlob.column.factory;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.request.InsertRequest;
import dev.kkorolyov.sqlob.request.SelectRequest;
import dev.kkorolyov.sqlob.type.SqlobType;
import dev.kkorolyov.sqlob.util.PersistenceHelper;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.UUID;

/**
 * Handles fields mapped to foreign-key columns.
 * Accepts all types.
 */
public class ReferencingColumnFactory implements ColumnFactory {
	@Override
	public FieldBackedColumn<?> get(Field f) {
		return new ReferencingColumn(f);
	}

	@Override
	public boolean accepts(Field f) {
		return true;
	}

	// TODO This shouldn't need to be exposed
	public static class ReferencingColumn extends FieldBackedColumn<Object> {
		private final KeyColumn keyDelegate;

		private ReferencingColumn(Field f) {
			this(f, KeyColumn.foreign(PersistenceHelper.getName(f), PersistenceHelper.getName(f.getType())));
		}
		private ReferencingColumn(Field f, KeyColumn keyDelegate) {
			super(f, (SqlobType<? super Object>) keyDelegate.getSqlobType());
			this.keyDelegate = keyDelegate;
		}

		@Override
		public Object resolveCriterion(Object value, ExecutionContext context) {
			return value != null
					? new SelectRequest<>(value)
					.execute(context)
					.getId().orElseGet(UUID::randomUUID)
					: null;
		}

		@Override
		public Object toFieldValue(Object instance, ExecutionContext context) {
			return new InsertRequest<>(super.toFieldValue(instance, context))
					.execute(context)
					.getId()
					.orElseThrow(() -> new IllegalStateException("This should never happen"));
		}

		@Override
		public Object getValue(ResultSet rs, ExecutionContext context) {
			return new SelectRequest<>(getType(), keyDelegate.getValue(rs, context))
					.execute(context)
					.getObject().orElse(null);
		}

		@Override
		public String getSql(ExecutionContext context) {
			return keyDelegate.getSql(context);
		}
	}
}
