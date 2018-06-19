package dev.kkorolyov.sqlob.statement;

import dev.kkorolyov.simplefuncs.function.ThrowingBiConsumer;
import dev.kkorolyov.simplefuncs.function.ThrowingConsumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maintains ordered batches of values with common keys.
 * @param <K> batch key type
 * @param <V> batch value type
 */
public class Batcher<K, V> {
	private final List<K> keys;
	private final List<List<V>> batches = new ArrayList<>();

	/**
	 * Constructs a new batcher for a list of keys.
	 * @param keys keys common throughout all batches.
	 */
	public Batcher(List<K> keys) {
		this.keys = new ArrayList<>(keys);
	}

	/**
	 * Adds a batch to this batcher.
	 * @param batch {@code {key, value}} pairs to add as a batch to this batcher
	 * @return {@code this}
	 * @throws IllegalArgumentException if any key in {@code batch} is not in the list of keys set in this batcher
	 */
	public Batcher batch(Map<K, V> batch) {
		List<V> orderedBatch = new ArrayList<>(Collections.nCopies(keys.size(), null));
		Collections.fill(orderedBatch, null);

		batch.forEach((key, value) -> {
			int i = keys.indexOf(key);
			// if (i < 0) throw new IllegalArgumentException("Unknown key: " + key + "; known keys" + keys);
			if (i >= 0) orderedBatch.set(i, value);
		});
		batches.add(orderedBatch);

		return this;
	}

	/**
	 * Invokes a given action on each value in each batch, and a finalizer at the end of each batch.
	 * @param action action invoked with {@code (index, value)} of each value in each batch
	 * @param finalizer action invoked after end of each batch, invoked with {@code index} of the last batch
	 */
	public void forEach(ThrowingBiConsumer<Integer, V, ?> action, ThrowingConsumer<Integer, ?> finalizer) {
		for (int i = 0; i < batches.size(); i++) {
			List<V> batch = batches.get(i);

			for (int j = 0; j < batch.size(); j++) {
				action.accept(j, batch.get(j));
			}
			finalizer.accept(i);
		}
	}
}
