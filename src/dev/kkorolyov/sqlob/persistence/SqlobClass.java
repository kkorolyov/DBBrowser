package dev.kkorolyov.sqlob.persistence;

import java.sql.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import dev.kkorolyov.sqlob.annotation.Table;
import dev.kkorolyov.sqlob.logging.Logger;

final class SqlobClass<T> {
	private static final Logger log = Logger.getLogger(SqlobClass.class.getName());
	
	final Class<T> c;
	final String name;
	final Map<Class<?>, SqlobField> fields;
	private final String 	idName;
	private final String 	create,
												insert,
												fieldSelection;
	
	SqlobClass(Class<T> c, Map<Class<?>, SqlobField> fields, String idName, String idType) {
		this.c = c;
		this.fields = fields;
		this.idName = idName;
		
		Table override = this.c.getAnnotation(Table.class);
		name = (override == null || override.value().length() <= 0) ? this.c.getSimpleName() : override.value();
		
		StringBuilder createBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(name).append("(").append(idName).append(" ").append(idType).append(" PRIMARY KEY"),
									insertBuilder = new StringBuilder("INSERT INTO ").append(name).append(" (").append(idName),
									valuesBuilder = new StringBuilder("VALUES (?"),
									fieldSelectionBuilder = new StringBuilder();

		for (SqlobField field : fields.values()) {
			createBuilder.append(", ").append(field.getInit(idName));
			
			insertBuilder.append(",").append(field.name);
			valuesBuilder.append(",?");
			
			if (fieldSelectionBuilder.length() > 0)
				fieldSelectionBuilder.append(",");
			fieldSelectionBuilder.append(field.name);
		}
		createBuilder.append(")");
		
		valuesBuilder.append(")");
		insertBuilder.append(") ").append(valuesBuilder);
		
		create = createBuilder.toString();
		insert = insertBuilder.toString();
		fieldSelection = fieldSelectionBuilder.toString();
	}
	
	SqlobClass<T> init(Connection conn) throws SQLException {
		try (Statement s = conn.createStatement()) {
			s.executeUpdate(create);
			log.debug(() -> "Executed statement: " + create); 
		}
		log.debug(() -> "Initialized new SqlobClass: " + this);
		return this;
	}
	
	UUID put(Object instance, Connection conn) throws SQLException {
		UUID id = getId(instance, conn);
		
		if (id == null) {
			id = UUID.randomUUID();
			
			try (PreparedStatement s = conn.prepareStatement(insert)) {
				s.setString(1, id.toString());
				
				int counter = 2;
				for (SqlobField field : fields.values()) {
					try {
						Object value = 	field.field.get(instance),
														transformed = transform(value, conn);
						if (value != null && transformed == null)	// Missing reference
							transformed = field.reference.put(value, conn);
						
						s.setObject(counter++, transformed, field.typeCode);
					} catch (IllegalAccessException e) {
						throw new NonPersistableException(field.field.getName() + " is inaccessible");
					}
				}
				s.executeUpdate();
			}
			UUID finalId = id;
			log.debug(() -> "Saved new " + this + ": " + instance + "->" + finalId);
		}
		return id;
	}
	
	@SuppressWarnings("unchecked")
	UUID getId(Object instance, Connection conn) throws SQLException {
		Condition where = buildEquals((T) instance);
		try (PreparedStatement s = conn.prepareStatement(buildSelect(idName, where))) {
			int counter = 1;
			for (Object value : where.values()) {
				Object transformed = transform(value, conn);
				if (value != null && transformed == null) // Missing reference
					return null;	// Short-circuit
				
				s.setObject(counter++, transformed);
			}
			try (ResultSet rs = s.executeQuery()) {
				return rs.next() ? UUID.fromString(rs.getString(1)) : null;
			}
		}
	}
	T getInstance(UUID id, Connection conn) throws SQLException {
		Set<T> instances = getInstances(new Condition(idName, "=", id.toString()), conn);
		return instances.isEmpty() ? null : instances.iterator().next();
	}
	Set<T> getInstances(Condition where, Connection conn) throws SQLException {
		Set<T> results = new HashSet<>();
		
		try (PreparedStatement s = conn.prepareStatement(buildSelect(fieldSelection, where))) {
			int counter = 1;
			for (Object value : where.values())
				s.setObject(counter++, transform(value, conn));
			
			try (ResultSet rs = s.executeQuery()) {
				while(rs.next()) {
					try {
						T result = c.newInstance();
						
						for (SqlobField field : fields.values()) {
							try {
								Object value = rs.getObject(field.name);
								if (field.isReference() && value != null)
									value = field.reference.getInstance(UUID.fromString((String) value), conn);
								
								field.field.set(result, value);
							} catch (IllegalAccessException e) {
								throw new NonPersistableException(field.field.getName() + " is inaccessible");
							}
						}
						results.add(result);
					} catch (IllegalAccessException | InstantiationException e) {
						throw new NonPersistableException(c.getName() + " does not provide an accessible nullary constructor");
					}
				}
			}
		}
		return results;
	}
	
	private Object transform(Object o, Connection conn) throws SQLException {
		if (o == null)
			return o;
		
		SqlobField field = fields.get(o.getClass());
		return field == null ? o : field.transform(o, conn);
	}
	
	private String buildSelect(String selection, Condition where) {
		String result = "SELECT " + selection + " FROM " + name;
		
		if (where != null)
			result += " WHERE " + where;
		
		return result;
	}
	
	private Condition buildEquals(T instance) {
		Condition result = null;
		
		for (SqlobField field : fields.values()) {
			try {
				String attribute = field.name;
				Object value = field.field.get(instance);
				Condition currentCondition = new Condition(attribute, (value == null ? "IS" : "="), value);
				
				if (result == null)
					result = currentCondition;
				else
					result.and(currentCondition);
			} catch (IllegalAccessException e) {
				throw new NonPersistableException(field.field.getName() + " is inaccessible");
			}
		}
		return result;
	}
	
	@Override
	public String toString() {
		return SqlobClass.class.getSimpleName() + "(" + c.getName() + "->" + name + ")";
	}
}
