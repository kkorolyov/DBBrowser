package dev.kkorolyov.sqlob.column;

import dev.kkorolyov.sqlob.request.ExecutionContext;
import dev.kkorolyov.sqlob.request.InsertRequest;
import dev.kkorolyov.sqlob.request.SelectRequest;
import dev.kkorolyov.sqlob.util.PersistenceHelper;
import dev.kkorolyov.sqlob.util.Where;

import java.lang.reflect.Field;
import java.sql.ResultSet;

/**
 * A {@link Column} which references another table to retrieve its value.
 */
public class ReferencingColumn extends Column<Object> {
	private final Class<?> referencedType;
	private final String referencedName;

	/**
	 * Constructs a new referencing column.
	 * @param f associated field
	 */
	public ReferencingColumn(Field f) {
		super(f, ID_COLUMN.getSqlType(), null);
		this.referencedType = f.getType();
		this.referencedName = PersistenceHelper.getName(referencedType);
	}

	@Override
	public Where contributeToWhere(Where where, ExecutionContext context) {
		where.setResolvedValue(getName(), value ->
				value != null
						? new SelectRequest<>(value)
						.execute(context.getConnection())
						.getId().orElse(null)
						: null);

		return where;
	}

	@Override
	public Object getValue(Object instance, ExecutionContext context) {
		Object value = super.getValue(instance, context);

		return new SelectRequest<>(value)
				.execute(context)
				.asOptional()
				.orElseGet(() -> new InsertRequest<>(value).execute(context))
				.getId();
	}
	@Override
	public Object getValue(ResultSet rs, ExecutionContext context) {
		return new SelectRequest<>(getType(), ID_COLUMN.getValue(rs, context))
				.execute(context)
				.getObject().orElse(null);
	}

	/** @return associated referenced type */
	public Class<?> getReferencedType() {
		return referencedType;
	}
	/** @return referenced table name */
	public String getReferencedName() {
		return referencedName;
	}
}
