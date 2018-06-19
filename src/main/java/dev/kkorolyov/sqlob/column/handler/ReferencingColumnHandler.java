package dev.kkorolyov.sqlob.column.handler;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.column.handler.factory.ColumnHandlerFactory;
import dev.kkorolyov.sqlob.request.CreateRequest;
import dev.kkorolyov.sqlob.request.InsertRequest;
import dev.kkorolyov.sqlob.request.SelectRequest;
import dev.kkorolyov.sqlob.result.Record;
import dev.kkorolyov.sqlob.struct.Table;
import dev.kkorolyov.sqlob.util.PersistenceHelper;
import dev.kkorolyov.sqlob.util.ReflectionHelper;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles fields mapped to foreign-key columns.
 * Accepts all types not accepted by any other column handler.
 */
public class ReferencingColumnHandler implements ColumnHandler {
	private final Map<String, Collection<CreateRequest<?>>> prerequisites = new HashMap<>();

	SelectRequest<?> select(Object value) {
		return new SelectRequest<>(value);
	}
	<T> SelectRequest<T> select(Class<T> c, UUID id) {
		return new SelectRequest<>(c, id);
	}

	InsertRequest<?> insert(Object instance) {
		return new InsertRequest<>(instance);
	}

	@Override
	public FieldBackedColumn<?> get(Field f) {
		return new ReferencingColumn(f);
	}
	@Override
	public boolean accepts(Field f) {
		return ColumnHandlerFactory.stream()
				.filter(columnHandler -> !columnHandler.equals(this))
				.noneMatch(columnHandler -> columnHandler.accepts(f));
	}

	private Collection<CreateRequest<?>> buildPrerequisites(Class<?> c) {
		Map<Class<?>, CreateRequest<?>> requests = new HashMap<>();
		Queue<CreateRequest<?>> requestQueue = new ArrayDeque<>();

		for (CreateRequest<?> request = new CreateRequest<>(c); request != null; request = requestQueue.poll()) {
			Class<?> requestType = request.getType();
			requests.put(requestType, request);

			request.streamColumns(ReferencingColumn.class)
					.map(ReferencingColumn::getType)
					.filter(referencedType -> !requests.containsKey(referencedType))
					.map(CreateRequest::new)
					.forEach(requestQueue::add);
		}
		return requests.values();
	}

	private class ReferencingColumn extends FieldBackedColumn<Object> {
		private final KeyColumn keyDelegate;

		ReferencingColumn(Field f) {
			// Relies on delegate
			super(f, null);
			this.keyDelegate = KeyColumn.foreign(PersistenceHelper.getName(f), PersistenceHelper.getName(f.getType()));
		}

		@Override
		public Object resolve(Object value, ExecutionContext context) {
			return value != null
					? select(value)
					.execute(context)
					.getKey().orElseGet(UUID::randomUUID)
					: null;
		}

		@Override
		public Object get(Record<UUID, ?> record, ExecutionContext context) {
			UUID referencedId = insert(ReflectionHelper.getValue(record.getObject(), getField()))
					.execute(context)
					.getKey()
					.orElseThrow(() -> new IllegalStateException("This should never happen"));

			return keyDelegate.getSqlobType().get(context.getMetadata(), referencedId);
		}

		@Override
		public Object get(ResultSet rs, ExecutionContext context) {
			return select(getType(), keyDelegate.get(rs, context))
					.execute(context)
					.getObject()
					.orElse(null);
		}

		@Override
		public String getSql(ExecutionContext context) {
			return keyDelegate.getSql(context);
		}

		@Override
		public Collection<Table> getPrerequisites(ExecutionContext context) {
			return prerequisites.computeIfAbsent(getName(), k -> buildPrerequisites(getType()))
					.stream()
					.map(request -> request.toTable(context))
					.collect(Collectors.toSet());
		}
	}
}
