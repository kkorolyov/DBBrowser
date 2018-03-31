package dev.kkorolyov.sqlob.column.factory;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.request.InsertRequest;
import dev.kkorolyov.sqlob.request.SelectRequest;
import dev.kkorolyov.sqlob.util.PersistenceHelper;
import dev.kkorolyov.sqlob.util.Where;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.UUID;

/**
 * Handles fields mapped to foreign-key columns.
 * Accepts all types.
 */
public class ReferencingColumnFactory extends BaseColumnFactory {
	/**
	 * Constructs a new referencing column factory.
	 */
	public ReferencingColumnFactory() {
		super(Object.class);
	}

	@Override
	public FieldBackedColumn<?> get(Field f) {
		return new ReferencingColumn(f);
	}

	// TODO This shouldn't need to be exposed
	public static class ReferencingColumn extends FieldBackedColumn<Object> {
		private final String referencedName;
		private final KeyColumn keyDelegate;

		private ReferencingColumn(Field f) {
			this(f, new KeyColumn(PersistenceHelper.getName(f)));
		}
		private ReferencingColumn(Field f, KeyColumn keyDelegate) {
			super(f, keyDelegate.getSqlType());

			this.referencedName = PersistenceHelper.getName(f.getType());
			this.keyDelegate = keyDelegate;
		}

		@Override
		public Where contributeToWhere(Where where, ExecutionContext context) {
			return where.resolve(getName(), value ->
					value != null
							? new SelectRequest<>(value)
							.execute(context)
							.getId().orElseGet(UUID::randomUUID)
							: null);
		}

		@Override
		public Object getValue(Object instance, ExecutionContext context) {
			return new InsertRequest<>(super.getValue(instance, context))
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
		public String getSql() {
			return super.getSql()
					+ ", FOREIGN KEY (" + getName() + ")"
					+ " REFERENCES " + referencedName + " (" + KeyColumn.PRIMARY.getName() + ")"
					+ " ON DELETE SET NULL";
		}

		@Override
		public String getSqlType() {
			return keyDelegate.getSqlType();
		}
	}
}
