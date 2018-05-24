package dev.kkorolyov.sqlob.type.factory;

import dev.kkorolyov.simplefiles.Providers;
import dev.kkorolyov.sqlob.type.SqlobType;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Provides for retrieval of {@link SqlobType}s by the associated Java type.
 */
public final class SqlobTypeFactory {
	private static final Providers<SqlobType> SQLOB_TYPES = Providers.fromConfig(SqlobType.class);

	private SqlobTypeFactory() {}

	/**
	 * @param value value to get SQLOb type for
	 * @return most appropriate SQLOb type for {@code value}'s type
	 * @see #get(Class)
	 */
	public static SqlobType<?> get(Object value) {
		return get(value.getClass());
	}

	/**
	 * Like {@link #poll(Class)}, but throws an exception if no SQLOb type accepts {@code type}.
	 * @throws NoSuchElementException if no SQLOb type accepts {@code type}
	 */
	public static <T> SqlobType<T> get(Class<T> type) {
		return poll(type)
				.orElseThrow(() -> new NoSuchElementException("No SQLOb type accepts: " + type));
	}
	/**
	 * @param type Java type to convert to SQLOb type.
	 * @return optional containing most appropriate SQLOb type for {@code type}
	 */
	public static <T> Optional<? extends SqlobType<T>> poll(Class<T> type) {
		return SQLOB_TYPES.stream()
				.map(sqlobType -> (SqlobType<?>) sqlobType)  // Cast raw to wildcard
				.filter(sqlobType -> sqlobType.getTypes().stream()
						.anyMatch(acceptedType -> acceptedType.isAssignableFrom(type)))
				.map(sqlobType -> (SqlobType<T>) sqlobType)
				.findFirst();
	}
}
