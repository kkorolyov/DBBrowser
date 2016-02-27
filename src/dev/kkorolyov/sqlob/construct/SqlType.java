package dev.kkorolyov.sqlob.construct;

/**
 * A representation of a SQL datatype.
 * Composed of a typecode and String representation of the type.
 */
public enum SqlType {
	/** The 'boolean' equivalent. */
	BOOLEAN("BOOLEAN", java.sql.Types.BIT, Boolean.class),	// Boolean is a bit in postgres
	
	/** The 'short' equivalent. */
	SMALLINT("SMALLINT", java.sql.Types.SMALLINT, Short.class),
	/** The 'int' equivalent. */
	INTEGER("INTEGER", java.sql.Types.INTEGER, Integer.class),
	/** The 'long' equivalent. */
	BIGINT("BIGINT", java.sql.Types.BIGINT, Long.class),
	/** The 'float' equivalent. */
	REAL("REAL", java.sql.Types.REAL, Float.class),
	/** The 'double' equivalent. */
	DOUBLE("DOUBLE PRECISION", java.sql.Types.DOUBLE, Double.class),
	
	/** The 'char' equivalent. */
	CHAR("CHAR", java.sql.Types.CHAR, Character.class),
	/** The 'String' equivalent. */
	VARCHAR("VARCHAR", java.sql.Types.VARCHAR, String.class);
	
	private String typeName;
	private int typeCode;
	private Class<? extends Object> typeClass;
	
	private SqlType(String typeName, int typeCode, Class<? extends Object> typeClass) {
		this.typeName = typeName;
		this.typeCode = typeCode;
		this.typeClass = typeClass;
	}
	
	/**
	 * Returns the {@code SqlType} for a specified type code.
	 * @param typeCode type code from {@link java.sql.Types}
	 * @return appropriate {@code SqlType} or {@code null} if not found
	 */
	public static SqlType get(int typeCode) {
		for (SqlType type : SqlType.values()) {
			if (type.getTypeCode() == typeCode)
				return type;
		}
		return null;
	}
	
	/** @return {@code String} representation of the type */
	public String getTypeName() {
		return typeName;
	}
	/** @return respective type code from {@link java.sql.Types} */
	public int getTypeCode() {
		return typeCode;
	}
	/** @return respective Java class type */
	public Class<? extends Object> getTypeClass() {
		return typeClass;
	}
}
