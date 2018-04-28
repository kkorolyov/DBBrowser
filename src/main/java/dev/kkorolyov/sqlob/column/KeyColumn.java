package dev.kkorolyov.sqlob.column;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.type.factory.SqlobTypeFactory;

import java.util.UUID;

/**
 * A {@link Column} with value being a primary or foreign key.
 */
public abstract class KeyColumn extends Column<UUID> {
	/** Column corresponding to the default primary key of all persisted types */
	public static final KeyColumn ID = primary("id");

	private KeyColumn(String name) {
		super(name, SqlobTypeFactory.get(UUID.class));
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
			public String getSql(ExecutionContext context) {
				return super.getSql(context)
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
			public String getSql(ExecutionContext context) {
				return super.getSql(context)
						+ ", FOREIGN KEY (" + getName() + ")"
						+ " REFERENCES " + referencedName + "(" + KeyColumn.ID.getName() + ")"
						+ " ON DELETE SET NULL";
			}
		};
	}
}
