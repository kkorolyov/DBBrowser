package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.util.Where;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import static dev.kkorolyov.sqlob.util.Where.eqId;
import static dev.kkorolyov.sqlob.util.Where.eqObject;

/**
 * Request to delete class instances from a table.
 * Result contains number of deleted records.
 */
public class DeleteRequest<T> extends Request<T> {
	private final Where where;

	/**
	 * Constructs a delete request deleting an instance.
	 * @param instance instance to delete
	 */
	public DeleteRequest(T instance) {
		this((Class<T>) instance.getClass(),
				eqObject(instance));
	}
	/**
	 * Constructs a delete request deleting by ID.
	 * @param id ID to delete
	 * @see #DeleteRequest(Class, Where)
	 */
	public DeleteRequest(Class<T> type, UUID id) {
		this(type,
				eqId(id));
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

	@Override
	Result<T> executeInContext(ExecutionContext context) throws SQLException {
		PreparedStatement statement = context.getConnection().prepareStatement(
				"DELETE FROM " + getName()
				+ " WHERE " + where);

		int updated = where.contributeToStatement(statement).executeUpdate();

		return new ConfigurableResult<T>()
				.size(updated);
	}
}
