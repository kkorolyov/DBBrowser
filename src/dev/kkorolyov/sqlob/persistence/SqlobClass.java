package dev.kkorolyov.sqlob.persistence;

import static dev.kkorolyov.sqlob.service.Constants.ID_NAME;
import static dev.kkorolyov.sqlob.service.Constants.ID_TYPE;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.kkorolyov.sqlob.annotation.Table;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.utility.Condition;

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

	SqlobClass(Class<T> c, Iterable<SqlobField> fields, Statement createStatement) throws SQLException {
		this.c = c;
		this.fields = fields;

		try {
			constructor = c.getDeclaredConstructor();
			constructor.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new NonPersistableException(c.getName() + " does not provide a nullary constructor");
		}
		Table override = c.getAnnotation(Table.class);
		if (override == null) name = c.getSimpleName();
		else if (override.value().length() <= 0) throw new NonPersistableException(c.getName() + " has table override with empty name");
		else name = override.value();

		fieldSelection = StreamSupport.stream(fields.spliterator(), true)	// Used in SELECT clause
																	.map(field -> field.name)
																	.collect(Collectors.joining(", "));
		insert = "INSERT INTO " + name + " (" + ID_NAME + ", " + fieldSelection + ") "
						 + StreamSupport.stream(fields.spliterator(), false)
														.map(field -> "?")
														.collect(Collectors.joining(", ", "VALUES (?, ", ")"));

		String create = "CREATE TABLE IF NOT EXISTS " + name + "(" + ID_NAME + " " + ID_TYPE + " PRIMARY KEY, "
										+ StreamSupport.stream(fields.spliterator(), false)
																	 .map(SqlobField::getCreateSnippet)
																	 .collect(Collectors.joining(", "))
										+ ")";
		createStatement.executeUpdate(create);

		log.debug(() -> "Initialized new SqlobClass: " + this
										+ System.lineSeparator() + "\t" + create);
	}

	UUID getId(Object instance, Connection conn) throws SQLException {
		Condition where = generateEquals(instance);
		String select = generateSelect(ID_NAME, where);
		
		try (PreparedStatement s = conn.prepareStatement(select)) {
			if (!setValues(s, where.values(), conn)) return null;	// setValues did not complete due to missing reference, abort early

			ResultSet rs = s.executeQuery();
			UUID result = rs.next() ? UUID.fromString(rs.getString(1)) : null;

			log.debug(() -> (result == null ? "Failed to find " : "Found ") + "ID of " + this + " instance: " + instance + "->" + result
											+ System.lineSeparator() + "\t" + s);
			return result;
		}
	}
	
	T get(UUID id, Connection conn) throws SQLException {
		Set<T> instances = get(new Condition(ID_NAME, "=", id.toString()), conn);
		T result = instances.isEmpty() ? null : instances.iterator().next();
		
		log.debug(() -> (result == null ? "Failed to find " : "Found ") + "instance of " + this + ": " + id + "->" + result);
		return result;
	}
	Set<T> get(Condition where, Connection conn) throws SQLException {
		Set<T> results = new HashSet<>();
		String select = generateSelect(fieldSelection, where);
		
		try (PreparedStatement s = conn.prepareStatement(select)) {
			if (where != null && !setValues(s, where.values(), conn)) return results;	// setValues did not complete due to missing reference, abort early

			ResultSet rs = s.executeQuery();
			while(rs.next()) {
				try {
					T result = constructor.newInstance();

					for (SqlobField field : fields) field.populateInstance(result, rs, conn);

					results.add(result);
				} catch (IllegalAccessException | InstantiationException | IllegalArgumentException | InvocationTargetException e) {
					throw new NonPersistableException("Failed to instantiate " + c.getName(), e);
				}
			}
			log.debug(() -> "Found " + results.size() + " instances of " + this
											+ System.lineSeparator() + "\t" + s);
		}
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

		try (PreparedStatement s = conn.prepareStatement(insert)) {
			s.setString(1, id.toString());

			int counter = 2;
			for (SqlobField field : fields) field.populateStatement(s, counter++, instance, conn);

			s.executeUpdate();

			log.debug(() -> (result ? "Replaced " : "Saved new ") + this + ": " + instance + "->" + id
											+ System.lineSeparator() + "\t" + s);
		}
		return result;
	}
	
	boolean drop(UUID id, Connection conn) throws SQLException {
		boolean result = drop(new Condition(ID_NAME, "=", id.toString()), conn) > 0;
		
		log.debug(() -> (result ? "Deleted " : "Failed to delete ") + "instance of " + this + ": " + id);
		return result;
	}
	int drop(Condition where, Connection conn) throws SQLException {
		String delete = generateDelete(where);
		
		try (PreparedStatement s = conn.prepareStatement(delete)) {
			if (where != null && !setValues(s, where.values(), conn)) return 0;	// setValues did not complete due to missing reference, abort early

			int result = s.executeUpdate();
			
			log.debug(() -> "Deleted " + result + " instances of " + this
											+ System.lineSeparator() + "\t" + s);
			return result;
		}
	}

	private boolean setValues(PreparedStatement s, Iterable<Object> values, Connection conn) throws SQLException {
		int i = 1;
		for (Object value : values) {
			Object transformed = (value == null) ? value : transform(value, conn);
			if (value != null && transformed == null) {
				log.info(() -> "Missing reference for " + this + " field value, aborting early");
				return false;
			}
			s.setObject(i++, transformed);
		}
		return true;
	}
	private Object transform(Object o, Connection conn) throws SQLException {
		SqlobField field = findField(o.getClass());
		return field == null ? o : field.transform(o, conn);
	}
	private SqlobField findField(Class<?> c) {
		for (SqlobField field : fields) {
			if (field.getType() == c) return field;
		}
		return null;
	}

	private String generateSelect(String selection, Condition where) {
		String result = "SELECT " + selection + " FROM " + name;
		
		if (where != null)
			result += " WHERE " + where;
		
		return result;
	}
	private String generateDelete(Condition where) {
		String result = "DELETE FROM " + name;
		
		if (where != null)
			result += " WHERE " + where;
		
		return result;
	}
	
	private Condition generateEquals(Object instance) {
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
	
	@Override
	public String toString() {
		return c.getName() + "->" + name;
	}
}
