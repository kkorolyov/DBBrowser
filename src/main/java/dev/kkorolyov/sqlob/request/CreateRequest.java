package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.column.handler.factory.ColumnHandlerFactory;
import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Result;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
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
	Result<T> executeThrowing(ExecutionContext context) throws SQLException {
		List<String> sql = ColumnHandlerFactory.stream()
				.flatMap(columnHandler -> columnHandler.expandCreates(this))
				.map(createRequest -> createRequest.getCreateStatement(context))
				.collect(Collectors.toList());

		logStatements(sql);

		Statement statement = context.generateStatement();
		for (String s : sql) statement.addBatch(s);
		statement.executeBatch();

		return new ConfigurableResult<>();
	}

	private String getCreateStatement(ExecutionContext context) {
		return streamColumns()
				.map(column -> column.getSql(context))
				.collect(Collectors.joining(", ",
						"CREATE TABLE IF NOT EXISTS " + getName() + " (",
						")"));
	}
}
