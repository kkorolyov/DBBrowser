package dev.kkorolyov.sqlob.service;

import static dev.kkorolyov.sqlob.service.Constants.ID_NAME;
import static dev.kkorolyov.sqlob.service.Constants.ID_TYPE;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.kkorolyov.sqlob.annotation.Column;
import dev.kkorolyov.sqlob.annotation.Table;
import dev.kkorolyov.sqlob.persistence.NonPersistableException;

/**
 * Generates SQL statements.
 */
public final class StatementGenerator {
	private final Mapper mapper;

	/**
	 * Constructs a new statement generator using the default mapper.
	 */
	public StatementGenerator() {
		this(new Mapper());
	}
	/**
	 * Constructs a new statement generator.
	 * @param mapper Java-SQL mapper
	 */
	public StatementGenerator(Mapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Generates a CREATE TABLE statement for a class.
	 * @param c class to generate statement for
	 * @return SQL statement creating a table matching a class
	 */
	public Iterable<String> generateCreate(Class<?> c) {	// TODO Return array of create statements with dependent / foreign key table creates first
		List<String> statements = new ArrayList<>();

		Iterable<Field> persistableFields = mapper.getPersistableFields(c);
		Iterable<Class<?>> dependencies = StreamSupport.stream(persistableFields.spliterator(), true)
				.filter(field -> mapper.getSql(field) == null)	// Keep only non-primitives
				.map(field -> field.getClass());

		statements.add("CREATE TABLE IF NOT EXISTS " + getName(c) + " (" + ID_NAME + " " + ID_TYPE + " PRIMARY KEY, "
									 + generateFieldDeclarations(c) + ")");

		return statements;
	}

	private Iterable<Class<?>> getDependentClasses(Class<?> c) {

	}

	private String generateFieldDeclarations(Class<?> c) {
		return StreamSupport.stream(mapper.getPersistableFields(c).spliterator(), true)
												.map(this::generateFieldDeclaration)
												.collect(Collectors.joining(", "));
	}
	private String generateFieldDeclaration(Field f) {
		String name = getName(f);
		String primitive = mapper.getSql(f);

		return name + " " + ((primitive != null) ? primitive : ID_TYPE + ", FOREIGN KEY (" + name + ") REFERENCES " + getName(f.getClass()) + " (" + ID_NAME + ")");
	}

	public String generateInsert(Class<?> c) {

	}

	private static final String getName(Class<?> c) {
		Table override = c.getAnnotation(Table.class);
		if (override != null && override.value().length() <= 0) throw new NonPersistableException(c + " has a Table annotation with an empty name");

		return (override == null) ? c.getSimpleName() : override.value();
	}
	private static final String getName(Field f) {
		Column override = f.getAnnotation(Column.class);
		if (override != null && override.value().length() <= 0) throw new NonPersistableException(f + " has a Column annotation with an empty name");

		return (override == null) ? f.getName() : override.value();
	}
}
