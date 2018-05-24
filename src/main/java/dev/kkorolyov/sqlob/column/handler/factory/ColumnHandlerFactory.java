package dev.kkorolyov.sqlob.column.handler.factory;

import dev.kkorolyov.simplefiles.Providers;
import dev.kkorolyov.sqlob.column.handler.ColumnHandler;

import java.lang.reflect.Field;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * Provides for retrieval of {@link ColumnHandler}s by accepted field.
 */
public final class ColumnHandlerFactory {
	private static final Providers<ColumnHandler> COLUMN_FACTORIES = Providers.fromConfig(ColumnHandler.class);

	private ColumnHandlerFactory() {}

	/**
	 * @param f field to get column handler for
	 * @return most appropriate column handler for {@code f}
	 * @throws NoSuchElementException if no column handler accepts {@code f}
	 */
	public static ColumnHandler get(Field f) {
		return COLUMN_FACTORIES.get(columnHandler -> columnHandler.accepts(f));
	}

	/** @return stream over all column factories */
	public static Stream<ColumnHandler> stream() {
		return COLUMN_FACTORIES.stream();
	}
}
