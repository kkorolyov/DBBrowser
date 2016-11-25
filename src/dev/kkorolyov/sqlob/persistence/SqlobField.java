package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.UUID;

import dev.kkorolyov.sqlob.annotation.Column;

final class SqlobField {
	final Field field;
	final String 	name,
								type;
	final int typeCode;
	final SqlobClass<?> reference;
	
	SqlobField(Field field, String type, SqlobClass<?> reference) {
		this.field = field;
		this.type = type;
		this.reference = reference;
		
		Column override = this.field.getAnnotation(Column.class);
		name = (override == null || override.value().length() <= 0) ? this.field.getName() : override.value();
		
		typeCode = JDBCType.valueOf(type.split("[\\s(]")[0]).getVendorTypeNumber();
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
	
	boolean isReference() {
		return reference != null;
	}
}
