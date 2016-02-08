package dev.kkorolyov.ezdb.column;

/**
 * A representation of a SQL datatype.
 * Composed of a typecode and String representation of the type.
 */
public enum SQLType {
	/** The 'boolean' equivalent. */
	BOOLEAN("BOOLEAN", java.sql.Types.BOOLEAN, Boolean.class),
	
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
	
	private SQLType(String typeName, int typeCode, Class<? extends Object> typeClass) {
		this.typeName = typeName;
		this.typeCode = typeCode;
		this.typeClass = typeClass;
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
