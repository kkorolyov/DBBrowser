package dev.kkorolyov.sqlob.column.handler.factory;

import dev.kkorolyov.sqlob.column.handler.ColumnHandler;

import java.lang.reflect.Field;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Provides for retrieval of {@link ColumnHandler}s by accepted field.
 */
public class ColumnHandlerFactory {
	private static final Iterable<ColumnHandler> COLUMN_FACTORIES = ServiceLoader.load(ColumnHandler.class);

	/**
	 * @param f field to get column handler for
	 * @return most appropriate column handler for {@code f}
	 * @throws NoSuchElementException if no column handler accepts {@code f}
	 */
	public static ColumnHandler get(Field f) {
		return stream()
				.filter(columnHandler -> columnHandler.accepts(f))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("No column handler accepts field " + f + "; known factories: " + COLUMN_FACTORIES));
	}

	/** @return stream over all column factories */
	public static Stream<ColumnHandler> stream() {
		return StreamSupport.stream(COLUMN_FACTORIES.spliterator(), false);
	}
}
