package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.simplegraphs.Graph;
import dev.kkorolyov.sqlob.struct.Column;
import dev.kkorolyov.sqlob.struct.Table;

import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static dev.kkorolyov.sqlob.util.UncheckedSqlException.wrapSqlException;

/**
 * Builds {@code CREATE} statements.
 */
public class CreateStatementBuilder implements StatementBuilder<Statement> {
	private final Supplier<Statement> statementSupplier;
	private final Graph<Table> tables = new Graph<>();
	private final Map<Table, String> creates = new HashMap<>();

	/**
	 * Constructs a new {@code CREATE} statement builder.
	 * @param statementSupplier provides a statement
	 */
	public CreateStatementBuilder(Supplier<Statement> statementSupplier) {
		this.statementSupplier = statementSupplier;
	}

	/**
	 * Adds a table to create to builder which has no prerequisites.
	 * @see #batch(Table, Iterable)
	 */
	public CreateStatementBuilder batch(Table table) {
		return batch(table, Collections.emptySet());
	}
	/**
	 * Adds a table to create to this builder.
	 * @param table table to create
	 * @param prerequisiteTables tables which must be created before {@code table}
	 * @return {@code this}
	 */
	public CreateStatementBuilder batch(Table table, Iterable<Table> prerequisiteTables) {
		tables.add(table);
		for (Table prerequisite : prerequisiteTables) {
			tables.add(prerequisite, table);
			batch(prerequisite);
		}
		creates.put(table, table.getColumns().stream()
				.map(Column::getSql)
				.collect(Collectors.joining(
						",",
						"CREATE TABLE IF NOT EXISTS " + table.getName() + " (",
						")"
				)));
		return this;
	}

	@Override
	public Statement build() {
		Statement statement = statementSupplier.get();

		tables.sortTopological().stream()
				.map(creates::get)
				.filter(Objects::nonNull)
				.forEach(create -> wrapSqlException(() -> statement.addBatch(create)));

		return statement;
	}
}
