package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.contributor.RecordStatementContributor;
import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Record;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.util.Where;

import java.sql.PreparedStatement;
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
		this(Collections.singleton(new Record<>(id, instance)));
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
		super((Class<T>) records.stream()
				.findFirst()
				.map(Record::getObject)
				.map(Object::getClass)
				.orElseThrow(() -> new IllegalArgumentException("No records supplied")));

		this.records = records;
	}

	@Override
	Result<T> executeThrowing(ExecutionContext context) throws SQLException {
		// Avoid re-inserting existing instances
		Collection<UUID> existing = new SelectRequest<>(getType(), records.stream()
				.map(Record::getObject)
				.map(Where::eqObject)
				.reduce(Where::or)
				.orElseThrow(() -> new IllegalStateException("This should never happen"))
		).execute(context)
				.getIds();

		Collection<Record<UUID, T>> remainingRecords = records.stream()
				.filter(record -> !existing.contains(record.getKey()))
				.collect(Collectors.toSet());

		String sql = "INSERT INTO " + getName() + " "
				+ generateColumns(Column::getName)
				+ " VALUES " + generateColumns(column -> "?");
		logStatements(sql);

		PreparedStatement statement = context.prepareStatement(sql);
		ConfigurableResult<T> result = new ConfigurableResult<>();

		for (Record<UUID, T> record : remainingRecords) {
			forEachColumn(RecordStatementContributor.class,
					(i, column) -> column.contribute(statement, record, i, context));

			statement.addBatch();

			result.add(record);
		}
		statement.executeBatch();

		return result;
	}

	private String generateColumns(Function<Column, String> columnValueMapper) {
		return streamColumns()
				.map(columnValueMapper)
				.collect(Collectors.joining(", ", "(", ")"));
	}
}
