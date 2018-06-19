package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.result.ConfigurableRecord;
import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Record;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.statement.InsertStatementBuilder;
import dev.kkorolyov.sqlob.statement.UpdateStatementBuilder;
import dev.kkorolyov.sqlob.util.Where;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Request to insert records of a class as table rows.
 */
public class InsertRequest<T> extends Request<T> {
	private final Collection<Record<UUID, T>> records;

	/**
	 * Constructs a single-instance insert request with random ID.
	 * @see #InsertRequest(UUID, Object)
	 */
	public InsertRequest(T instance) {
		this(Collections.singleton(instance));
	}
	/**
	 * Constructs a single-instance insert request.
	 * @see #InsertRequest(Collection)
	 */
	public InsertRequest(UUID id, T instance) {
		this(Collections.singleton(new ConfigurableRecord<>(id, instance)));
	}
	/**
	 * Constructs an insert request with random IDs.
	 * @see #InsertRequest(Collection)
	 */
	public InsertRequest(Iterable<T> instances) {
		this(StreamSupport.stream(instances.spliterator(), false)
				.collect(Record.collector(instance -> UUID.randomUUID())));
	}
	/**
	 * Constructs a new insert request.
	 * @param records records to insert
	 * @throws IllegalArgumentException if {@code records} is empty
	 * @see Request#Request(Class)
	 */
	public InsertRequest(Collection<Record<UUID, T>> records) {
		super(getType(records));

		this.records = records;
	}

	/**
	 * Constructs a new insert request with custom columns.
	 * @see Request#Request(Class, String, Iterable)
	 */
	InsertRequest(Collection<Record<UUID, T>> records, String name, Iterable<Column<?>> columns) {
		super(getType(records), name, columns);

		this.records = records;
	}

	private static <T> Class<T> getType(Collection<Record<UUID, T>> records) {
		return (Class<T>) records.stream()
				.findFirst()
				.map(Record::getObject)
				.map(Object::getClass)
				.orElseThrow(() -> new IllegalArgumentException("No records supplied"));
	}

	@Override
	protected Result<T> executeThrowing(ExecutionContext context) throws SQLException {
		Collection<UUID> ignoreIds = selectIds(whereRecordsExist(Record::getObject, Where::eqObject), context);  // Avoid re-inserting existing instances
		Collection<UUID> updateIds = selectIds(whereRecordsExist(Record::getKey, Where::eqId), context);  // Update existing records instead of trying to re-insert

		return new ConfigurableResult<T>()
				.add(insert(
						records.stream()
								.filter(record -> !ignoreIds.contains(record.getKey()))
								.filter(record -> !updateIds.contains(record.getKey()))
								.collect(Collectors.toSet()),
						context
				).getRecords())
				.add(update(
						records.stream()
								.filter(record -> !ignoreIds.contains(record.getKey()))
								.filter(record -> updateIds.contains(record.getKey()))
								.collect(Collectors.toSet()),
						context
				).getRecords());
	}
	private Result<T> insert(Collection<Record<UUID, T>> records, ExecutionContext context) throws SQLException {
		ConfigurableResult<T> result = new ConfigurableResult<>();

		if (!records.isEmpty()) {
			InsertStatementBuilder statementBuilder = new InsertStatementBuilder(
					context::generateStatement,
					getName(),
					streamColumns()
							.map(Column::getName)
							.collect(Collectors.toList())
			);
			for (Record<UUID, T> record : records) {
				statementBuilder.batch(buildBatch(record, context));
				result.add(record);
			}
			statementBuilder.build()
					.executeBatch();
		}
		return result;
	}
	private Result<T> update(Collection<Record<UUID, T>> records, ExecutionContext context) throws SQLException {
		ConfigurableResult<T> result = new ConfigurableResult<>();

		if (!records.isEmpty()) {
			UpdateStatementBuilder statementBuilder = new UpdateStatementBuilder(
					context::generateStatement,
					getName(),
					streamColumns(FieldBackedColumn.class)
							.map(Column::getName)
							.collect(Collectors.toList()),
					resolve(Where.eqId(UUID.randomUUID()), context)
			);
			for (Record<UUID, T> record : records) {
				statementBuilder.batch(buildBatch(record, context), resolve(Where.eqId(record.getKey()), context));
				result.add(record);
			}
			statementBuilder.build()
					.executeBatch();
		}
		return result;
	}

	private <R> Where whereRecordsExist(Function<Record<UUID, T>, R> recordMapper, Function<R, Where> whereMapper) {
		return records.stream()
				.map(recordMapper)
				.map(whereMapper)
				.reduce(Where::or)
				.orElseThrow(() -> new IllegalStateException("This should never happen"));
	}

	private Collection<UUID> selectIds(Where where, ExecutionContext context) {
		return select(getType(), where)
				.execute(context)
				.getKeys();
	}
	SelectRequest<?> select(Class<?> c, Where where) {
		return new SelectRequest<>(c, where);
	}
}
