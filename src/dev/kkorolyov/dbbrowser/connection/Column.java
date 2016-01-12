package dev.kkorolyov.dbbrowser.connection;

/**
 * Describes the name and type of a database column.
 */
public class Column {
	/** Java type code identifying the SQL type {@code BOOLEAN}. */
	public static final int BOOLEAN = java.sql.Types.BOOLEAN;
	/** Java type code identifying the SQL type {@code CHAR}. */
	public static final int CHAR = java.sql.Types.CHAR;
	/** Java type code identifying the SQL type {@code DOUBLE}. */
	public static final int DOUBLE = java.sql.Types.DOUBLE;
	/** Java type code identifying the SQL type {@code FLOAT}. */
	public static final int FLOAT = java.sql.Types.FLOAT;
	/** Java type code identifying the SQL type {@code INTEGER}. */
	public static final int INTEGER = java.sql.Types.INTEGER;
	/** Java type code identifying the SQL type {@code VARCHAR}. */
	public static final int VARCHAR = java.sql.Types.VARCHAR;
	
	private String name;
	private int type;
	
	public Column(String name, int type) {
		this.name = name;
		this.type = type;
	}
	
	/**
	 * @return column name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return column type from {@code java.sql.Types}
	 */
	public int getType() {
		return type;
	}
}
