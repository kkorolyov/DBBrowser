package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Result;

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

import static dev.kkorolyov.sqlob.column.Column.ID_COLUMN;

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
		this(UUID.randomUUID(), instance);
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
	public InsertRequest(Collection<T> instances) {
		this(instances.stream()
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
	Result<T> executeInContext(ExecutionContext context) throws SQLException {
		PreparedStatement statement = context.getConnection().prepareStatement(
				"INSERT INTO " + getName() + " "
						+ generateColumns(Column::getName)
						+ " VALUES " + generateColumns(column -> "?"));

		logStatements(statement.toString());

		ConfigurableResult<T> result = new ConfigurableResult<>();

		for (Entry<UUID, T> record : records.entrySet()) {
			int index = 0;

			// TODO Abstract this UUID conversion
			statement.setObject(index++, record.getKey().toString());
			for (Column<?> column : getColumns()) {
				column.contributeToStatement(statement, record.getValue(), index++, context);
			}
			statement.addBatch();

			result.add(record);
		}
		statement.executeUpdate();

		return result;
	}

	private String generateColumns(Function<Column, String> columnValueMapper) {
		return Stream.concat(
				Stream.of(ID_COLUMN),
				getColumns().stream()
		).map(columnValueMapper)
				.collect(Collectors.joining(", ", "(", ")"));
	}
}
