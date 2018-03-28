package dev.kkorolyov.sqlob.column;

import dev.kkorolyov.sqlob.request.ExecutionContext;
import dev.kkorolyov.sqlob.util.Where;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * A {@link Column} with value being a primary or foreign key.
 */
public class KeyColumn extends Column<UUID> {
	/** Column corresponding to the primary key of all persisted types */
	public static KeyColumn PRIMARY = new KeyColumn("id");

	/**
	 * Constructs a new key column.
	 * @param name column name
	 */
	public KeyColumn(String name) {
		super(name, "CHAR(16)");
	}

	@Override
	public Where contributeToWhere(Where where, ExecutionContext context) {
		where.resolve(getName(), value ->
				value instanceof UUID ? value.toString() : value);

		return where;
	}

	@Override
	public UUID getValue(ResultSet rs, ExecutionContext context) throws SQLException {
		String idString = rs.getString(getName());
		return idString != null ? UUID.fromString(idString) : null;
	}

	@Override
	public String getSql() {
		return super.getSql() + " PRIMARY KEY";
	}
}
