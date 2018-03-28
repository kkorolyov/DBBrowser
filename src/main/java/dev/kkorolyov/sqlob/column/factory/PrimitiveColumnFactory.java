package dev.kkorolyov.sqlob.column.factory;

import dev.kkorolyov.simplefuncs.function.ThrowingBiFunction;
import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.column.PrimitiveColumn;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Handles fields mappable directly to SQL types.
 */
public class PrimitiveColumnFactory extends BaseColumnFactory {
	private final Map<Class<?>, String> types = new HashMap<>();
	private final Map<Class<?>, Function<?, ?>> converters = new HashMap<>();
	private final Map<Class<?>, BiFunction<ResultSet, String, ?>> extractors = new HashMap<>();

	/**
	 * Constructs a new primitive column factory.
	 */
	public PrimitiveColumnFactory() {
		put("TINYINT", ResultSet::getByte,
				Byte.TYPE, Byte.class);
		put("SMALLINT", ResultSet::getShort,
				Short.TYPE, Short.class);
		put("INTEGER", ResultSet::getInt,
				Integer.TYPE, Integer.class);
		put("BIGINT", ResultSet::getLong,
				Long.TYPE, Long.class);
		put("REAL", ResultSet::getFloat,
				Float.TYPE, Float.class);
		put("DOUBLE", ResultSet::getDouble,
				Double.TYPE, Double.class);
		put("NUMERIC", ResultSet::getBigDecimal,
				BigDecimal.class);

		put("BOOLEAN", ResultSet::getBoolean,
				Boolean.TYPE, Boolean.class);

		put("CHAR(1)", (rs, column) -> {
			String string = rs.getString(column);
			return string == null ? null : string.charAt(0);
		}, Character.TYPE, Character.class);

		put("VARCHAR(1024)", ResultSet::getString,
				String.class);

		put("VARBINARY(1024)", ResultSet::getBytes,
				byte[].class);

		put("DATE", ResultSet::getDate,
				Date.class);
		put("TIME(6)", ResultSet::getTime,
				Time.class);
		put("TIMESTAMP(6)", ResultSet::getTimestamp,
				Timestamp.class);

		put("CHAR(16)", UUID::toString, (rs, column) -> UUID.fromString(rs.getString(column)),
				UUID.class);

		addAll(types.keySet());
	}
	private <T> void put(String sqlType, ThrowingBiFunction<ResultSet, String, T, SQLException> extractor, Class<T>... classes) {
		put(sqlType, Function.identity(), extractor, classes);
	}
	private <T> void put(String sqlType, Function<T, ?> converter, ThrowingBiFunction<ResultSet, String, T, SQLException> extractor, Class<T>... classes) {
		for (Class<?> c : classes) {
			types.put(c, sqlType);
			converters.put(c, converter);
			extractors.put(c, extractor);
		}
	}

	@Override
	public FieldBackedColumn<?> get(Field f) {
		return new PrimitiveColumn<>(f, types.get(f.getType()), extractors.get(f.getType()));
	}
}
