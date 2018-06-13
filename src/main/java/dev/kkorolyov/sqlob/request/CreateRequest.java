package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.statement.CreateStatementBuilder;

import java.sql.SQLException;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Request to create a table for a specific class.
 */
public class CreateRequest<T> extends Request<T> {
	/**
	 * Constructs a new create request.
	 * @see Request#Request(Class)
	 */
	public CreateRequest(Class<T> type) {
		super(type);
	}

	/**
	 * Constructs a new create request with custom columns.
	 * @see Request#Request(Class, String, Iterable)
	 */
	CreateRequest(Class<T> type, String name, Iterable<Column<?>> columns) {
		super(type, name, columns);
	}

	@Override
	protected Result<T> executeThrowing(ExecutionContext context) throws SQLException {
		CreateStatementBuilder statementBuilder = new CreateStatementBuilder(context::generateStatement);
		statementBuilder.batch(toTable(context), streamColumns()
				.map(column -> column.getPrerequisites(context))
				.flatMap(Collection::stream)
				.collect(Collectors.toSet()));

		statementBuilder.build().executeBatch();

		return new ConfigurableResult<>();
	}
}
