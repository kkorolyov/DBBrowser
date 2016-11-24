package dev.kkorolyov.sqlob.construct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.kkorolyov.sqlob.annotation.Table;
import dev.kkorolyov.sqlob.persistence.NonPersistableException;

/**
 * A class persisted as a SQL table.
 */
public class SqlobClass<T> {
	private final Class<T> c;
	private final String name;
	private final List<SqlobField> fields;
	
	SqlobClass(Class<T> c, List<SqlobField> fields) {
		this.c = c;
		this.fields = fields;
		
		Table override = this.c.getAnnotation(Table.class);
		name = (override == null || override.value().length() <= 0) ? this.c.getSimpleName() : override.value();
	}
	
	/**
	 * Returns a mapping of an instance's persisted fields to their values.
	 * @param instance instance of the Java class wrapped by this object
	 * @return mapping of SqlobFields to their values in {@code instance}
	 */
	public Map<SqlobField, Object> getValues(T instance) {
		Map<SqlobField, Object> values = new HashMap<>();
		
		for (SqlobField field : fields) {
			try {
				values.put(field, field.getType().get(instance));
			} catch (IllegalAccessException e) {
				throw new NonPersistableException(field.getType().getName() + " is inaccessible");
			}
		}
		return values;
	}
	
	/** @return persisted class */
	public Class<T> getType() {
		return c;
	}
	
	/** @return table name */
	public String getName() {
		return name;
	}
	
	/** @return persisted fields */
	public Iterable<SqlobField> getFields() {
		return fields;
	}
	
	@Override
	public String toString() {
		return SqlobClass.class.getSimpleName() + "(" + c.getName() + "->" + name + ")";
	}
}
