package dev.kkorolyov.sqlob.type.factory;

import dev.kkorolyov.sqlob.type.SqlobType;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

/**
 * Provides for retrieval of {@link SqlobType}'s by the associated Java type.
 */
public class SqlobTypeFactory {
	public static final Iterable<SqlobType> sqlobTypes = ServiceLoader.load(SqlobType.class);

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
	public static <T> SqlobType<? super T> get(Class<T> type) {
		return poll(type)
				.orElseThrow(() -> new NoSuchElementException("No SQLOb type accepts: " + type));
	}
	/**
	 * @param type Java type to convert to SQLOb type.
	 * @return optional containing most appropriate SQLOb type for {@code type}
	 */
	public static <T> Optional<? extends SqlobType<? super T>> poll(Class<T> type) {
		return StreamSupport.stream(sqlobTypes.spliterator(), false)
				.map(sqlobType -> (SqlobType<?>) sqlobType)  // Cast raw to wildcard
				.filter(sqlobType -> sqlobType.getTypes().stream()
						.anyMatch(acceptedType -> acceptedType.isAssignableFrom(type)))
				.map(sqlobType -> (SqlobType<? super T>) sqlobType)
				.findFirst();
	}
}
