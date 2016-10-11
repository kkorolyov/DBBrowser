package dev.kkorolyov.sqlob.construct;

/**
 * A representation of a single entry in a SQL database table row.
 * Composed of a column, operator, and a value.
 * The value's type must match the column's type.
 */
public class RowEntry {
	private Column column;
	private Operator operator;
	private Object value;
	
	/**
	 * Constructs a new entry with a default operator of {@code Operator.EQUALS}.
	 * @see #RowEntry(Column, Operator, Object)
	 */
	public RowEntry(Column column, Object value) {
		this(column, Operator.EQUALS, value);
	}
	/**
	 * Constructs a new entry of the specified column, operator, and value.
	 * @param column entry's column
	 * @param operator entry's comparison operator
	 * @param value entry's value, may be another {@link Column}
	 * @throws IllegalArgumentException if the value's type does not match the column's type
	 */
	public RowEntry(Column column, Operator operator, Object value) {
		this.column = column;
		this.operator = operator;
		setValue(value);
	}
	
	/** @return the representation of this entry in a SQL statement */
	public String getSql() {
		return column.getSql() + operator.getSql();
	}
	
	/** @return column */
	public Column getColumn() {
		return column;
	}
	/** @return operator */
	public Operator getOperator() {
		return operator;
	}
	/** @return value */
	public Object getValue() {
		return value;
	}
	
	private void setValue(Object value) {
		if (value != null && value.getClass() != Column.class && value.getClass() != column.getType().getTypeClass())
			throw new IllegalArgumentException("Column type: " + column.getType().getTypeClass() + " does not match value type: " + value.getClass());
		
		this.value = value;
	}
	
	@Override
	public int hashCode() {
		int result = 1, prime = 31;
		
		result = result * prime + ((column != null) ? column.hashCode() : 0);
		result = result * prime + ((operator != null) ? operator.hashCode() : 0);
		result = result * prime + ((value != null) ? value.hashCode() : 0);
		
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
	
	/**
	 * An operator of a {@code RowEntry}.
	 */
	@SuppressWarnings("javadoc")
	public static enum Operator {
		NULL(" IS NULL"),
		EQUALS("=?"),
		GREATER(">?"),
		GREATER_EQUALS(">=?"),
		LESS("<?"),
		LESS_EQUALS("<=?"),
		LIKE("LIKE %?%");
		
		private String sql;
		
		private Operator(String sql) {
			this.sql = sql;
		}
		
		/** @return SQL representation or this operator */
		public String getSql() {
			return sql;
		}
	}
}
