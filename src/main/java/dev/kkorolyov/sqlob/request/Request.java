package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.column.handler.factory.ColumnHandlerFactory;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.util.PersistenceHelper;
import dev.kkorolyov.sqlob.util.UncheckedSqlException;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static dev.kkorolyov.sqlob.util.PersistenceHelper.getPersistableFields;
import static dev.kkorolyov.sqlob.util.UncheckedSqlException.wrapSqlException;

/**
 * A single transaction of a specific class.
 * @param <T> handled type
 */
public abstract class Request<T> {
	private final Class<T> type;
	private final String name;
	private final Set<FieldBackedColumn<?>> columns = new LinkedHashSet<>();

	/**
	 * Constructs a new request with name retrieved from {@code type} using {@link PersistenceHelper}.
	 * @see #Request(Class, String)
	 */
	Request(Class<T> type) {
		this(type, PersistenceHelper.getName(type));
	}
	/**
	 * Constructs a new request.
	 * @param type associated type
	 * @param name associated table name
	 */
	Request(Class<T> type, String name) {
		this.type = type;
		this.name = name;

		getPersistableFields(type)
				.map(this::asColumn)
				.forEach(this::addColumn);
	}
	private FieldBackedColumn<?> asColumn(Field f) {
		return ColumnHandlerFactory.get(f)
				.get(f);
	}

	/**
	 * Adds a column to this request.
	 * @param column column to add
	 */
	public void addColumn(FieldBackedColumn<?> column) {
		columns.add(column);
	}

	/**
	 * Executes this request within the given context.
	 * @param context execution context
	 * @return execution result
	 * @throws UncheckedSqlException if a SQL database issue occurs
	 */
	public final Result<T> execute(ExecutionContext context) {
		return wrapSqlException(() -> executeThrowing(context));
	}
	abstract Result<T> executeThrowing(ExecutionContext context) throws SQLException;

	/** @see #logStatements(Iterable) */
	void logStatements(String... statements) {
		logStatements(Arrays.asList(statements));
	}
	/**
	 * Logs a message that this request is executing statements.
	 * @param statements statements to write to log
	 */
	void logStatements(Iterable<String> statements) {
		Logger.getLogger(getClass().getName())
				.debug("Executing statements: {}", statements);
	}

	/** @return type handled by request */
	public final Class<T> getType() {
		return type;
	}
	/** @return table name of type handled by this request */
	public final String getName() {
		return name;
	}
	/** @return all request columns */
	public final Set<FieldBackedColumn<?>> getColumns() {
		return columns;
	}
}
