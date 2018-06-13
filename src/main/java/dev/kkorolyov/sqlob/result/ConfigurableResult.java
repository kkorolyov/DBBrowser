package dev.kkorolyov.sqlob.result;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A {@link Result} that can be incrementally built/configured.
 */
public class ConfigurableResult<T> implements Result<T> {
	private final Collection<Record<UUID, T>> records = new HashSet<>();
	private Integer customSize;

	/**
	 * @param record record to add
	 * @return {@code this}
	 */
	public ConfigurableResult<T> add(Record<UUID, T> record) {
		records.add(record);
		return this;
	}
	/**
	 * @param records records to add
	 * @return {@code this}
	 */
	public ConfigurableResult<T> add(Iterable<Record<UUID, T>> records) {
		records.forEach(this.records::add);
		return this;
	}

	/**
	 * @param size custom size to set, overrides the default of {@code records.size()}
	 * @return {@code this}
	 */
	public ConfigurableResult<T> size(Integer size) {
		customSize = size;
		return this;
	}

	@Override
	public Collection<Record<UUID, T>> getRecords() {
		return records;
	}

	@Override
	public Collection<UUID> getKeys() {
		return getRecords().stream()
				.map(Record::getKey)
				.collect(Collectors.toSet());
	}
	@Override
	public Collection<T> getObjects() {
		return getRecords().stream()
				.map(Record::getObject)
				.collect(Collectors.toSet());
	}

	@Override
	public Optional<UUID> getKey() {
		return getKeys().stream()
				.findFirst();
	}
	@Override
	public Optional<T> getObject() {
		return getObjects().stream()
				.findFirst();
	}

	@Override
	public int size() {
		return customSize != null
				? customSize
				: records.size();
	}

	@Override
	public Optional<Result<T>> asOptional() {
		return getObject()
				.map(o -> this);
	}
}
