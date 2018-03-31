package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.column.factory.ColumnFactory;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.util.PersistenceHelper;
import dev.kkorolyov.sqlob.util.UncheckedSqlException;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static dev.kkorolyov.sqlob.util.PersistenceHelper.getPersistableFields;
import static dev.kkorolyov.sqlob.util.UncheckedSqlException.wrapSqlException;

/**
 * A single transaction of a specific class.
 */
public abstract class Request<T> {
	private static final Iterable<ColumnFactory> COLUMN_FACTORIES = ServiceLoader.load(ColumnFactory.class);

	private final Class<T> type;
	private final String name;
	private final Set<FieldBackedColumn<?>> columns = new LinkedHashSet<>();

	/**
	 * Constructs a new request.
	 * @param type associated type
	 */
	Request(Class<T> type) {
		this.type = type;
		this.name = PersistenceHelper.getName(type);

		getPersistableFields(type)
				.map(this::asColumn)
				.forEach(this::addColumn);
	}
	private FieldBackedColumn<?> asColumn(Field f) {
		return StreamSupport.stream(COLUMN_FACTORIES.spliterator(), false)
				.filter(fieldHandler -> fieldHandler.accepts(f))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("No known column factory accepts field " + f + "; known factories: " + COLUMN_FACTORIES))
				.get(f);
	}

	/**
	 * Adds a column to this request.
	 * @param column column to add
	 */
	public void addColumn(FieldBackedColumn<?> column) {
		columns.add(column);
	}

	/** @return all request columns */
	final Set<FieldBackedColumn<?>> getColumns() {
		return columns;
	}
	/** @return all request columns of type {@code c} */
	final <C extends Column<?>> Set<C> getColumns(Class<C> c) {
		return columns.stream()
				.filter(c::isInstance)
				.map(c::cast)
				.collect(Collectors.toSet());
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

	/**
	 * @see #logStatements(Iterable)
	 */
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
	final String getName() {
		return name;
	}
}
