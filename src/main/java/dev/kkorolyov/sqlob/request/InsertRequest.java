package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.util.Where;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Request to insert records of a class as table rows.
 */
public class InsertRequest<T> extends Request<T> {
	private final Map<UUID, T> records;

	/**
	 * Constructs a single-instance insert request with random ID.
	 * @see #InsertRequest(UUID, Object)
	 */
	public InsertRequest(T instance) {
		this(Collections.singleton(instance));
	}
	/**
	 * Constructs a single-instance insert request.
	 * @see #InsertRequest(Map)
	 */
	public InsertRequest(UUID id, T instance) {
		this(Collections.singletonMap(id, instance));
	}
	/**
	 * Constructs an insert request with random IDs.
	 * @see #InsertRequest(Map)
	 */
	public InsertRequest(Iterable<T> instances) {
		this(StreamSupport.stream(instances.spliterator(), false)
				.collect(Collectors.toMap(instance -> UUID.randomUUID(), Function.identity())));
	}
	/**
	 * Constructs a new insert request.
	 * @param records instances to insert mapped by their IDs
	 * @throws IllegalArgumentException if {@code records} is empty
	 * @see Request#Request(Class)
	 */
	public InsertRequest(Map<UUID, T> records) {
		super((Class<T>) records.values().stream()
				.findFirst()
				.map(T::getClass)
				.orElseThrow(() -> new IllegalArgumentException("No records supplied")));

		this.records = records;
	}

	@Override
	Result<T> executeThrowing(ExecutionContext context) throws SQLException {
		// Avoid re-inserting existing instances
		Collection<UUID> existing = new SelectRequest<>(getType(), records.values().stream()
				.map(Where::eqObject)
				.reduce(Where::or)
				.orElseThrow(() -> new IllegalStateException("This should never happen"))
		).execute(context)
				.getIds();

		Map<UUID, T> remainingRecords = records.entrySet().stream()
				.filter(entry -> !existing.contains(entry.getKey()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));

		String sql = "INSERT INTO " + getName() + " "
				+ generateColumns(Column::getName)
				+ " VALUES " + generateColumns(column -> "?");
		logStatements(sql);

		PreparedStatement statement = context.prepareStatement(sql);
		ConfigurableResult<T> result = new ConfigurableResult<>();

		for (Entry<UUID, T> record : remainingRecords.entrySet()) {
			int index = 1;

			statement.setObject(index++, record.getKey());
			for (FieldBackedColumn<?> column : getColumns()) {
				column.contributeToStatement(statement, record.getValue(), index++, context);
			}
			statement.addBatch();

			result.add(record);
		}
		statement.executeBatch();

		return result;
	}

	private String generateColumns(Function<Column, String> columnValueMapper) {
		return Stream.concat(
				Stream.of(KeyColumn.PRIMARY),
				getColumns().stream()
		).map(columnValueMapper)
				.collect(Collectors.joining(", ", "(", ")"));
	}
}
