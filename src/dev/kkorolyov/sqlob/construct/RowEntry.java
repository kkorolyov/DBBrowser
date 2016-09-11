package dev.kkorolyov.sqlob.construct;

/**
 * A representation of a single entry in a SQL database table row.
 * Composed of a column and a value.
 * The value's type must match the column's type.
 */
public class RowEntry {
	private static final String SQL_VALUE = "=?",
															SQL_VALUE_NULL = " IS NULL";
	
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
	
	/** @return the representation of this entry in a SQL statement */
	public String getSql() {
		return value != null ? column.getName() + SQL_VALUE : column.getName() + SQL_VALUE_NULL;
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
	
	@Override
	public int hashCode() {
		int result = 1, prime = 31;
		
		result = result * prime + column.hashCode();
		result = result * prime + value.hashCode();
		
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (!(obj instanceof RowEntry))
			return false;
		
		RowEntry o = (RowEntry) obj;
		if (column == null) {
			if (o.column != null)
				return false;
		} else if (!column.equals(o.column))
			return false;
		if (value == null) {
			if (o.value != null)
				return false;
		} else if (!value.equals(o.value))
			return false;
		
		return true;
	}
}
