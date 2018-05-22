package dev.kkorolyov.sqlob.request.collection;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.util.Where;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Request to select collection records from a field's table.
 */
public class SelectCollectionRequest extends CollectionRequest {
	private final Where where;

	/**
	 * Constructs a new select collection request.
	 * @param where selection constraint
	 * @see CollectionRequest#CollectionRequest(Field)
	 */
	public SelectCollectionRequest(Field f, Where where) {
		super(f);

		this.where = where;
	}

	@Override
	protected Result<Collection<?>> executeThrowing(ExecutionContext context) throws SQLException {
		return null;
	}
}
