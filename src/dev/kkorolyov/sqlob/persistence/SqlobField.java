package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import dev.kkorolyov.sqlob.annotation.Column;

final class SqlobField {
	final Field field;
	final Extractor extractor;
	final String 	name,
								type;
	final int typeCode;
	final SqlobClass<?> reference;
	
	SqlobField(Field field, Extractor extractor, String type, SqlobClass<?> reference) {
		this.field = field;
		this.extractor = extractor;
		this.type = type;
		this.reference = reference;
		
		Column override = this.field.getAnnotation(Column.class);
		name = (override == null || override.value().length() <= 0) ? this.field.getName() : override.value();
		
		typeCode = JDBCType.valueOf(type.split("[\\s(]")[0]).getVendorTypeNumber();
		
		this.field.setAccessible(true);
	}
	
	String getInit(String idName) {
		String init = name + " " + type;
		
		if (isReference())
			init += ", FOREIGN KEY (" + name + ") REFERENCES " + reference.name + "(" + idName + ")";
		
		return init;
	}
	
	Object transform(Object o, Connection conn) throws SQLException {	// Transforms to UUID String if reference
		if (isReference()) {
			UUID id = reference.getId(o, conn);
			return id == null ? null : id.toString();
		}
		return o;
	}
	
	void apply(Object instance, ResultSet rs, Connection conn) throws SQLException {
		Object value = (extractor == null ? rs.getObject(name) : extractor.execute(rs, name));
		
		if (isReference() && value != null)
			value = reference.get(UUID.fromString((String) value), conn);
		
		try {
			field.set(instance, value);
		} catch (IllegalAccessException e) {
			throw new NonPersistableException(field + " is inaccessible", e);
		}
	}
	
	boolean isReference() {
		return reference != null;
	}
}
