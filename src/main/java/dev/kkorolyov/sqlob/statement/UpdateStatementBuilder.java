package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.sqlob.util.Where;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds {@code UPDATE} statements.
 */
public class UpdateStatementBuilder implements StatementBuilder<PreparedStatement> {
	private final Function<String, PreparedStatement> statementSupplier;
	private final String table;
	private final List<String> columns;
	private final Where where;
	private final Batcher<String, Object> batcher;
	private final List<Where> wheres = new ArrayList<>();

	/**
	 * Constructs a new {@code UPDATE} statement builder.
	 * @param statementSupplier provides a prepared statement from a SQL string supplied to it.
	 * @param table name of table to update
	 * @param columns names of columns to update values of
	 * @param where template WHERE clause to update batches at
	 */
	public UpdateStatementBuilder(Function<String, PreparedStatement> statementSupplier, String table, List<String> columns, Where where) {
		this.statementSupplier = statementSupplier;
		this.table = table;
		this.columns = new ArrayList<>(columns);
		this.where = where;
		batcher = new Batcher<>(columns);
	}

	/**
	 * Adds a batch of values to update and relevant WHERE clause to this builder.
	 * @param batch {@code {columnName, columnValue}} pairs to add as a batch to this builder
	 * @param where criteria to update the given batch at
	 * @return {@code this}
	 * @throws IllegalArgumentException if any key in {@code batch} does not match an existing column in this builder
	 */
	public UpdateStatementBuilder batch(Map<String, Object> batch, Where where) {
		batcher.batch(batch);
		wheres.add(where);

		return this;
	}

	@Override
	public PreparedStatement build() {
		PreparedStatement statement = statementSupplier.apply(
				"UPDATE " + table + " SET "
						+ buildColumns()
						+ " WHERE " + where.getSql());

		batcher.forEach(
				(i, value) -> statement.setObject(i + 1, value),
				batchIndex -> {
					wheres.get(batchIndex)
							.forEach((i, value) -> statement.setObject(i + 1 + columns.size(), value));
					statement.addBatch();
				}
		);

		return statement;
	}
	private String buildColumns() {
		return columns.stream()
				.map(column -> column + "=?")
				.collect(Collectors.joining(","));
	}
}
