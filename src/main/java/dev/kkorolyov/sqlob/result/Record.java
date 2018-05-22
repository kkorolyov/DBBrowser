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
public interface Record<K, O> {
	/**
	 * @param keyGenerator supplies a unique key for each collected object
	 * @param <K> record key type
	 * @param <O> record object type
	 * @return collector which collects objects into records
	 */
	static <K, O> Collector<O, ?, Collection<Record<K, O>>> collector(Function<O, K> keyGenerator) {
		return Collector.of(
				HashSet::new,
				(records, object) -> records.add(new ConfigurableRecord<K, O>()
						.setKey(keyGenerator.apply(object))
						.setObject(object)),
				(set1, set2) -> {
					set1.addAll(set2);
					return set1;
				}
		);
	}

	/** @return record key */
	K getKey();
	/** @return record object */
	O getObject();
}
