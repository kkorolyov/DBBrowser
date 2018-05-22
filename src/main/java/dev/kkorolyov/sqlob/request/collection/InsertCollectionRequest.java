package dev.kkorolyov.sqlob.request.collection;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.result.Result;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Collection;

public class InsertCollectionRequest extends CollectionRequest {
	protected InsertCollectionRequest(Field f) {
		super(f);
	}

	@Override
	protected Result<Collection<?>> executeThrowing(ExecutionContext context) throws SQLException {
		return null;
	}
}
