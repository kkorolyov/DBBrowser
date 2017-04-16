package dev.kkorolyov.sqlob.service;

import static dev.kkorolyov.sqlob.service.Constants.ID_NAME;
import static dev.kkorolyov.sqlob.service.Constants.ID_TYPE;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.kkorolyov.sqlob.annotation.Column;
import dev.kkorolyov.sqlob.annotation.Table;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.persistence.NonPersistableException;

/**
 * Generates SQL statements.
 */
public final class StatementGenerator {
	private static final Logger log = Logger.getLogger(StatementGenerator.class.getName());

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
	 * Generates all CREATE TABLE statements required to represent a class as a relational table.
	 * @param c class to generate statement for
	 * @return SQL statement creating a table matching a class
	 */
	public Iterable<String> generateCreate(Class<?> c) {
		List<String> statements = new ArrayList<>();

		for (Class<?> associated : mapper.getAssociatedClasses(c)) {
			String statement = "CREATE TABLE IF NOT EXISTS " + getName(associated) + " " + generateFieldDeclarations(associated);
			statements.add(statement);
			log.debug(() -> "Added to CREATE statements for " + c + ": " + statement);
		}
		return statements;
	}
	private String generateFieldDeclarations(Class<?> c) {
		StringJoiner declarations = new StringJoiner(", ", "(", ")");

		declarations.add(ID_NAME + " " + ID_TYPE + " PRIMARY KEY");
		declarations.add(StreamSupport.stream(mapper.getPersistableFields(c).spliterator(), true)
																	.map(this::generateFieldDeclaration)
																	.collect(Collectors.joining(", ")));

		return declarations.toString();
	}
	private String generateFieldDeclaration(Field f) {
		String name = getName(f);
		String primitive = mapper.getSql(f);

		return name + " " + (primitive != null ? primitive : ID_TYPE + ", FOREIGN KEY (" + name + ") REFERENCES " + getName(f.getClass()) + " (" + ID_NAME + ")");
	}

	/**
	 * Generates all INSERT INTO statements required to insert data into a relational table mapped to a class.
	 * @param c class to generate statement for
	 * @return SQL statement inserting into a table mapped to a class
	 */
	public Iterable<String> generateInsert(Class<?> c) {
		List<String> statements = new ArrayList<>();

		StreamSupport.stream(mapper.getPersistableFields(c).spliterator(), true)
								 .filter(mapper::isComplex)
								 .forEach(field -> {
								 		for (String statement : generateInsert(field.getType())) statements.add(statement);
								 });
		String statement = "INSERT INTO TABLE " + getName(c) + " " + generateColumns(c) + " VALUES " + generatePlaceholders(c);
		statements.add(statement);
		log.debug(() -> "Added to INSERT statements for " + c + ": " + statement);

		return statements;
	}
	private String generateColumns(Class<?> c) {
		return StreamSupport.stream(mapper.getPersistableFields(c).spliterator(), true)
												.map(StatementGenerator::getName)
												.collect(Collectors.joining(", ", "(", ")"));
	}
	private String generatePlaceholders(Class<?> c) {
		return StreamSupport.stream(mapper.getPersistableFields(c).spliterator(), true)
												.map(field -> "?")
												.collect(Collectors.joining(", ", "(", ")"));
	}

	private static String getName(Class<?> c) {
		Table override = c.getAnnotation(Table.class);
		if (override != null && override.value().length() <= 0) throw new NonPersistableException(c + " has a Table annotation with an empty name");

		return (override == null) ? c.getSimpleName() : override.value();
	}
	private static String getName(Field f) {
		Column override = f.getAnnotation(Column.class);
		if (override != null && override.value().length() <= 0) throw new NonPersistableException(f + " has a Column annotation with an empty name");

		return (override == null) ? f.getName() : override.value();
	}
}
