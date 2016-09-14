package dev.kkorolyov.sqlob.construct;

/**
 * A representation of a column in a SQL database.
 * Composed of a name and a type.
 */
public class Column {
	private static final String NAME_TYPE_DELIMITER = " ";
	
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
	
	/** @return the representation of this column in a SQL statement */
	public String getSql() {
		return name + NAME_TYPE_DELIMITER + type.getTypeName();
	}
	
	/** @return column name */
	public String getName() {
		return name;
	}
	/** @return column type */
	public SqlType getType() {
		return type;
	}
	
	@Override
	public int hashCode() {
		int result = 1, prime = 31;
		
		result = result * prime + ((name != null) ? name.toUpperCase().hashCode() : 0);
		result = result * prime + ((type != null) ? type.hashCode() : 0);
		
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (!(obj instanceof Column))
			return false;
		
		Column o = (Column) obj;
		if (name == null) {
			if (o.name != null)
				return false;
		} else if (!name.equalsIgnoreCase(o.name))
			return false;
		if (type == null) {
			if (o.type != null)
				return false;
		} else if (!type.equals(o.type))
			return false;
		
		return true;
	}
}
