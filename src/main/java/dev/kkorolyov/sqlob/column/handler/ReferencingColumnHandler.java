package dev.kkorolyov.sqlob.column.handler;

import dev.kkorolyov.simplegraphs.Graph;
import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.column.handler.factory.ColumnHandlerFactory;
import dev.kkorolyov.sqlob.request.CreateRequest;
import dev.kkorolyov.sqlob.request.InsertRequest;
import dev.kkorolyov.sqlob.request.SelectRequest;
import dev.kkorolyov.sqlob.result.Record;
import dev.kkorolyov.sqlob.util.PersistenceHelper;
import dev.kkorolyov.sqlob.util.ReflectionHelper;
import dev.kkorolyov.sqlob.util.Where;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Handles fields mapped to foreign-key columns.
 * Accepts all types not accepted by any other column handler.
 */
public class ReferencingColumnHandler implements ColumnHandler {
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

	@Override
	public Stream<CreateRequest<?>> expandCreates(CreateRequest<?> primaryRequest) {
		Graph<Class<?>> typeDependencies = new Graph<>();
		Map<Class<?>, CreateRequest<?>> requests = new HashMap<>();
		Queue<CreateRequest<?>> requestQueue = new ArrayDeque<>();

		for (CreateRequest<?> request = primaryRequest; request != null; request = requestQueue.poll()) {
			Class<?> requestType = request.getType();
			requests.put(requestType, request);

			request.streamColumns(ReferencingColumn.class)
					.map(ReferencingColumn::getType)
					.peek(referencedType -> typeDependencies.add(referencedType, requestType))
					.filter(referencedType -> !requests.containsKey(referencedType))
					.map(CreateRequest::new)
					.forEach(requestQueue::add);
		}
		return typeDependencies.sortTopological().stream()
				.map(requests::get);
	}

	private class ReferencingColumn extends FieldBackedColumn<Object> {
		private final KeyColumn keyDelegate;

		ReferencingColumn(Field f) {
			this(f, KeyColumn.foreign(PersistenceHelper.getName(f), PersistenceHelper.getName(f.getType())));
		}
		private ReferencingColumn(Field f, KeyColumn keyDelegate) {
			// Relies on delegate
			super(f, null);
			this.keyDelegate = keyDelegate;
		}

		@Override
		public PreparedStatement contribute(PreparedStatement statement, Where where, ExecutionContext context) {
			where.consumeValues(getName(), (index, value) ->
					keyDelegate.getSqlobType().set(
							context.getMetadata(),
							statement,
							index,
							value != null
									? select(value)
									.execute(context)
									.getId().orElseGet(UUID::randomUUID)
									: null));

			return statement;
		}
		@Override
		public <O> PreparedStatement contribute(PreparedStatement statement, Record<UUID, O> record, int index, ExecutionContext context) {
			UUID referencedId = insert(ReflectionHelper.getValue(record.getObject(), getField()))
					.execute(context)
					.getId()
					.orElseThrow(() -> new IllegalStateException("This should never happen"));

			keyDelegate.getSqlobType().set(context.getMetadata(), statement, index, referencedId);

			return statement;
		}

		@Override
		public Object getValue(ResultSet rs, ExecutionContext context) {
			return select(getType(), keyDelegate.getValue(rs, context))
					.execute(context)
					.getObject()
					.orElse(null);
		}

		@Override
		public String getSql(ExecutionContext context) {
			return keyDelegate.getSql(context);
		}
	}
}
