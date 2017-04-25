package dev.kkorolyov.sqlob.service;

import static dev.kkorolyov.sqlob.service.Constants.ID_NAME;
import static dev.kkorolyov.sqlob.service.Constants.ID_TYPE;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.utility.Condition;

/**
 * Generates SQL statements.
 */
public class StatementGenerator {
	private static final Logger log = Logger.getLogger(StatementGenerator.class.getName());

	private final Mapper mapper;

	/**
	 * Constructs a new statement generator using the default @{code Mapper}.
	 */
	public StatementGenerator() {
		this(new Mapper());
	}
	/**
	 * Constructs a new statement generator.
	 * @param mapper Java-relational mapper
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
			String statement = "CREATE TABLE IF NOT EXISTS " + mapper.getName(associated) + " " + generateFieldDeclarations(associated);
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
		String name = mapper.getName(f);
		String primitive = mapper.sql(f);

		return name + " " + (primitive != null ? primitive : ID_TYPE + ", FOREIGN KEY (" + name + ") REFERENCES " + mapper.getName(f.getType()) + " (" + ID_NAME + ")");
	}

	/**
	 * Generates a parametrized SELECT statement for selecting instances of a class matching a condition.
	 * @param c class to generate statement for
	 * @param where condition to match, {@code null} implies no condition
	 * @return parametrized SQL statement selecting instances of {@code c} matching {@code where}
	 */
	public String generateSelect(Class<?> c, Condition where) {
		return generateSelect(c, where, false);
	}

	/**
	 * Generates a parametrized SELECT statement for selecting the ID of a class instance matching a condition.
	 * @param c class to generate statement for
	 * @param where condition to match
	 * @return parametrized SQL statement selecting ID
	 */
	public String generateSelectId(Class<?> c, Condition where) {
		return generateSelect(c, where, true);
	}

	private String generateSelect(Class<?> c, Condition where, boolean idOnly) {
		String statement = "SELECT " + (idOnly ? ID_NAME : "*") +
											 " FROM " + mapper.getName(c) +
											 (where == null ? "" : " WHERE " + where.toString());

		log.debug(() -> "Generated SELECT: " + statement);
		return statement;
	}

	/**
	 * Generates a parametrized INSERT statement for inserting an instance of a class into the corresponding relational table.
	 * @param c class to generate statement for
	 * @return parametrized SQL statement inserting an instance
	 */
	public String generateInsert(Class<?> c) {
		String statement = "INSERT INTO " + mapper.getName(c) +
											 " " + generateColumns(c) +
											 " VALUES " + generatePlaceholders(c);

		log.debug(() -> "Generated INSERT: " + statement);
		return statement;
	}
	private String generateColumns(Class<?> c) {
		return StreamSupport.stream(mapper.getPersistableFields(c).spliterator(), true)
												.map(mapper::getName)
												.collect(Collectors.joining(", ", "(" + ID_NAME + ", ", ")"));	// Sneak in ID column
	}
	private String generatePlaceholders(Class<?> c) {
		return StreamSupport.stream(mapper.getPersistableFields(c).spliterator(), true)
												.map(field -> "?")
												.collect(Collectors.joining(", ", "(?, ", ")"));	// Sneak in ID column placeholder
	}

	/**
	 * Generates a parametrized UPDATE statement for updating a class instance.
	 * @param c class to generate statement for
	 * @param where update criteria, may not be {@code null}
	 * @return parametrized SQL statement updating an instance of {@code c}
	 */
	public String generateUpdate(Class<?> c, Condition where) {
		String statement = "UPDATE " + mapper.getName(c) +
											 " SET " + generateSet(c) +
											 " WHERE " + where.toString();

		log.debug(() -> "Generated UPDATE: " + statement);
		return statement;
	}
	private String generateSet(Class<?> c) {
		return StreamSupport.stream(mapper.getPersistableFields(c).spliterator(), true)
												.map(field -> mapper.getName(field) + " = ?")
												.collect(Collectors.joining(", "));
	}

	/**
	 * Generates a parametrized DELETE statement for deleting a class instance.
	 * @param c class to generate statement for
	 * @param where deletion criteria
	 * @return parametrized SQL statement deleting an instance of {@code c}
	 */
	public String generateDelete(Class<?> c, Condition where) {
		String statement = "DELETE FROM " + mapper.getName(c) +
											 (where == null ? "" : " WHERE " + where.toString());

		log.debug(() -> "Generated DELETE: " + statement);
		return statement;
	}
}
