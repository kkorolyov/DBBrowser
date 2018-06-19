package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.result.ConfigurableRecord;
import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.statement.SelectStatementBuilder;
import dev.kkorolyov.sqlob.util.ReflectionHelper;
import dev.kkorolyov.sqlob.util.Where;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.stream.Collectors;

import static dev.kkorolyov.sqlob.util.Where.eqId;
import static dev.kkorolyov.sqlob.util.Where.eqObject;

/**
 * Request to select records from a class's table.
 */
public class SelectRequest<T> extends Request<T> {
	private final Where where;

	/**
	 * Constructs a select request retrieving an instance.
	 * @param instance instance to match
	 * @see #SelectRequest(Class, Where)
	 */
	public SelectRequest(T instance) {
		this(
				(Class<T>) instance.getClass(),
				eqObject(instance)
		);
	}
	/**
	 * Constructs a select request retrieving by ID.
	 * @param id ID to match
	 * @see #SelectRequest(Class, Where)
	 */
	public SelectRequest(Class<T> type, UUID id) {
		this(
				type,
				eqId(id)
		);
	}
	/**
	 * Constructs a new select request.
	 * @param where selection constraint
	 * @see Request#Request(Class)
	 */
	public SelectRequest(Class<T> type, Where where) {
		super(type);

		this.where = where;
	}

	/**
	 * Constructs a new select request with custom columns.
	 * @see Request#Request(Class, String, Iterable)
	 */
	SelectRequest(Class<T> type, String name, Where where, Iterable<Column<?>> columns) {
		super(type, name, columns);

		this.where = where;
	}

	@Override
	protected Result<T> executeThrowing(ExecutionContext context) throws SQLException {
		ResultSet rs = selectBuilder(context).build()
				.executeQuery();

		ConfigurableResult<T> result = new ConfigurableResult<>();

		while (rs.next()) {
			result.add(streamColumns()
					.reduce(
							new ConfigurableRecord<UUID, T>()
									.setObject(ReflectionHelper.newInstance(getType())),
							(record, column) -> column.set(record, rs, context),
							(record, record1) -> record
					));
		}
		return result;
	}

	SelectStatementBuilder selectBuilder(ExecutionContext context) {
		return new SelectStatementBuilder(
				context::generateStatement,
				getName(),
				streamColumns()
						.map(Column::getName)
						.collect(Collectors.toSet()),
				resolve(where, context)
		);
	}
}
