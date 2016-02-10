package dev.kkorolyov.ezdb.construct;

/**
 * A representation of a column in a SQL database.
 * Composed of a name and a type.
 */
public class Column {
	private String name;
	private SqlType type;
	
	/**
	 * Creates a column of the specified name and type.
	 * @param name column name
	 * @param type column type
	 */
	public Column(String name, SqlType type) {
		this.name = name;
		this.type = type;
	}
	
	/** @return column name */
	public String getName() {
		return name;
	}
	/** @return column type */
	public SqlType getType() {
		return type;
	}
}
