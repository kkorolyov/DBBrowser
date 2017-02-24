package dev.kkorolyov.sqlob.persistence;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.kkorolyov.sqlob.annotation.Table;
import dev.kkorolyov.sqlob.logging.Logger;

/**
 * Manages persistence at the class/table level.
 * @param <T> persisted class type
 */
final class SqlobClass<T> {
	private static final Logger log = Logger.getLogger(SqlobClass.class.getName());

	final String name;
	private final Class<T> c;
	private final Constructor<T> constructor;
	private final Iterable<SqlobField> fields;
	private final String insert;
	private final String fieldSelection;
	
	SqlobClass(Class<T> c, Iterable<SqlobField> fields, Connection conn) throws SQLException {
		this.c = c;
		this.fields = fields;

		try {
			constructor = this.c.getDeclaredConstructor();
			constructor.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new NonPersistableException(c.getName() + " does not provide a nullary constructor");
		}
		Table override = this.c.getAnnotation(Table.class);
		if (override == null) name = c.getSimpleName();
		else if (override.value().length() <= 0) throw new NonPersistableException(c.getName() + " has table override with empty name");
		else name = override.value();

		fieldSelection = StreamSupport.stream(fields.spliterator(), true)	// Used in SELECT clause
																	.map(field -> field.name)
																	.collect(Collectors.joining(", "));
		insert = "INSERT INTO " + name + " (" + Constants.ID_NAME + fieldSelection + ") "
						 + StreamSupport.stream(fields.spliterator(), false)
														.map(field -> "?")
														.collect(Collectors.joining(", ", "VALUES (?", ")"));
		String create = "CREATE TABLE IF NOT EXISTS " + name + "(" + Constants.ID_NAME + " " + Constants.ID_TYPE + " PRIMARY KEY, "
										+ StreamSupport.stream(fields.spliterator(), false)
																	 .map(field -> field.getInit(Constants.ID_NAME))
																	 .collect(Collectors.joining(", "));
		try (Statement s = conn.createStatement()) {
			s.executeUpdate(create);
		}
		log.debug(() -> "Initialized new SqlobClass: " + this + System.lineSeparator() + "\t" + create);
	}

	@SuppressWarnings("unchecked")
	UUID getId(Object instance, Connection conn) throws SQLException {
		Condition where = buildEquals((T) instance);
		String select = buildSelect(Constants.ID_NAME, where);
		
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
		Set<T> instances = get(new Condition(Constants.ID_NAME, "=", id.toString()), conn);
		T result = instances.isEmpty() ? null : instances.iterator().next();
		
		log.debug(() -> (result == null ? "Failed to find " : "Found ") + "instance of " + this + ": " + id + (result == null ? "" : "->" + result));
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
						T result = constructor.newInstance();
						
						for (SqlobField field : fields)
							field.apply(result, rs, conn);
						
						results.add(result);
					} catch (IllegalAccessException | InstantiationException | IllegalArgumentException | InvocationTargetException e) {
						throw new NonPersistableException("Failed to instantiate " + c.getName(), e);
					}
				}
			}
		}
		log.debug(() -> "Found " + results.size() + " instances of " + this + System.lineSeparator() + "\t" + applyStatement(select, (where == null ? null : where.values())));
		return results;
	}
	
	UUID put(Object instance, Connection conn) throws SQLException {
		UUID id = getId(instance, conn);
		if (id == null) {
			id = UUID.randomUUID();
			put(id, instance, conn);
		}
		return id;
	}
	boolean put(UUID id, Object instance, Connection conn) throws SQLException {
		boolean result = drop(id, conn);	// Drop any previous version
		List<Object> parameters = new LinkedList<>();	// For logging
		
		try (PreparedStatement s = conn.prepareStatement(insert)) {
			s.setString(1, id.toString());
			parameters.add(id);	// Logging

			int counter = 2;
			for (SqlobField field : fields) {
				Object value = 	field.get(instance),
												transformed = transform(value, conn);
				if (value != null && transformed == null)	// Missing reference
					transformed = field.put(value, conn);	// Recursive put

				s.setObject(counter++, transformed, field.typeCode);
				parameters.add(transformed);	// Logging
			}
			s.executeUpdate();
		}
		log.debug(() -> (result ? "Replaced " : "Saved new ") + this + ": " + instance + "->" + id + System.lineSeparator() + "\t" + applyStatement(insert, parameters));
		return result;
	}
	
	boolean drop(UUID id, Connection conn) throws SQLException {
		boolean result = drop(new Condition(Constants.ID_NAME, "=", id.toString()), conn) > 0;
		
		log.debug(() -> (result ? "Deleted " : "Failed to delete ") + "instance of " + this + ": " + id);
		return result;
	}
	int drop(Condition where, Connection conn) throws SQLException {
		String delete = buildDelete(where);
		
		try (PreparedStatement s = conn.prepareStatement(delete)) {
			if (where != null) {	// Has conditions
				int counter = 1;
				for (Object value : where.values()) {
					if (!shortCircuitSet(s, counter++, value, conn))	// Missing reference
						return 0;	// Short-circuit
				}
			}
			int result = s.executeUpdate();
			
			log.debug(() -> "Deleted " + result + " instances of " + this + System.lineSeparator() + "\t" + applyStatement(delete, (where == null ? null : where.values())));
			return result;
		}
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
			if (field.getType() == oClass)
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
	private String buildDelete(Condition where) {
		String result = "DELETE FROM " + name;
		
		if (where != null)
			result += " WHERE " + where;
		
		return result;
	}
	
	private Condition buildEquals(T instance) {
		Condition result = null;
		
		for (SqlobField field : fields) {
			String attribute = field.name;
			Object value = field.get(instance);
			Condition currentCondition = new Condition(attribute, (value == null ? "IS" : "="), value);

			if (result == null)
				result = currentCondition;
			else
				result.and(currentCondition);
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
