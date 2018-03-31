package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.simplefuncs.stream.Iterables;
import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.column.KeyColumn;
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

	@Override
	Result<T> executeThrowing(ExecutionContext context) throws SQLException {
		for (Column<?> column : Iterables.append(getColumns(), KeyColumn.PRIMARY)) {
			column.contributeToWhere(where, context);
		}
		String sql = getColumns().stream()
				.map(Column::getName)
				.collect(Collectors.joining(", ",
						"SELECT " + KeyColumn.PRIMARY.getName() + ", ",
						" FROM " + getName() + " WHERE " + where.getSql()));
		logStatements(sql);

		PreparedStatement statement = context.prepareStatement(sql);
		ResultSet rs = where.contributeToStatement(statement).executeQuery();

		ConfigurableResult<T> result = new ConfigurableResult<>();

		while (rs.next()) {
			result.add(KeyColumn.PRIMARY.getValue(rs, context), toInstance(rs, context));
		}
		return result;
	}
	private T toInstance(ResultSet rs, ExecutionContext context) {
		try {
			Constructor<T> noArgConstructor = getType().getDeclaredConstructor();
			noArgConstructor.setAccessible(true);

			T instance = noArgConstructor.newInstance();
			for (FieldBackedColumn<?> column : getColumns()) {
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
