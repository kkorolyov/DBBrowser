package dev.kkorolyov.sqlob.statement;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds {@code INSERT} statements.
 */
public class InsertStatementBuilder implements StatementBuilder<PreparedStatement> {
	private final Function<String, PreparedStatement> statementSupplier;
	private final String table;
	private final List<String> columns;
	private final Batcher<String, Object> batcher;

	/**
	 * Constructs a new {@code INSERT} statement builder.
	 * @param statementSupplier provides a prepared statement from a SQL string supplied to it
	 * @param table name of table to insert into
	 * @param columns names of columns to insert values into
	 */
	public InsertStatementBuilder(Function<String, PreparedStatement> statementSupplier, String table, List<String> columns) {
		this.statementSupplier = statementSupplier;
		this.table = table;
		this.columns = new ArrayList<>(columns);
		batcher = new Batcher<>(columns);
	}

	/**
	 * Adds a batch of values to insert to this builder.
	 * @param batch {@code {columnName, columnValue}} pairs to add as a batch to this builder
	 * @return {@code this}
	 * @throws IllegalArgumentException if any key in {@code batch} does not match a column set in this builder
	 */
	public InsertStatementBuilder batch(Map<String, Object> batch) {
		batcher.batch(batch);
		return this;
	}

	@Override
	public PreparedStatement build() {
		PreparedStatement statement = statementSupplier.apply(
				"INSERT INTO " + table + " "
						+ buildColumns(Function.identity())
						+ " VALUES " + buildColumns(column -> "?"));

		batcher.forEach((i, value) -> statement.setObject(i + 1, value),
				batchIndex -> statement.addBatch());

		return statement;
	}
	private String buildColumns(Function<String, String> nameMapper) {
		return columns.stream()
				.map(nameMapper)
				.collect(Collectors.joining(",", "(", ")"));
	}
}
