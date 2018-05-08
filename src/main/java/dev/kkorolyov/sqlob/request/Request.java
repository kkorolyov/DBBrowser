package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.column.handler.factory.ColumnHandlerFactory;
import dev.kkorolyov.sqlob.logging.Logger;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.util.PersistenceHelper;
import dev.kkorolyov.sqlob.util.UncheckedSqlException;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static dev.kkorolyov.sqlob.util.PersistenceHelper.getPersistableFields;
import static dev.kkorolyov.sqlob.util.UncheckedSqlException.wrapSqlException;

/**
 * A single transaction of a specific class.
 * @param <T> handled type
 */
public abstract class Request<T> {
	private final Class<T> type;
	private final String name;
	private final Map<Integer, Column<?>> columns = new HashMap<>();

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

		Stream.concat(
				Stream.of(KeyColumn.ID),
				getPersistableFields(type).map(this::asColumn)
		).forEach(this::addColumn);
	}
	private Column<?> asColumn(Field f) {
		return ColumnHandlerFactory.get(f)
				.get(f);
	}

	/**
	 * Adds a column to this request.
	 * @param column column to add
	 */
	public void addColumn(Column<?> column) {
		columns.put(columns.size(), column);
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

	/**
	 * Invokes an action for each filtered {@code {columnIndex, column}} pair
	 * @param c column superinterface or superclass to filter by
	 * @param action action to invoke with index and column from all columns in this request which subclass {@code c}
	 * @param <C> interface type
	 */
	public final <C> void forEachColumn(Class<C> c, BiConsumer<Integer, C> action) {
		columns.entrySet().stream()
				.filter(entry -> c.isInstance(entry.getValue()))
				.forEach(entry -> action.accept(entry.getKey(), (C) entry.getValue()));
	}

	/** @return stream over all columns in this request */
	public final Stream<Column<?>> streamColumns() {
		return columns.values().stream();
	}
	/**
	 * @param c column superinterface or superclass to filter by
	 * @param <C> interface type
	 * @return stream over all columns in this request which subclass {@code c}
	 */
	public final <C> Stream<C> streamColumns(Class<C> c) {
		return streamColumns()
				.filter(c::isInstance)
				.map(c::cast);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Request<?> request = (Request<?>) o;

		return Objects.equals(type, request.type) &&
				Objects.equals(name, request.name) &&
				Objects.equals(columns, request.columns);
	}
	@Override
	public int hashCode() {
		return Objects.hash(type, name, columns);
	}

	@Override
	public String toString() {
		return "Request{" +
				"type=" + type +
				", name='" + name + '\'' +
				", columns=" + columns +
				'}';
	}
}
