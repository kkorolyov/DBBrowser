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
public abstract class KeyColumn extends Column<UUID> {
	/** Column corresponding to the primary key of all persisted types */
	public static final KeyColumn PRIMARY = id();

	private KeyColumn(String name) {
		super(name, "UUID");
	}

	/** @return primary key column of all persisted types */
	public static KeyColumn id() {
		return primary("id");
	}

	/**
	 * @param referencedName parent table name
	 * @return foreign key column referencing {@code referencedName} as a parent table
	 */
	public static KeyColumn parent(String referencedName) {
		return foreign("parent", referencedName);
	}
	/**
	 * @param referencedName child table name
	 * @return foreign key column referencing {@code referencedName} as a child table
	 */
	public static KeyColumn child(String referencedName) {
		return foreign("child", referencedName);
	}

	/**
	 * @param name column name
	 * @return primary key column named {@code name}
	 */
	public static KeyColumn primary(String name) {
		return new KeyColumn(name) {
			@Override
			public String getSql() {
				return super.getSql()
						+ " PRIMARY KEY";
			}
		};
	}
	/**
	 * @param name column name
	 * @param referencedName referenced table name
	 * @return foreign key column named {@code name} and referencing table {@code referencedName}
	 */
	public static KeyColumn foreign(String name, String referencedName) {
		return new KeyColumn(name) {
			@Override
			public String getSql() {
				return super.getSql()
						+ ", FOREIGN KEY (" + getName() + ")"
						+ " REFERENCES " + referencedName + " (" + KeyColumn.PRIMARY.getName() + ")"
						+ " ON DELETE SET NULL";
			}
		};
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
}
