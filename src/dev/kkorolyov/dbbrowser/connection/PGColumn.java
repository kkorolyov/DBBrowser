package dev.kkorolyov.dbbrowser.connection;

/**
 * Describes the name and type of a postgres database column.
 */
// TODO Loose-couple types
public class PGColumn {	
	private String name;
	private Type type;
	
	/**
	 * Creates a column of the specified name and type.
	 * @param name column name
	 * @param type column type from
	 * <ul>
	 * <li> {@code Column.TypeMapping.BOOLEAN}
	 * <li> {@code Column.TypeMapping.CHAR}
	 * <li> {@code Column.TypeMapping.DOUBLE}
	 * <li> {@code Column.TypeMapping.FLOAT}
	 * <li> {@code Column.TypeMapping.INTEGER}
	 * <li> {@code Column.TypeMapping.VARCHAR}
	 * </ul>
	 */
	public PGColumn(String name, Type type) {
		this.name = name;
		this.type = type;
	}
	
	/** @return column name */
	public String getName() {
		return name;
	}
	/** @return column type code from {@code java.sql.Types} */
	public int getType() {
		return type.typeCode;
	}
	/** @return column type common name */
	public String getTypeName() {
		return type.typeName;
	}
	
	/**
	 * Used to specify a database column's datatype.
	 */
	public enum Type {
		BOOLEAN(java.sql.Types.BOOLEAN, "BOOLEAN"),
		CHAR(java.sql.Types.CHAR, "CHAR"),
		DOUBLE(java.sql.Types.DOUBLE, "DOUBLE PRECISION"),
		REAL(java.sql.Types.REAL, "REAL"),
		INTEGER(java.sql.Types.INTEGER, "INTEGER"),
		VARCHAR(java.sql.Types.VARCHAR, "VARCHAR");
		
		private int typeCode;
		private String typeName;
		
		private Type(int typeCode, String typeName) {
			this.typeCode = typeCode;
			this.typeName = typeName;
		}
	}
}
