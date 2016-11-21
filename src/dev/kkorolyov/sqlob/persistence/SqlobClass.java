package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Field;
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
	private final String 	name,
												init;
	private final List<SqlobField> fields = new LinkedList<>();
	private final Session session;
	
	SqlobClass(Class<?> c, Session session) {
		this.c = c;
		this.session = session;
		
		Table override = this.c.getAnnotation(Table.class);
		name = ((override == null || override.value().length() <= 0) ? this.c.getSimpleName() : override.value());
		
		buildFields();

		init = buildDefaultInit();
	}
	private void buildFields() {
		for (Field field : c.getDeclaredFields()) {
			if (field.getAnnotation(Transient.class) == null) {	// Not transient
				field.setAccessible(true);	// TODO Un-permanentize this
				fields.add(new SqlobField(field, this));
			}
		}
	}
	private String buildDefaultInit() {
		StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(name).append(" (").append(Session.ID_NAME).append(" ").append(Session.ID_TYPE).append(" PRIMARY KEY");	// Use type 4 UUID
		
		for (SqlobField field : fields) {
			builder.append(", ").append(field.getName()).append(" ").append(field.getType());
			
			if (field.isReference())
				builder.append(", FOREIGN KEY (").append(field.getName()).append(") REFERENCES ").append(field.getReferencedClass().getName()).append(" (").append(Session.ID_NAME).append(")");
		}
		String result = builder.append(")").toString();
		
		if (LOGGING_ENABLED)
			log.debug("Built table init statement for " + name + ": " + result); 
		return result;
	}
	
	/** @return persisted class */
	public Class<?> getClazz() {
		return c;
	}
	
	/** @return table name */
	public String getName() {
		return name;
	}
	/** @return table initialization statement of this table and its referenced tables, in appropriate order */
	public Iterable<String> getInits() {
		List<String> inits = new LinkedList<>();
		
		for (SqlobField field : fields) {	// Add referenced inits first
			if (field.isReference()) {
				for (String init : field.getReferencedClass().getInits())
					inits.add(init);
			}
		}
		inits.add(init);	// Add this init last
		
		return inits;
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
}
