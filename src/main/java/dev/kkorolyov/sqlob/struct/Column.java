package dev.kkorolyov.sqlob.struct;

import java.util.Objects;

/**
 * Simple data structure representing a SQL column.
 */
public class Column {
	private final String name;
	private final String sql;

	/**
	 * Constructs a new column.
	 * @param name column name
	 * @param sql column description SQL
	 */
	public Column(String name, String sql) {
		this.name = name;
		this.sql = sql;
	}

	/** @return column name */
	public String getName() {
		return name;
	}
	/** @return column description SQL */
	public String getSql() {
		return sql;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Column column = (Column) o;
		return Objects.equals(name, column.name) &&
				Objects.equals(sql, column.sql);
	}
	@Override
	public int hashCode() {
		return Objects.hash(name, sql);
	}

	@Override
	public String toString() {
		return "Column{" +
				"name='" + name + '\'' +
				", sql='" + sql + '\'' +
				'}';
	}
}
