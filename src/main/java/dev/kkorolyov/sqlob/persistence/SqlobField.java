package dev.kkorolyov.sqlob.persistence;

import static dev.kkorolyov.sqlob.service.Constants.ID_NAME;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.UUID;

import dev.kkorolyov.sqlob.annotation.Column;
import dev.kkorolyov.sqlob.utility.Extractor;

/**
 * Manages persistence at the field/column level.
 */
final class SqlobField {
	private static final Extractor DEFAULT_EXTRACTOR = ResultSet::getString;

	final String name;
	private final String type;
	private final int typeCode;
	private final Field field;
	private final Extractor extractor;
	private final SqlobClass<?> reference;
	
	SqlobField(Field field, Extractor extractor, String type, SqlobClass<?> reference) {
		field.setAccessible(true);

		this.field = field;
		this.extractor = (extractor == null) ? DEFAULT_EXTRACTOR : extractor;
		this.type = type;
		this.reference = reference;
		
		Column override = field.getAnnotation(Column.class);
		name = (override == null || override.value().length() <= 0) ? field.getName() : override.value();
		
		typeCode = JDBCType.valueOf(type.split("[\\s(]")[0]).getVendorTypeNumber();
	}

	/** @return value of this field on {@code obj} */
	Object get(Object obj) {
		try {
			return field.get(obj);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("This should never happen");
		}
	}

	Class<?> getType() {
		return field.getType();
	}

	String getCreateSnippet() {
		String init = name + " " + type;
		
		if (isReference()) init += ", FOREIGN KEY (" + name + ") REFERENCES " + reference.name + "(" + ID_NAME + ")";
		
		return init;
	}

	/**
	 * Populates the value of this field in {@code instance} using the appropriate value from {@code rs}.
	 * @param instance instance to populate
	 * @param rs result set to extract from
	 * @param conn connection to utilize for reference mapping
	 */
	void populateInstance(Object instance, ResultSet rs, Connection conn) throws SQLException {
		Object value = extractor.execute(rs, name);
		if (isReference() && value != null) value = reference.get(UUID.fromString((String) value), conn);

		try {
			field.set(instance, value);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("This should never happen");
		}
	}

	/**
	 * Populates a SQL statement using the value of this field in {@code instance}.
	 * @param s statement to populate
	 * @param index parameter index to write to
	 * @param instance instance to retrieve value from
	 * @param conn connection to utilize for reference mapping
	 */
	void populateStatement(PreparedStatement s, int index, Object instance, Connection conn) throws SQLException {
		try {
			s.setObject(index, transform(field.get(instance), conn), typeCode);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("This should never happen");
		}
	}
	Object transform(Object o, Connection conn) throws SQLException {	// Transforms to UUID String if reference TODO privatize
		if (o != null && isReference()) {
			UUID id = reference.getId(o, conn);
			return (id == null ? reference.put(o, conn) : id).toString();	// Put if missing reference
		}
		return o;
	}

	private boolean isReference() {
		return reference != null;
	}
}
