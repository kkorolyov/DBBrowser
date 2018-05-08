package dev.kkorolyov.sqlob.contributor;

import dev.kkorolyov.sqlob.ExecutionContext;

import java.sql.ResultSet;

/**
 * Contributes data from a {@link ResultSet} to an instance.
 */
public interface ResultInstanceContributor {
	/**
	 * Contributes associated values in a statement to an instance.
	 * @param instance instance to contribute to
	 * @param rs result set to retrieve value from
	 * @param context context to work in
	 * @return {@code instance}
	 */
	Object contribute(Object instance, ResultSet rs, ExecutionContext context);
}
