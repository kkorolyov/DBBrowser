package dev.kkorolyov.ezdb.column;

public class RowEntry {	// TODO Move Column value code to here
	private Column column;
	private Object value;
	
	public RowEntry(Column column, Object value) {
		this.column = column;
		this.value = value;
	}
	
	public Column getColumn() {
		return column;
	}
	public Object getValue() {
		return value;
	}
}
