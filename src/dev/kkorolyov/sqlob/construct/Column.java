package dev.kkorolyov.sqlob.construct;

/**
 * A single table column in a SQL database.
 */
public class Column {
	private static final String TABLE_NAME_DELIMETER = ".";
	
	private String 	table,
									name;
	private SqlobType type;
	
	/**
	 * Creates a column of the specified table, name and type.
	 * @param table table name
	 * @param name column name
	 * @param type column type
	 */
	public Column(String table, String name, SqlobType type) {
		this.table = table;
		this.name = name;
		this.type = type;
	}
	
	/** @return the representation of this column in a SQL statement */
	public String getSql() {
		return table + TABLE_NAME_DELIMETER + name;
	}
	
	/** @return table name */
	public String getTable() {
		return table;
	}
	/** @return column name */
	public String getName() {
		return name;
	}
	/** @return column type */
	public SqlobType getType() {
		return type;
	}
	
	@Override
	public int hashCode() {
		int result = 1, prime = 31;
		
		result = result * prime + ((table != null) ? table.hashCode() : 0);
		result = result * prime + ((name != null) ? name.hashCode() : 0);
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
		if (table == null) {
			if (o.table != null)
				return false;
		} else if (!table.equals(o.table))
			return false;
		if (name == null) {
			if (o.name != null)
				return false;
		} else if (!name.equals(o.name))
			return false;
		if (type == null) {
			if (o.type != null)
				return false;
		} else if (!type.equals(o.type))
			return false;
		
		return true;
	}
}
