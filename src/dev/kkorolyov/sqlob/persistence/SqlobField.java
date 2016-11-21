package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Field;
import java.sql.JDBCType;
import java.sql.SQLException;

import dev.kkorolyov.sqlob.annotation.Column;

/**
 * A field persisted as a column of a SQL table.
 */
public class SqlobField {
	private final Field field;
	private final SqlobClass 	parentClass,
																referencedClass;
	private final String 	name,
												type;
	private final int typeCode;
	
	SqlobField(Field field, SqlobClass parentClass) throws SQLException {
		this.field = field;
		this.parentClass = parentClass;
		
		Column override = this.field.getAnnotation(Column.class);
		name = ((override == null || override.value().length() <= 0) ? this.field.getName() : override.value());
		
		if (override == null || override.type().length() <= 0) {	// No type override
			String mappedType = this.parentClass.getTypeMap().get(this.field.getType());

			if (mappedType == null) {	// Reference
				type = Session.ID_TYPE;
				referencedClass = this.parentClass.getSession().getSqlobClass(this.field.getType());
			} else {
				type = mappedType;
				referencedClass = null;
			}
		} else {	// Type override
			type = override.type();
			referencedClass = null;
		}
		typeCode = JDBCType.valueOf(type.split("[\\s\\(]")[0]).getVendorTypeNumber();
	}
	
	/** @return class field */
	public Field getField() {
		return field;
	}
	
	/** @return column name */
	public String getName() {
		return name;
	}
	/** @return column type */
	public String getType() {
		return type;
	}
	
	/** @return the JDBC type code associated with this column's type */
	public int getTypeCode() {
		return typeCode;
	}
	
	/** @return {@code true} if this column refers to another table */
	public boolean isReference() {
		return referencedClass != null;
	}
	
	/** @return referenced class, or {@code null} if this column is primitive */
	public SqlobClass getReferencedClass() {
		return referencedClass;
	}
}
