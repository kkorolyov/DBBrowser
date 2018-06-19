package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.column.handler.factory.ColumnHandlerFactory;
import dev.kkorolyov.sqlob.result.Record;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.struct.Table;
import dev.kkorolyov.sqlob.util.PersistenceHelper;
import dev.kkorolyov.sqlob.util.UncheckedSqlException;
import dev.kkorolyov.sqlob.util.Where;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static dev.kkorolyov.simplefuncs.stream.Collectors.keyedOn;
import static dev.kkorolyov.sqlob.util.PersistenceHelper.getPersistableFields;
import static dev.kkorolyov.sqlob.util.UncheckedSqlException.wrapSqlException;

/**
 * A single transaction of a specific class.
 * @param <T> handled type
 */
public abstract class Request<T> {
	// TODO Use struct.Table
	private final Class<T> type;
	private final String name;
	private final Map<String, Column<?>> columns;

	/**
	 * Constructs a new request with name retrieved from {@code type} using {@link PersistenceHelper}.
	 * @see #Request(Class, String)
	 */
	protected Request(Class<T> type) {
		this(type, PersistenceHelper.getName(type));
	}
	/**
	 * Constructs a new request with an ID column and additional columns generated from each persistable field in {@code type}.
	 * @param type associated type
	 * @param name associated table name
	 */
	protected Request(Class<T> type, String name) {
		this(
				type,
				name,
				Stream.concat(
						Stream.of(KeyColumn.ID),
						getPersistableFields(type)
								.map(f -> ColumnHandlerFactory.get(f).get(f))
				)
		);
	}

	/**
	 * Constructs a new request with custom columns.
	 * @param type associated type
	 * @param name associated table name
	 * @param columns associated columns
	 */
	protected Request(Class<T> type, String name, Iterable<Column<?>> columns) {
		this(type, name, StreamSupport.stream(columns.spliterator(), false));
	}

	private Request(Class<T> type, String name, Stream<Column<?>> columns) {
		this.type = type;
		this.name = name;
		this.columns = columns
				.collect(keyedOn(Column::getName));
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
	protected abstract Result<T> executeThrowing(ExecutionContext context) throws SQLException;

	/**
	 * Resolves a where clause's values within a given context.
	 * @param where where to resolve
	 * @param context context to work in
	 * @return resolved where
	 * @throws IllegalArgumentException if an attribute in {@code where} does not correspond to a persistable field on this request's {@code type}
	 */
	protected final Where resolve(Where where, ExecutionContext context) {
		Map<String, String> fieldNames = new HashMap<>(PersistenceHelper.getPersistableFields(getType())
				.collect(Collectors.toMap(
						Field::getName,
						PersistenceHelper::getName
				)));
		fieldNames.put(KeyColumn.ID.getName(), KeyColumn.ID.getName());

		return where.map(
				name -> {
					String resolvedName = fieldNames.get(name);
					if (resolvedName == null) throw new IllegalArgumentException("No such persistable field: " + name + " for type: " + getType() + "; available persistable fields: " + fieldNames.keySet());
					return resolvedName;
				},
				(name, value) -> getColumn(name).resolve(value, context)
		);
	}

	/**
	 * @param record record to build a statement batch for
	 * @param context execution context to work in
	 * @return {@code {name, value}} pairs for values of each column associated with {@code record}
	 */
	protected final Map<String, Object> buildBatch(Record<UUID, T> record, ExecutionContext context) {
		return streamColumns()
				.collect(Collectors.toMap(
						Column::getName,
						column -> column.get(record, context)
				));
	}

	/**
	 * @param context context to work in
	 * @return SQL table represented by this request within {@code context}
	 */
	public final Table toTable(ExecutionContext context) {
		return new Table(
				getName(),
				streamColumns()
						.map(column -> new dev.kkorolyov.sqlob.struct.Column(column.getName(), column.getSql(context)))
						.collect(Collectors.toList())
		);
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
	 * @param name name of column to get
	 * @return column with name matching {@code name}
	 */
	public final Column<?> getColumn(String name) {
		return columns.get(name);
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
