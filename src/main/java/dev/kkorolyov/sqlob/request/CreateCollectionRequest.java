package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.util.PersistenceHelper;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CreateCollectionRequest extends Request<Collection<?>> {
	private final KeyColumn parent;
	private final KeyColumn child;

	/**
	 * Constructs a new create collection request.
	 * @param f collection field to create collection table for
	 * @throws IllegalArgumentException if {@code f}'s type is not a {@link Collection}
	 */
	CreateCollectionRequest(Field f) {
		this(f, KeyColumn.parent(PersistenceHelper.getName(f.getDeclaringClass())), KeyColumn.child(PersistenceHelper.getName(f.getType())));
	}
	private CreateCollectionRequest(Field f, KeyColumn parent, KeyColumn child) {
		super(verifyType(f), parent + "_" + PersistenceHelper.getName(f));

		this.parent = parent;
		this.child = child;
	}
	private static Class<Collection<?>> verifyType(Field f) {
		if (!Collection.class.isAssignableFrom(f.getType())) {
			throw new IllegalArgumentException(f.getType() + " must be a collection");
		}
		return (Class<Collection<?>>) f.getType();
	}

	@Override
	Result<Collection<?>> executeThrowing(ExecutionContext context) throws SQLException {
		String sql = Stream.of(
				parent,
				child
		).map(column -> column.getSql(context))
				.collect(Collectors.joining(", ",
						"CREATE TABLE IF NOT EXISTS " + getName() + " (",
						")"));

		logStatements(sql);

		Statement statement = context.generateStatement();
		statement.execute(sql);

		return new ConfigurableResult<>();
	}
}
