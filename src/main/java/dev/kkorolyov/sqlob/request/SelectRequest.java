package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.contributor.ResultRecordContributor;
import dev.kkorolyov.sqlob.contributor.WhereStatementContributor;
import dev.kkorolyov.sqlob.result.ConfigurableRecord;
import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.util.ReflectionHelper;
import dev.kkorolyov.sqlob.util.Where;

import java.sql.PreparedStatement;
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
		this((Class<T>) instance.getClass(),
				eqObject(instance));
	}
	/**
	 * Constructs a select request retrieving by ID.
	 * @param id ID to match
	 * @see #SelectRequest(Class, Where)
	 */
	public SelectRequest(Class<T> type, UUID id) {
		this(type,
				eqId(id));
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
	Result<T> executeThrowing(ExecutionContext context) throws SQLException {
		String sql = streamColumns()
				.map(Column::getName)
				.collect(Collectors.joining(", ",
						"SELECT " + KeyColumn.ID.getName() + ", ",
						" FROM " + getName() + " WHERE " + where.getSql()));
		logStatements(sql.replace(where.getSql(), where.toString()));

		PreparedStatement statement = context.generateStatement(sql);

		streamColumns(WhereStatementContributor.class)
				.forEach(column -> column.contribute(statement, where, context));

		ResultSet rs = statement.executeQuery();

		ConfigurableResult<T> result = new ConfigurableResult<>();

		while (rs.next()) {
			ConfigurableRecord<UUID, T> record = new ConfigurableRecord<UUID, T>()
					.setObject(ReflectionHelper.newInstance(getType()));

			streamColumns(ResultRecordContributor.class)
					.forEach(column -> column.contribute(record, rs, context));

			result.add(record);
		}
		return result;
	}
}
