package dev.kkorolyov.sqlob.util;

import dev.kkorolyov.simplefuncs.throwing.ThrowingFunction;
import dev.kkorolyov.simplefuncs.throwing.ThrowingRunnable;
import dev.kkorolyov.simplefuncs.throwing.ThrowingSupplier;

import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Wraps a checked {@link SQLException}.
 */
public class UncheckedSqlException extends RuntimeException {
	/**
	 * Constructs a new wrapped sql exception.
	 * @param e SQL exception to wrap
	 */
	public UncheckedSqlException(SQLException e) {
		super(e);
	}

	/**
	 * Invokes a runnable which may throw a {@link SQLException}.
	 * If such an exception is thrown, it is wrapped and rethrown as an {@link UncheckedSqlException}.
	 * @param runnable runnable to invoke
	 * @throws UncheckedSqlException if {@code runnable} threw a {@link SQLException}
	 */
	public static void wrapSqlException(ThrowingRunnable<SQLException> runnable) {
		try {
			runnable.runThrowing();
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}
	}
	/**
	 * Invokes a supplier which may throw a {@link SQLException}.
	 * If such an exception is thrown, it is wrapped and rethrown as an {@link UncheckedSqlException}.
	 * @param supplier supplier to invoke
	 * @param <T> supplied type
	 * @return supplied result
	 * @throws UncheckedSqlException if {@code supplier} threw a {@link SQLException}
	 */
	public static <T> T wrapSqlException(ThrowingSupplier<T, SQLException> supplier) {
		try {
			return supplier.getThrowing();
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}
	}

	/**
	 * Invokes (with resources) a function which may throw a {@link SQLException}.
	 * If such an exception is thrown, it is wrapped and rethrown as an {@link UncheckedSqlException}.
	 * If the auto-closeable resource throws an exception, it is wrapped and rethrown as a {@link RuntimeException}.
	 * @param resourceSupplier supplies auto-closeable function resource
	 * @param function function to invoke with resource
	 * @param <T> returned type
	 * @param <R> resource type
	 * @return function result
	 * @throws UncheckedSqlException if {@code function} threw a {@link SQLException}
	 */
	public static <T, R extends AutoCloseable> T wrapSqlException(Supplier<R> resourceSupplier, ThrowingFunction<R, T, SQLException> function) {
		try (R resource = resourceSupplier.get()) {
			return function.applyThrowing(resource);
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
