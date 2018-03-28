package dev.kkorolyov.sqlob.column;

import dev.kkorolyov.sqlob.request.ExecutionContext;
import dev.kkorolyov.sqlob.util.Where;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * @param <T>
 */
public class PrimitiveColumn<T> extends FieldBackedColumn<T> {
	private final BiFunction<ResultSet, String, T> extractor;

	/**
	 * Constructs a new primitive field column.
	 * @param extractor function extracting this column's SQL value from a result set and converting it into a Java value; invoked with result set and column name
	 * @see FieldBackedColumn#FieldBackedColumn(Field, String)
	 */
	public PrimitiveColumn(Field f, String sqlType, BiFunction<ResultSet, String, T> extractor) {
		super(f, sqlType);
		this.extractor = extractor;
	}

	@Override
	public Where contributeToWhere(Where where, ExecutionContext context) {
		where.resolve(getName(), UnaryOperator.identity());
		return where;
	}

	@Override
	public T getValue(Object instance, ExecutionContext context) {
		return (T) super.getValue(instance, context);
	}
	@Override
	public T getValue(ResultSet rs, ExecutionContext context) throws SQLException {
		try {
			return extractor.apply(rs, getName());
		} catch (RuntimeException e) {
			if (e.getCause() instanceof SQLException) throw (SQLException) e.getCause();
			else throw e;
		}
	}
}
