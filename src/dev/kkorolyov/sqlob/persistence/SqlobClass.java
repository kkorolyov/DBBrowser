package dev.kkorolyov.sqlob.persistence;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.kkorolyov.sqlob.annotation.Table;
import dev.kkorolyov.sqlob.logging.Logger;

final class SqlobClass<T> {
	private static final Logger log = Logger.getLogger(SqlobClass.class.getName());
	
	final Class<T> c;
	final String name;
	final List<SqlobField> fields;
	private final String 	idName;
	private final String 	create,
												insert,
												fieldSelection;
	
	SqlobClass(Class<T> c, List<SqlobField> fields, String idName, String idType) {
		this.c = c;
		this.fields = fields;
		this.idName = idName;
		
		Table override = this.c.getAnnotation(Table.class);
		name = (override == null || override.value().length() <= 0) ? this.c.getSimpleName() : override.value();
		
		StringBuilder createBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(name).append("(").append(idName).append(" ").append(idType).append(" PRIMARY KEY"),
									insertBuilder = new StringBuilder("INSERT INTO ").append(name).append(" (").append(idName),
									valuesBuilder = new StringBuilder("VALUES (?"),
									fieldSelectionBuilder = new StringBuilder();

		for (SqlobField field : fields) {
			createBuilder.append(", ").append(field.getInit(idName));
			
			insertBuilder.append(", ").append(field.name);
			valuesBuilder.append(", ?");
			
			if (fieldSelectionBuilder.length() > 0)
				fieldSelectionBuilder.append(", ");
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
		}
		log.debug(() -> "Initialized new SqlobClass: " + this + System.lineSeparator() + "\t" + create);
		return this;
	}
	
	UUID put(Object instance, Connection conn) throws SQLException {
		UUID id = getId(instance, conn);
		
		if (id == null) {
			id = UUID.randomUUID();
			List<Object> parameters = new LinkedList<>();	// For logging
			
			try (PreparedStatement s = conn.prepareStatement(insert)) {
				s.setString(1, id.toString());
				parameters.add(id);	// Logging

				int counter = 2;
				for (SqlobField field : fields) {
					try {
						Object value = 	field.field.get(instance),
														transformed = transform(value, conn);
						if (value != null && transformed == null)	// Missing reference
							transformed = field.reference.put(value, conn);	// Recursive put
						
						s.setObject(counter++, transformed, field.typeCode);
						parameters.add(transformed);	// Logging
					} catch (IllegalAccessException e) {
						throw new NonPersistableException(field.field + " is inaccessible");
					}
				}
				s.executeUpdate();
			}
			UUID finalId = id;
			log.debug(() -> "Saved new " + this + ": " + instance + "->" + finalId + System.lineSeparator() + "\t" + applyStatement(insert, parameters));
		}
		return id;
	}
	
	@SuppressWarnings("unchecked")
	UUID getId(Object instance, Connection conn) throws SQLException {
		Condition where = buildEquals((T) instance);
		String select = buildSelect(idName, where);
		
		try (PreparedStatement s = conn.prepareStatement(select)) {
			int counter = 1;
			for (Object value : where.values()) {
				if (!shortCircuitSet(s, counter++, value, conn))	// Missing reference
					return null;	// Short-circuit
			}
			try (ResultSet rs = s.executeQuery()) {
				UUID result = rs.next() ? UUID.fromString(rs.getString(1)) : null;
				
				log.debug(() -> (result == null ? "Failed to find " : "Found ") + "ID of " + this + " instance: " + instance + (result == null ? "" : "->" + result) + System.lineSeparator() + "\t" + applyStatement(select, (where == null ? null : where.values())));
				return result;
			}
		}
	}
	
	T get(UUID id, Connection conn) throws SQLException {
		Set<T> instances = get(new Condition(idName, "=", id.toString()), conn);
		T result = instances.isEmpty() ? null : instances.iterator().next();
		
		log.debug(() -> (result == null ? "Failed to find " : "Found ") + "instance of " + this + ": " + id + "->" + result);
		return result;
	}
	Set<T> get(Condition where, Connection conn) throws SQLException {
		Set<T> results = new HashSet<>();
		String select = buildSelect(fieldSelection, where);
		
		try (PreparedStatement s = conn.prepareStatement(select)) {
			if (where != null) {	// Has conditions
				int counter = 1;
				for (Object value : where.values()) {
					if (!shortCircuitSet(s, counter++, value, conn))	// Missing reference
						return results;	// Short-circuit
				}
			}
			try (ResultSet rs = s.executeQuery()) {
				while(rs.next()) {
					try {
						T result = c.newInstance();
						
						for (SqlobField field : fields) {
							try {
								Object value = rs.getObject(field.name);
								if (field.isReference() && value != null)
									value = field.reference.get(UUID.fromString((String) value), conn);
								
								field.field.set(result, value);
							} catch (IllegalAccessException e) {
								throw new NonPersistableException(field.field + " is inaccessible");
							}
						}
						results.add(result);
					} catch (IllegalAccessException | InstantiationException e) {
						throw new NonPersistableException(c.getName() + " does not provide an accessible nullary constructor");
					}
				}
			}
		}
		log.debug(() -> "Found " + results.size() + " instances of " + this + System.lineSeparator() + "\t" + applyStatement(select, (where == null ? null : where.values())));
		return results;
	}
	
	private boolean shortCircuitSet(PreparedStatement s, int index, Object value, Connection conn) throws SQLException {	// Returns false and does not modify Statement if missing reference
		Object transformed = transform(value, conn);
		if (value != null && transformed == null) {	// Missing reference
			log.info(() -> "Missing reference for " + this + " field value; short-circuiting");
			return false;
		}
		s.setObject(index, transformed);
		return true;
	}
	
	private Object transform(Object o, Connection conn) throws SQLException {
		if (o == null)
			return o;
		
		SqlobField field = getField(o);
		return field == null ? o : field.transform(o, conn);
	}
	private SqlobField getField(Object o) {
		Class<?> oClass = o.getClass();
		
		for (SqlobField field : fields) {
			if (field.field.getType() == oClass)
				return field;
		}
		return null;
	}
	
	private String buildSelect(String selection, Condition where) {
		String result = "SELECT " + selection + " FROM " + name;
		
		if (where != null)
			result += " WHERE " + where;
		
		return result;
	}
	
	private Condition buildEquals(T instance) {
		Condition result = null;
		
		for (SqlobField field : fields) {
			try {
				String attribute = field.name;
				Object value = field.field.get(instance);
				Condition currentCondition = new Condition(attribute, (value == null ? "IS" : "="), value);
				
				if (result == null)
					result = currentCondition;
				else
					result.and(currentCondition);
			} catch (IllegalAccessException e) {
				throw new NonPersistableException(field.field + " is inaccessible");
			}
		}
		return result;
	}
	
	private static String applyStatement(String base, Iterable<Object> parameters) {	// For logging
		String statement = base;
		
		if (parameters != null) {
			for (Object parameter : parameters)
				statement = statement.replaceFirst(Pattern.quote("?"), Matcher.quoteReplacement(parameter.toString()));
		}
		return statement;
	}
	
	@Override
	public String toString() {
		return c.getName() + "->" + name;
	}
}
