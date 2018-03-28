package dev.kkorolyov.sqlob.column;

import dev.kkorolyov.sqlob.request.ExecutionContext;
import dev.kkorolyov.sqlob.request.InsertRequest;
import dev.kkorolyov.sqlob.request.SelectRequest;
import dev.kkorolyov.sqlob.util.PersistenceHelper;
import dev.kkorolyov.sqlob.util.Where;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * A {@link Column} which references another table to retrieve its value.
 */
public class ReferencingColumn extends FieldBackedColumn<Object> {
	private final String referencedName;
	private final KeyColumn keyDelegate;

	/**
	 * Constructs a new referencing column.
	 * @param f associated field
	 */
	public ReferencingColumn(Field f) {
		this(f, new KeyColumn(PersistenceHelper.getName(f)));
	}
	private ReferencingColumn(Field f, KeyColumn keyDelegate) {
		super(f, keyDelegate.getSqlType());

		this.referencedName = PersistenceHelper.getName(f.getType());
		this.keyDelegate = keyDelegate;
	}

	@Override
	public Where contributeToWhere(Where where, ExecutionContext context) {
		where.resolve(getName(), value ->
				value != null
						? new SelectRequest<>(value)
						.execute(context.getConnection())
						.getId().orElseGet(UUID::randomUUID)
						: null);

		return where;
	}

	@Override
	public Object getValue(Object instance, ExecutionContext context) {
		return new InsertRequest<>(super.getValue(instance, context))
				.execute(context)
				.getId()
				.orElseThrow(() -> new IllegalStateException("This should never happen"));
	}
	@Override
	public Object getValue(ResultSet rs, ExecutionContext context) throws SQLException {
		return new SelectRequest<>(getType(), keyDelegate.getValue(rs, context))
				.execute(context)
				.getObject().orElse(null);
	}

	@Override
	public String getSql() {
		return super.getSql()
				+ ", FOREIGN KEY (" + getName() + ")"
				+ " REFERENCES " + getReferencedName() + " (" + KeyColumn.PRIMARY.getName() + ")"
				+ " ON DELETE SET NULL";
	}

	@Override
	public String getSqlType() {
		return keyDelegate.getSqlType();
	}

	/** @return referenced table name */
	public String getReferencedName() {
		return referencedName;
	}
}
