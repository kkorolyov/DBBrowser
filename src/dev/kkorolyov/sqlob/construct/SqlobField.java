package dev.kkorolyov.sqlob.construct;

import java.lang.reflect.Field;
import java.sql.JDBCType;

import dev.kkorolyov.sqlob.annotation.Column;

/**
 * A field persisted as a column of a SQL table.
 */
public class SqlobField {
	private final Field field;
	private final String 	name,
												type;
	private final int typeCode;
	private final SqlobClass<?> reference;
	
	SqlobField(Field field, String defaultType, SqlobClass<?> reference) {
		this.field = field;
		this.reference = reference;
		
		Column override = this.field.getAnnotation(Column.class);
		name = (override == null || override.value().length() <= 0) ? this.field.getName() : override.value();
		type = (override == null || override.type().length() <= 0 || isReference()) ? defaultType : override.type();	// Apply override only to non-references
		
		typeCode = JDBCType.valueOf(type).getVendorTypeNumber();
	}
	
	/** @return persisted field */
	public Field getType() {
		return field;
	}
	
	/** @return column name */
	public String getName() {
		return name;
	}
	
	/** @return column type */
	public String getSqlType() {
		return type;
	}
	/** @return JDBC type code for this field's type */
	public int getSqlTypeCode() {
		return typeCode;
	}
	
	/** @return class referenced by this field, or {@code null} if it does not reference a class */
	public SqlobClass<?> getReference() {
		return reference;
	}
	/** @return {@code true} if this field references a separate class/table */
	public boolean isReference() {
		return getReference() != null;
	}
}
