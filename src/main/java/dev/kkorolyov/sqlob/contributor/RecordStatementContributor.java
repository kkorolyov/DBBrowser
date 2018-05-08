package dev.kkorolyov.sqlob.contributor;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.result.Record;

import java.sql.PreparedStatement;
import java.util.UUID;

/**
 * Contributes data from a {@link Record} to a {@link PreparedStatement}.
 */
public interface RecordStatementContributor {
	/**
	 * Contribute's associated values in a record to a statement.
	 * @param statement statement to contribute to
	 * @param record record to retrieve values from
	 * @param index statement index to contribute to
	 * @param context context to work in
	 * @param <O> record object type
	 * @return {@code statement}
	 */
	<O> PreparedStatement contribute(PreparedStatement statement, Record<UUID, O> record, int index, ExecutionContext context);
}
