package dev.kkorolyov.sqlob.contributor;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.result.ConfigurableRecord;
import dev.kkorolyov.sqlob.result.Record;

import java.sql.ResultSet;
import java.util.UUID;

/**
 * Contributes data from a {@link ResultSet} to a {@link Record}.
 */
public interface ResultRecordContributor {
	/**
	 * Contributes associated values in a statement to a record.
	 * @param record record to contribute to
	 * @param rs result set to retrieve value from
	 * @param context context to work in
	 * @param <O> record object type
	 * @return {@code record}
	 */
	<O> ConfigurableRecord<UUID, O> contribute(ConfigurableRecord<UUID, O> record, ResultSet rs, ExecutionContext context);
}
