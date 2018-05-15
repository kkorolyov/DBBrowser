package dev.kkorolyov.sqlob.result;

/**
 * A {@link Record} that can be incrementally built/configured.
 * @param <K> key type
 * @param <O> object type
 */
public class ConfigurableRecord<K, O> implements Record<K, O> {
	private K key;
	private O object;

	/**
	 * Constructs an empty record.
	 */
	public ConfigurableRecord() {}
	/**
	 * Constructs a new preconfigured record.
	 * @param key record key
	 * @param object record object
	 */
	public ConfigurableRecord(K key, O object) {
		this.key = key;
		this.object = object;
	}

	@Override
	public K getKey() {
		return key;
	}
	/**
	 * @param key record key
	 * @return {@code this}
	 */
	public ConfigurableRecord<K, O> setKey(K key) {
		this.key = key;
		return this;
	}

	@Override
	public O getObject() {
		return object;
	}
	/**
	 * @param object record object
	 * @return {@code this}
	 */
	public ConfigurableRecord<K, O> setObject(O object) {
		this.object = object;
		return this;
	}
}
