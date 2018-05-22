package dev.kkorolyov.sqlob.contributor;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.util.Where;

import java.sql.PreparedStatement;

/**
 * Contributes data from a {@link Where} to a {@link PreparedStatement}
 */
public interface WhereStatementContributor {
	/**
	 * Contributes associated values in {@code where} to {@code statement}.
	 * @param statement statement to contribute to
	 * @param where where to resolve values from
	 * @param context context to work in
	 * @return {@code statement}
	 */
	PreparedStatement contribute(PreparedStatement statement, Where where, ExecutionContext context);
}
