package dev.kkorolyov.sqlob.request.collection;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.request.CreateRequest;
import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Result;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Request to create a table for a collection field.
 */
public class CreateCollectionRequest extends CollectionRequest {
	/**
	 * Constructs a new create collection request.
	 * @param f collection field to create collection table for
	 * @throws IllegalArgumentException if {@code f}'s type is not a {@link Collection}
	 */
	public CreateCollectionRequest(Field f) {
		super(f);
	}

	@Override
	protected Result<Collection<?>> executeThrowing(ExecutionContext context) throws SQLException {
		create(getParameterType()).execute(context);

		String sql = streamColumns()
				.map(column -> column.getSql(context))
				.collect(Collectors.joining(", ",
						"CREATE TABLE IF NOT EXISTS " + getName() + " (",
						")"));

		logStatements(sql);

		Statement statement = context.generateStatement();
		statement.execute(sql);

		return new ConfigurableResult<>();
	}

	CreateRequest<?> create(Class<?> c) {
		return new CreateRequest<>(c);
	}
}
