package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.contributor.WhereStatementContributor;
import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.util.Where;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Request to delete records from a class's table.
 * Result contains number of deleted records.
 */
public class DeleteRequest<T> extends Request<T> {
	private final Where where;

	/** @see #DeleteRequest(Iterable) */
	@SafeVarargs
	public DeleteRequest(T... instances) {
		this(Arrays.asList(instances));
	}
	/**
	 * Constructs a delete request deleting instances.
	 * @param instances instances to delete
	 * @throws NoSuchElementException if {@code instances} is empty
	 */
	public DeleteRequest(Iterable<T> instances) {
		this((Class<T>) instances.iterator().next().getClass(),
				StreamSupport.stream(instances.spliterator(), false)
						.map(Where::eqObject)
						.reduce(Where::or)
						.orElseThrow(() -> new NoSuchElementException("No instances specified")));
	}

	/** @see #DeleteRequest(Class, Iterable) */
	public DeleteRequest(Class<T> type, UUID... ids) {
		this(type, Arrays.asList(ids));
	}
	/**
	 * Constructs a delete request deleting by IDs.
	 * @param ids IDs to delete
	 * @throws java.util.NoSuchElementException if {@code ids} is empty
	 */
	public DeleteRequest(Class<T> type, Iterable<UUID> ids) {
		this(type,
				StreamSupport.stream(ids.spliterator(), false)
						.map(Where::eqId)
						.reduce(Where::or)
						.orElseThrow(() -> new NoSuchElementException("No IDs specified")));
	}

	/**
	 * Constructs a new delete request.
	 * @param where deletion constraint
	 * @see Request#Request(Class)
	 */
	public DeleteRequest(Class<T> type, Where where) {
		super(type);

		this.where = where;
	}

	/**
	 * Constructs a new delete request with custom columns.
	 * @see Request#Request(Class, String, Iterable)
	 */
	DeleteRequest(Class<T> type, String name, Where where, Iterable<Column<?>> columns) {
		super(type, name, columns);

		this.where = where;
	}

	@Override
	Result<T> executeThrowing(ExecutionContext context) throws SQLException {
		String sql = "DELETE FROM " + getName() + " WHERE " + where.getSql();
		logStatements(sql);

		PreparedStatement statement = context.generateStatement(sql);

		streamColumns(WhereStatementContributor.class)
				.forEach(column -> column.contribute(statement, where, context));

		int updated = statement.executeUpdate();

		return new ConfigurableResult<T>()
				.size(updated);
	}
}
