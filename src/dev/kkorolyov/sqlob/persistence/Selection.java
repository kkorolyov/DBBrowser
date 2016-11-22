package dev.kkorolyov.sqlob.persistence;

/**
 * A selection of persisted attributes.
 */
public class Selection {
	private StringBuilder sql;
	
	/**
	 * Constructs a new selection with no attributes.
	 */
	public Selection() {
		this(null);
	}
	/**
	 * Constructs a new selection with a single attribute.
	 * @param attribute attribute to select
	 */
	public Selection(String attribute) {
		sql = new StringBuilder(attribute);
	}
	
	/**
	 * Appends an additional selection attribute.
	 * @param attribute attribute to append
	 * @throws IllegalArgumentException if {@code attribute} is null
	 */
	public void append(String attribute) {
		if (attribute == null)
			throw new IllegalArgumentException("May not select null");
		
		if (sql.length() > 0)	// At least one attribute already exists
			sql.append(",");
		
		sql.append(attribute);
	}
	
	/** @return SQL phrase representing this selection */
	@Override
	public String toString() {
		return sql.toString();
	}
}
