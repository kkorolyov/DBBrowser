package dev.kkorolyov.sqlob.result;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * A persistable {@code {uniqueKey, object}} combination.
 * @param <K> key type
 * @param <O> object type
 */
public class Record<K, O> {
	private final K key;
	private final O object;

	/**
	 * Constructs a new record.
	 * @param key record key
	 * @param object record object
	 */
	public Record(K key, O object) {
		this.key = key;
		this.object = object;
	}

	/**
	 * @param keyGenerator supplies a unique key for each collected object
	 * @param <K> record key type
	 * @param <V> record object type
	 * @return collector which collects objects into records
	 */
	public static <K, V> Collector<V, ?, Collection<Record<K, V>>> collector(Function<V, K> keyGenerator) {
		return Collector.of(
				HashSet::new,
				(records, object) -> records.add(new Record<K, V>(keyGenerator.apply(object), object)),
				(set1, set2) -> {
					set1.addAll(set2);
					return set1;
				}
		);
	}

	/** @return record key */
	public K getKey() {
		return key;
	}
	/** @return record object */
	public O getObject() {
		return object;
	}
}
