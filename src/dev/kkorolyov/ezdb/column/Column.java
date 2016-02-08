package dev.kkorolyov.ezdb.column;

/**
 * Describes the name, type, and (optional) value of a column in a SQL database table row.
 */
// TODO Loose-couple types
// TODO Use factory for different database table column types
public class Column {
	private String name;
	private Type type;
	private Object value;
	
	/**
	 * Creates a column of the specified name and type.
	 * @see #Column(String, Type, Object)
	 */
	public Column(String name, Type type) {
		this(name, type, null);
	}
	/**
	 * Creates a column of the specified name, type, and value.
	 * @param name column name
	 * @param type column type from {@code PGColumn.Type}
	 * @param value object of a type matching the {@code Type} specified
	 */
	public Column(String name, Type type, Object value) {
		this.name = name;
		this.type = type;
		this.value = value;
	}
	
	/** @return column name */
	public String getName() {
		return name;
	}
	
	/** @return column type code from {@code java.sql.Types} */
	public int getType() {
		return type.getTypeCode();
	}
	/** @return column type common name */
	public String getTypeName() {
		return type.getTypeName();
	}
	
	/** @return column value */
	public Object getValue() {
		return value;
	}
	
	/**
	 * Sets the column's value.
	 * Necessary in order to use this column as a criterion in a statement.
	 * @param value value to set
	 */
	public void setValue(Object value) {	// TODO Type check
		this.value = value;
	}
	
	/**
	 * Used to specify a database column's datatype.
	 */
	public enum Type {
		/** The 'boolean' equivalent. */
		BOOLEAN(java.sql.Types.BOOLEAN, "BOOLEAN"),
		
		/** The 'short' equivalent. */
		SMALLINT(java.sql.Types.SMALLINT, "SMALLINT"),
		/** The 'int' equivalent. */
		INTEGER(java.sql.Types.INTEGER, "INTEGER"),
		/** The 'long' equivalent. */
		BIGINT(java.sql.Types.BIGINT, "BIGINT"),
		/** The 'float' equivalent. */
		REAL(java.sql.Types.REAL, "REAL"),
		/** The 'double' equivalent. */
		DOUBLE(java.sql.Types.DOUBLE, "DOUBLE PRECISION"),
		
		/** The 'char' equivalent. */
		CHAR(java.sql.Types.CHAR, "CHAR"),
		/** The 'String' equivalent. */
		VARCHAR(java.sql.Types.VARCHAR, "VARCHAR");
		
		private int typeCode;
		private String typeName;
		
		private Type(int typeCode, String typeName) {
			this.typeCode = typeCode;
			this.typeName = typeName;
		}
		
		int getTypeCode() {
			return typeCode;
		}
		String getTypeName() {
			return typeName;
		}
	}
}
