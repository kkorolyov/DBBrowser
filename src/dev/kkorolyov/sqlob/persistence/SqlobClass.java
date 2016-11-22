package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dev.kkorolyov.sqlob.annotation.Table;
import dev.kkorolyov.sqlob.annotation.Transient;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.logging.LoggerInterface;

/**
 * A class persisted as a SQL table.
 */
public class SqlobClass {
	private static final LoggerInterface log = Logger.getLogger(SqlobClass.class.getName());
	private static final boolean LOGGING_ENABLED = !(log instanceof dev.kkorolyov.sqlob.logging.LoggerStub);
	
	private final Class<?> c;
	private final String name;
	private final List<SqlobField> fields = new LinkedList<>();
	private final Session session;
	
	SqlobClass(Class<?> c, Session session) throws SQLException {
		this.c = c;
		this.session = session;
		
		Table override = this.c.getAnnotation(Table.class);
		name = ((override == null || override.value().length() <= 0) ? this.c.getSimpleName() : override.value());
		
		buildFields();
	}
	private void buildFields() throws SQLException {
		for (Field field : c.getDeclaredFields()) {
			if (field.getAnnotation(Transient.class) == null) {	// Not transient
				field.setAccessible(true);	// TODO Un-permanentize this
				fields.add(new SqlobField(field, this));
			}
		}
	}
	
	public Map<SqlobField, Object> getValues(Object o) {
		if (o.getClass() != c)
			throw new IllegalArgumentException("Not an instance of the class wrapped by: " + this);
		
		Map<SqlobField, Object> values = new HashMap<>();
		
		for (SqlobField field : fields) {
			try {
				values.put(field, field.getType().get(o));
			} catch (IllegalAccessException e) {
				throw new NonPersistableException(field.getType().getName() + " is inaccessible");
			}
		}
		return values;
	}
	
	/** @return persisted class */
	public Class<?> getType() {
		return c;
	}
	
	/** @return table name */
	public String getName() {
		return name;
	}
	
	/** @return table fields */
	public Iterable<SqlobField> getFields() {
		return fields;
	}
	
	/** @return this SqlobClass's parent session */
	public Session getSession() {
		return session;
	}
	
	/** @return type map used by this {@code SqlobClass} for mapping Java classes to SQL types */
	public Map<Class<?>, String> getTypeMap() {
		return session.getTypeMap();
	}
	Map<Class<?>, String> getRawTypeMap() {	// For overhead reduction
		return session.getRawTypeMap();
	}
	
	@Override
	public String toString() {
		return SqlobClass.class.getSimpleName() + "(" + c.getName() + "->" + name + ")";
	}
}
