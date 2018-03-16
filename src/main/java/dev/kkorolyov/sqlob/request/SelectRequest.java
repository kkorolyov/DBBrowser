package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Result;
import dev.kkorolyov.sqlob.util.Where;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.stream.Collectors;

import static dev.kkorolyov.sqlob.column.Column.ID_COLUMN;
import static dev.kkorolyov.sqlob.util.Where.eqId;
import static dev.kkorolyov.sqlob.util.Where.eqObject;

/**
 * Request to select data from a class's table.
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

	@Override
	Result<T> executeInContext(ExecutionContext context) throws SQLException {
		for (Column<?> column : getColumns()) {
			column.contributeToWhere(where, context);
		}
		PreparedStatement statement = context.getConnection().prepareStatement(getColumns().stream()
				.map(Column::getName)
				.collect(Collectors.joining(", ",
						"SELECT " + ID_COLUMN + ", ",
						" FROM " + getName() + " WHERE " + where)));

		logStatements(statement.toString());
		ResultSet rs = where.contributeToStatement(statement).executeQuery();

		ConfigurableResult<T> result = new ConfigurableResult<>();

		while (rs.next()) {
			result.add(ID_COLUMN.getValue(rs, context), toInstance(rs, context));
		}
		return result;
	}
	private T toInstance(ResultSet rs, ExecutionContext context) {
		try {
			Constructor<T> noArgConstructor = getType().getConstructor();
			noArgConstructor.setAccessible(true);

			T instance = noArgConstructor.newInstance();
			for (Column<?> column : getColumns()) {
				column.contributeToInstance(instance, rs, context);
			}
			return instance;
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(getType() + " does not provide a no-arg constructor");
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
