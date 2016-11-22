package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Field;
import java.sql.SQLException;
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
	
	SqlobClass(Class<?> c, Session session) throws SQLException {
		this.c = c;
		this.session = session;
		
		Table override = this.c.getAnnotation(Table.class);
		name = ((override == null || override.value().length() <= 0) ? this.c.getSimpleName() : override.value());
		
		buildFields();

		init = buildDefaultInit();
	}
	private void buildFields() throws SQLException {
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
			log.debug("Built table init statement for " + this + ": " + result); 
		return result;
	}
	
	/** @return persisted class */
	public Class<?> getType() {
		return c;
	}
	
	/** @return table name */
	public String getName() {
		return name;
	}
	/** @return SQL statements to initialize this SqlobClass and SqlobClasses it references, in valid order */
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
	
	/**
	 * @param condition condition to match, {@code null} is no constraining condition
	 * @return SQL statement to retrieve all instances of this SqlobClass matching {@code condition}
	 */
	public String getGet(Condition condition) {
		String result = "SELECT * FROM " + name;
		
		if (condition != null)
			result += " WHERE " + condition;
		
		if (LOGGING_ENABLED)
			log.debug("Built GET for " + this + ": " + result);
		return result;
	}
	/** @return	SQL statement to add an instance of this SqlobClass */
	public String getPut() {
		StringBuilder builder = new StringBuilder("INSERT INTO ").append(name).append("(").append(Session.ID_NAME).append(","),
									values = new StringBuilder("VALUES (?,");

		for (SqlobField sqlField : fields)  {
			builder.append(sqlField.getName()).append(",");
			values.append("?,");
		}
		values.replace(values.length() - 1, values.length(), ")");
		builder.replace(builder.length() - 1, builder.length(), ") ").append(values.toString());
		
		String result = builder.toString();
		
		if (LOGGING_ENABLED)
			log.debug("Built PUT for " + this + ": " + result);
		return result;
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
		return SqlobClass.class.getSimpleName() + "(" + c + "->" + name + ")";
	}
}
