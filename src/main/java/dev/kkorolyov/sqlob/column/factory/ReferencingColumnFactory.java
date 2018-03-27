package dev.kkorolyov.sqlob.column.factory;

import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.column.ReferencingColumn;

import java.lang.reflect.Field;

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
	public Column<?> get(Field f) {
		return new ReferencingColumn(f);
	}
}
