package dev.kkorolyov.sqlob.result;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

/**
 * A {@link Result} that can be incrementally built/configured.
 */
public class ConfigurableResult<T> implements Result<T> {
	private final Map<UUID, T> records = new HashMap<>();

	/**
	 * @param record record to add
	 * @return {@code this}
	 */
	public ConfigurableResult<T> add(Entry<UUID, T> record) {
		return add(record.getKey(), record.getValue());
	}
	/**
	 * @param id ID of record to add
	 * @param instance instance of record to add
	 * @return {@code this}
	 */
	public ConfigurableResult<T> add(UUID id, T instance) {
		records.put(id, instance);
		return this;
	}

	@Override
	public Map<UUID, T> getRecords() {
		return records;
	}

	@Override
	public Collection<UUID> getIds() {
		return getRecords().keySet();
	}
	@Override
	public Collection<T> getObjects() {
		return getRecords().values();
	}

	@Override
	public Optional<UUID> getId() {
		return getIds().stream()
				.findFirst();
	}
	@Override
	public Optional<T> getObject() {
		return getObjects().stream()
				.findFirst();
	}

	@Override
	public int size() {
		return records.size();
	}

	@Override
	public Optional<Result<T>> asOptional() {
		return getObject()
				.map(o -> this);
	}
}
