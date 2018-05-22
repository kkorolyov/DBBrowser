package dev.kkorolyov.sqlob.request.collection;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.request.Request;
import dev.kkorolyov.sqlob.result.Result;

import java.sql.SQLException;
import java.util.Collection;

public class DeleteCollectionRequest extends Request<Collection<?>> {
	protected DeleteCollectionRequest(Class<Collection<?>> type) {
		super(type);
	}

	@Override
	protected Result<Collection<?>> executeThrowing(ExecutionContext context) throws SQLException {
		return null;
	}
}
