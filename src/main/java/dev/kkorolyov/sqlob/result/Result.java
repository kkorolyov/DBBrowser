package dev.kkorolyov.sqlob.result;

import dev.kkorolyov.sqlob.request.Request;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * A result of a persistence or retrieval {@link Request}.
 * @param <T> response object type
 */
public interface Result<T> {
	/** @return all relevant records */
	Collection<Record<UUID, T>> getRecords();

	/** @return persisted record IDs */
	Collection<UUID> getKeys();
	/** @return persisted record objects */
	Collection<T> getObjects();

	/** @return ID of first record, if any */
	Optional<UUID> getKey();
	/** @return ID of first record, if any */
	Optional<T> getObject();

	/** @return number of records */
	int size();

	/** @return optional containing this result if it has at least one record, else an empty optional */
	Optional<Result<T>> asOptional();
}
