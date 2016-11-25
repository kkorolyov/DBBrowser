package dev.kkorolyov.sqlob.persistence;

import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

import dev.kkorolyov.sqlob.annotation.Table;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.sql.Condition;
import dev.kkorolyov.sqlob.sql.Selection;

final class SqlobClass<T> {
	private static final Logger log = Logger.getLogger(SqlobClass.class.getName());
	
	final Class<T> c;
	final String name;
	final List<SqlobField> fields;	// TODO Map
	private final String 	idName,
												idType;
	private final Selection idSelection,
													fieldSelection;
	private final String insert;
	
	SqlobClass(Class<T> c, List<SqlobField> fields, String idName, String idType) {
		this.c = c;
		this.fields = fields;
		this.idName = idName;
		this.idType = idType;
		
		Table override = this.c.getAnnotation(Table.class);
		name = (override == null || override.value().length() <= 0) ? this.c.getSimpleName() : override.value();
		
		idSelection = new Selection(idName);
		fieldSelection = new Selection();
		for (SqlobField field : fields)
			fieldSelection.append(field.name);
		
		insert = buildInsert();
	}
	private String buildInsert() {
		StringBuilder builder = new StringBuilder("INSERT INTO ").append(name).append(" (").append(idName),
									values = new StringBuilder("VALUES (?");
		
		for (SqlobField field : fields) {
			builder.append(",").append(field.name);
			values.append(",?");
		}
		builder.append(") ");
		values.append(")");
		
		builder.append(values.toString());
		
		return builder.toString();
	}
	
	SqlobClass<T> init(Connection conn) throws SQLException {
		StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(name);
		builder.append("(").append(idName).append(" ").append(idType).append(" PRIMARY KEY");
		
		for (SqlobField field : fields)
			builder.append(", ").append(field.getInit(idName));
		builder.append(")");
		
		try (Statement s = conn.createStatement()) {
			s.executeUpdate(builder.toString());
		}
		conn.commit();
		log.debug(() -> "Initialized new SqlobClass: " + this);
		return this;
	}
	
	UUID put(Object instance, Connection conn) throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(insert)) {
			int counter = 1;
			for (SqlobField field : fields) {
				try {
					s.setObject(counter++, transform(field.field.get(instance), conn), field.typeCode);
				} catch (IllegalAccessException e) {
					throw new NonPersistableException(field.field.getName() + " is inaccessible");
				}
			}
			s.executeUpdate();
		}
		conn.commit();
		return getId(instance, conn);
	}
	
	UUID getId(Object instance, Connection conn) throws SQLException {
		Condition where = buildEquals(getValues((T) instance));
		try (PreparedStatement s = conn.prepareStatement(buildSelect(idSelection, where))) {
			int counter = 1;
			for (Object value : where.values())
				s.setObject(counter++, transform(value, conn));
			
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
						
						for (SqlobField field : fields) {
							try {
								Object value = field.isReference() ? field.reference.getInstance(UUID.fromString(rs.getString(field.name)), conn) : rs.getObject(field.name);
								field.field.set(result, value);
							} catch (IllegalAccessException e) {
								throw new NonPersistableException(field.field.getName() + " is inaccessible");
							}
						}
					} catch (IllegalAccessException | InstantiationException e) {
						throw new NonPersistableException(c.getName() + " does not provide an accessible nullary constructor");
					}
				}
			}
		}
		return results;
	}
	
	private Object transform(Object o, Connection conn) throws SQLException {
		Class<?> c = o.getClass();
		
		for (SqlobField field : fields) {
			if (c == field.field.getType() && field.isReference()) {
				UUID id = field.reference.getId(o, conn);
				return id == null ? null : id.toString();
			}
		}
		return o;
	}
	
	private Map<SqlobField, Object> getValues(T instance) {
		Map<SqlobField, Object> values = new HashMap<>();
		
		for (SqlobField field : fields) {
			try {
				values.put(field, field.field.get(instance));
			} catch (IllegalAccessException e) {
				throw new NonPersistableException(field.field.getName() + " is inaccessible");
			}
		}
		return values;
	}
	
	private String buildSelect(Selection selection, Condition where) {
		String result = "SELECT " + selection + " FROM " + name;
		
		if (where != null)
			result += " WHERE " + where;
		
		return result;
	}
	
	private Condition buildEquals(Map<SqlobField, Object> fieldMap) {
		Condition result = null;
		
		for (Entry<SqlobField, Object> entry : fieldMap.entrySet()) {
			String attribute = entry.getKey().name;
			Object value = entry.getValue();
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
		return SqlobClass.class.getSimpleName() + "(" + c.getName() + "->" + name + ")";
	}
}
