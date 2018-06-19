package dev.kkorolyov.sqlob.struct;

import java.util.List;
import java.util.Objects;

/**
 * Simple data structure representing a single SQL table.
 */
public class Table {
	private final String name;
	private final List<Column> columns;

	/**
	 * Constructs a new table.
	 * @param name table name
	 * @param columns ordered table columns
	 */
	public Table(String name, List<Column> columns) {
		this.name = name;
		this.columns = columns;
	}

	/** @return table name */
	public String getName() {
		return name;
	}
	/** @return ordered table columns */
	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Table table = (Table) o;
		return Objects.equals(name, table.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return "Table{" +
				"name='" + name + '\'' +
				", columns=" + columns +
				'}';
	}
}
