package dev.kkorolyov.sqlob.request.collection;

import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.request.Request;
import dev.kkorolyov.sqlob.util.PersistenceHelper;
import dev.kkorolyov.sqlob.util.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * A single transaction of a {@link Collection}.
 */
public abstract class CollectionRequest extends Request<Collection<?>> {
	private final Class<?> parameterType;

	/**
	 * Constructs a new collection request.
	 * @param f associated collection field
	 */
	protected CollectionRequest(Field f) {
		super((Class<Collection<?>>) (Class<?>) Collection.class, PersistenceHelper.getFieldTableName(f),
				KeyColumn.parent(PersistenceHelper.getName(f.getDeclaringClass())),
				KeyColumn.child(PersistenceHelper.getName(ReflectionHelper.getGenericParameters(f)[0])));

		parameterType = ReflectionHelper.getGenericParameters(f)[0];
	}

	/** @return parameter type of collection handled by request */
	public final Class<?> getParameterType() {
		return parameterType;
	}
}
