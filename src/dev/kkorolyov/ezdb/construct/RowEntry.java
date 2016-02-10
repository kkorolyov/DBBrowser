package dev.kkorolyov.ezdb.construct;

import dev.kkorolyov.ezdb.exceptions.MismatchedTypeException;

/**
 * A representation of a single entry in a SQL database table row.
 * Composed of a column and a value.
 * The value's type must match the column's type.
 */
public class RowEntry {
	private Column column;
	private Object value;
	
	/**
	 * Constructs a new entry of the specified column and value.
	 * @param column imposes constraints on possible value types
	 * @param value type must match column's type
	 * @throws MismatchedTypeException if the value's type does not match the column's type
	 */
	public RowEntry(Column column, Object value) throws MismatchedTypeException {
		this.column = column;
		setValue(value);
	}
	
	/** @return column */
	public Column getColumn() {
		return column;
	}
	/** @return value */
	public Object getValue() {
		return value;
	}
	
	private void setValue(Object value) throws MismatchedTypeException {
		if (value.getClass() != column.getType().getTypeClass())
			throw new MismatchedTypeException(column.getType().getTypeClass(), value.getClass());
		
		this.value = value;
	}
}
