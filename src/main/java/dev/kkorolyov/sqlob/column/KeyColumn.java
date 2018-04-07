package dev.kkorolyov.sqlob.column;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.util.Where;

import java.sql.ResultSet;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static dev.kkorolyov.sqlob.util.UncheckedSqlException.wrapSqlException;

/**
 * A {@link Column} with value being a primary or foreign key.
 */
public class KeyColumn extends Column<UUID> {
	/** Column corresponding to the primary key of all persisted types */
	public static final KeyColumn PRIMARY = new KeyColumn("id");

	/**
	 * Constructs a new key column.
	 * @param name column name
	 */
	public KeyColumn(String name) {
		super(name, "UUID");
	}

	@Override
	public Where contributeToWhere(Where where, ExecutionContext context) {
		return where.resolve(getName(), UnaryOperator.identity());
	}

	@Override
	public UUID getValue(ResultSet rs, ExecutionContext context) {
		String idString = wrapSqlException(() -> rs.getString(getName()));
		return idString != null ? UUID.fromString(idString) : null;
	}

	@Override
	public String getSql() {
		return super.getSql() + " PRIMARY KEY";
	}
}
