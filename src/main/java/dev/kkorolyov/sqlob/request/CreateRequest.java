package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.simplefuncs.stream.Predicates;
import dev.kkorolyov.sqlob.column.ReferencingColumn;
import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Result;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.kkorolyov.simplefuncs.stream.Collectors.joiningDefaultEmpty;
import static dev.kkorolyov.sqlob.column.Column.ID_COLUMN;

/**
 * Request to create a table for a specific class.
 */
public class CreateRequest<T> extends Request<T> {
	private final Set<Class<?>> knownTypes;

	/**
	 * Constructs a new create request.
	 * @see Request#Request(Class)
	 */
	public CreateRequest(Class<T> type) {
		this(type, null);
	}
	// Used only to create prerequisites
	private CreateRequest(Class<T> type, CreateRequest parent) {
		super(type);

		// Ensure only distinct prerequisites created across entire hierarchy
		knownTypes = parent != null
				? parent.knownTypes
				: new HashSet<>();
		knownTypes.add(type);
	}

	@Override
	Result<T> executeInContext(ExecutionContext context) throws SQLException {
		Collection<CreateRequest<?>> allRequests = getAllRequests()
				.collect(Collectors.toSet());

		Collection<String> sql = Stream.of(
				getCreateStatements(allRequests),
				getConstraintStatements(allRequests)
		).flatMap(s -> s)
				.collect(Collectors.toList());

		logStatements(sql);

		Statement statement = context.getStatement();
		for (String s : sql) statement.addBatch(s);
		statement.executeBatch();

		return new ConfigurableResult<>();
	}

	private Stream<CreateRequest<?>> getAllRequests() {
		return Stream.concat(
				Stream.of(this),
				getColumns(ReferencingColumn.class).stream()
						.map(ReferencingColumn::getReferencedType)
						.filter(type -> !knownTypes.contains(type))
						.map(type -> new CreateRequest<>(type, this))
						.flatMap(CreateRequest::getAllRequests)
		);
	}

	private static Stream<String> getCreateStatements(Collection<CreateRequest<?>> requests) {
		return requests.stream()
				.map(request -> request.getColumns().stream()
						.map(column -> column.getName() + " " + column.getSqlType())
						.collect(Collectors.joining(", ",
								"CREATE TABLE IF NOT EXISTS " + request.getName()
										+ " (" + ID_COLUMN.getName() + " " + ID_COLUMN.getSqlType() + " PRIMARY KEY, ",
								")")));
	}
	private static Stream<String> getConstraintStatements(Collection<CreateRequest<?>> requests) {
		return requests.stream()
				.map(request -> request.getColumns(ReferencingColumn.class).stream()
						.map(column -> "CONSTRAINT IF NOT EXISTS FK_" + column.getName() + "_" + column.getReferencedName()
								+ " FOREIGN KEY (" + column.getName() + ")"
								+ " REFERENCES " + column.getReferencedName() + " (" + ID_COLUMN.getName() + ")"
								+ " ON DELETE SET NULL")
						.collect(joiningDefaultEmpty(", ",
								"ALTER TABLE " + request.getName() + " ADD ",
								"")))
				.filter(Predicates::nonEmpty);
	}
}
