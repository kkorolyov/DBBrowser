package dev.kkorolyov.ezdb.column;

/**
 * A representation of a column in a SQL database.
 * Composed of a name and a type.
 */
public class Column {
	private String name;
	private SQLType type;
	
	/**
	 * Creates a column of the specified name and type.
	 * @param name column name
	 * @param type column type
	 */
	public Column(String name, SQLType type) {
		this.name = name;
		this.type = type;
	}
	
	/** @return column name */
	public String getName() {
		return name;
	}
	/** @return column type */
	public SQLType getType() {
		return type;
	}
}
