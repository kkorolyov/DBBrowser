package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Field;
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
public class PersistedClass {
	private static final LoggerInterface log = Logger.getLogger(PersistedClass.class.getName());
	private static final boolean LOGGING_ENABLED = !(log instanceof dev.kkorolyov.sqlob.logging.LoggerStub);
	private static final Map<Class<?>, PersistedClass> instances = new HashMap<>();
	
	private final Class<?> c;
	private final String 	name,
												init;
	private final List<PersistedField> fields = new LinkedList<>();
	
	static PersistedClass getInstance(Class<?> c, TypeMap typeMap) {
		PersistedClass pc = null;
		
		if ((pc = instances.get(c)) == null) {
			pc = new PersistedClass(c, typeMap);
			instances.put(c, pc);
			
			if (LOGGING_ENABLED)
				log.info("Cached new " + PersistedClass.class.getName() + ": " + pc);
		}
		return pc;
	}
	private PersistedClass(Class<?> c, TypeMap typeMap) {
		this.c = c;
		
		Table override = this.c.getAnnotation(Table.class);
		name = ((override == null || override.value().length() <= 0) ? this.c.getSimpleName() : override.value());
		
		buildFields(typeMap);

		init = buildDefaultInit();
	}
	private void buildFields(TypeMap typeMap) {
		for (Field field : c.getDeclaredFields()) {
			if (field.getAnnotation(Transient.class) == null) {	// Not transient
				field.setAccessible(true);	// TODO Un-permanentize this
				fields.add(new PersistedField(field, typeMap));
			}
		}
	}
	private String buildDefaultInit() {
		StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(name).append(" (").append(Session.ID_NAME).append(" ").append(Session.ID_TYPE).append(" PRIMARY KEY");	// Use type 4 UUID
		
		for (PersistedField field : fields) {
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
	/** @return table initialization statement */
	public String getInit() {
		return init;
	}
	
	/** @return table fields */
	public Iterable<PersistedField> getFields() {
		return fields;
	}
}
