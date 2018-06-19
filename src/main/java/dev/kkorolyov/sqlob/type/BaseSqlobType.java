package dev.kkorolyov.sqlob.type;

import dev.kkorolyov.simplefuncs.function.ThrowingBiFunction;
import dev.kkorolyov.simplefuncs.stream.Iterables;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Function;

import static dev.kkorolyov.sqlob.util.UncheckedSqlException.wrapSqlException;

/**
 * A {@link SqlobType} composed of other database-specific SQLOb types.
 * @param <T> associated base Java type
 */
public abstract class BaseSqlobType<T> implements SqlobType<T> {
	/** Key for the default, database-agnostic SQLOb type */
	protected static final String DEFAULT = "SQL";

	private final Collection<Class<T>> acceptedTypes = new HashSet<>();
	private final Map<String, SqlobType<T>> delegates = new HashMap<>();

	/**
	 * Adds a database-SQLObType mapping.
	 * @param database associated database
	 * @param delegate SQLOb type to associate with {@code database}
	 */
	protected void put(String database, SqlobType<T> delegate) {
		acceptedTypes.addAll(delegate.getTypes());
		delegates.put(database, delegate);
	}

	@Override
	public Collection<Class<T>> getTypes() {
		return acceptedTypes;
	}
	@Override
	public String getSqlType(DatabaseMetaData metaData) {
		return wrapSqlException(() -> getDelegate(metaData)
				.getSqlType(metaData));
	}

	@Override
	public Object get(DatabaseMetaData metaData, T value) {
		return wrapSqlException(() -> getDelegate(metaData)
				.get(metaData, value));
	}
	@Override
	public T get(DatabaseMetaData metaData, ResultSet rs, String column) {
		return wrapSqlException(() -> getDelegate(metaData)
				.get(metaData, rs, column));
	}

	private SqlobType<T> getDelegate(DatabaseMetaData metaData) throws SQLException {
		SqlobType<T> delegate = delegates.getOrDefault(metaData.getDatabaseProductName(), delegates.get(DEFAULT));
		if (delegate == null) throw new NoSuchElementException("No SQLOb type for types [" + acceptedTypes + "] associated with database: " + metaData.getDatabaseProductName());

		return delegate;
	}

	/**
	 * Delegate SQLOb type which provides the actual logic.
	 */
	protected static class Delegate<T> implements SqlobType<T> {
		private final Collection<Class<T>> acceptedTypes = new HashSet<>();
		private final String sqlType;
		private final Function<? super T, ?> converter;
		private final BiFunction<ResultSet, String, T> extractor;

		/** @see #Delegate(String, ThrowingBiFunction, Iterable) */
		@SafeVarargs
		public Delegate(String sqlType, ThrowingBiFunction<ResultSet, String, T, SQLException> extractor, Class<T> type, Class<T>... types) {
			this(sqlType, extractor, Iterables.append(Collections.singleton(type), types));
		}
		/** @see #Delegate(String, Function, ThrowingBiFunction, Iterable) */
		@SafeVarargs
		public Delegate(String sqlType, Function<T, ?> converter, ThrowingBiFunction<ResultSet, String, T, SQLException> extractor, Class<T> type, Class<T>... types) {
			this(sqlType, converter, extractor, Iterables.append(Collections.singleton(type), types));
		}

		/**
		 * Constructs a new delegate SQLOb type with no conversion.
		 * @see #Delegate(String, Function, ThrowingBiFunction, Iterable)
		 */
		public Delegate(String sqlType, ThrowingBiFunction<ResultSet, String, T, SQLException> extractor, Iterable<Class<T>> types) {
			this(sqlType, Function.identity(), extractor, types);
		}
		/**
		 * Constructs a new simple SQLOb type.
		 * @param sqlType associated SQL type
		 * @param converter converts values for persistence
		 * @param extractor extracts value from result set
		 * @param types accepted types
		 */
		public Delegate(String sqlType, Function<? super T, ?> converter, ThrowingBiFunction<ResultSet, String, T, SQLException> extractor, Iterable<Class<T>> types) {
			this.sqlType = sqlType;
			this.converter = converter;
			this.extractor = extractor;
			types.forEach(acceptedTypes::add);
		}

		@Override
		public Collection<Class<T>> getTypes() {
			return acceptedTypes;
		}
		@Override
		public String getSqlType(DatabaseMetaData metaData) {
			return sqlType;
		}

		@Override
		public Object get(DatabaseMetaData metaData, T value) {
			return converter.apply(value);
		}
		@Override
		public T get(DatabaseMetaData metaData, ResultSet rs, String column) {
			return extractor.apply(rs, column);
		}
	}
}
