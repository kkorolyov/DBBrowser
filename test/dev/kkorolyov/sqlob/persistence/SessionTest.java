package dev.kkorolyov.sqlob.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import dev.kkorolyov.sqlob.Stub.BasicStub;

public class SessionTest {
	private static final int NUM_PARAMETERIZED_TESTS = 10;
	private static final Class<?> stubClass = BasicStub.class;
	private static final Iterable<Object> stubs = Stream.generate(BasicStub::random)
																											.limit(NUM_PARAMETERIZED_TESTS)
																											.collect(Collectors.toList());
	private static final Iterable<UUID> uuids = Stream.generate(UUID::randomUUID)
																										.limit(NUM_PARAMETERIZED_TESTS)
																										.collect(Collectors.toList());
	private static final Iterable<Condition> conditions = IntStream.range(0, NUM_PARAMETERIZED_TESTS)
																																 .mapToObj(i -> new Condition("Field" + i, "=", i))
																																 .collect(Collectors.toList());

	@Mock
	private DataSource ds;
	@Mock
	private Connection conn;
	@Mock
	private Statement statement;
	@Mock
	private PreparedStatement preparedStatement;
	@Mock
	private ResultSet resultSet;

	private Session session;

	@BeforeEach
	void beforeEach() throws SQLException {
		MockitoAnnotations.initMocks(this);

		when(statement.executeQuery(any(String.class))).thenReturn(resultSet);

		when(preparedStatement.executeQuery()).thenReturn(resultSet);

		when(conn.createStatement()).thenReturn(statement);
		when(conn.prepareStatement(any(String.class))).thenReturn(preparedStatement);

		when(ds.getConnection()).thenReturn(conn);

		session = new Session(ds);
	}

	@TestFactory
	Iterable<DynamicTest> negativeBufferSizeDefaultsToZero() {
		return generateTests(
				() -> {
					Field bufferSizeField = Session.class.getDeclaredField("bufferSize");
					bufferSizeField.setAccessible(true);

					assertEquals(0, bufferSizeField.get(session));
				},
				IntStream.range(-100, 0).mapToObj(i -> new Session(ds, i))::iterator
		);
	}
	@Test
	void oneBufferSizeDefaultsToZero() throws NoSuchFieldException, IllegalAccessException {
		session = new Session(ds, 1);

		Field bufferSizeField = Session.class.getDeclaredField("bufferSize");
		bufferSizeField.setAccessible(true);

		assertEquals(0, bufferSizeField.get(session));
	}
	@Test
	void greaterThanOneBufferSizeStaysAsIs() throws NoSuchFieldException, IllegalAccessException {
		int bufferSize = 2;
		session = new Session(ds, bufferSize);

		Field bufferSizeField = Session.class.getDeclaredField("bufferSize");
		bufferSizeField.setAccessible(true);

		assertEquals(bufferSize, bufferSizeField.get(session));
	}

	@TestFactory
	Iterable<DynamicTest> noCommitOnGetId() throws SQLException {
		return generateTests(
				() -> {
					for (Object stub : stubs) session.getId(stub);
					verify(conn, never()).commit();
				},
				IntStream.range(0, NUM_PARAMETERIZED_TESTS).mapToObj(i -> new Session(ds, i))::iterator
		);
	}
	@TestFactory
	Iterable<DynamicTest> noCommitOnGet() throws SQLException {
		return generateTests(
				() -> {
					for (UUID uuid : uuids) session.get(stubClass, uuid);
					verify(conn, never()).commit();
				},
				IntStream.range(0, NUM_PARAMETERIZED_TESTS).mapToObj(i -> new Session(ds, i))::iterator
		);
	}
	@TestFactory
	Iterable<DynamicTest> noCommitOnGetByCondition() throws SQLException {
		return generateTests(
				() -> {
					for (Condition condition : conditions) session.get(stubClass, condition);
					verify(conn, never()).commit();
				},
				IntStream.range(0, NUM_PARAMETERIZED_TESTS).mapToObj(i -> new Session(ds, i))::iterator
		);
	}

	@Test
	void commitEveryPutWhenNoBuffer() throws SQLException {
		session = new Session(ds, 0);

		for (Object stub : stubs) session.put(stub);
		verify(conn, times(NUM_PARAMETERIZED_TESTS)).commit();
	}
	@Test
	void commitEveryDropWhenNoBuffer() throws SQLException {
		session = new Session(ds, 0);

		for (UUID uuid : uuids) session.drop(stubClass, uuid);
		verify(conn, times(NUM_PARAMETERIZED_TESTS)).commit();
	}
	@Test
	void commitEveryDropByConditionWhenNoBuffer() throws SQLException {
		session = new Session(ds, 0);

		for (Condition condition : conditions) session.drop(stubClass, condition);
		verify(conn, times(NUM_PARAMETERIZED_TESTS)).commit();
	}

	@Test
	void commitUponBufferFill() throws SQLException {
		session = new Session(ds, NUM_PARAMETERIZED_TESTS);

		Iterator<Object> it = stubs.iterator();
		while (it.hasNext()) {
			session.put(it.next());

			if (it.hasNext()) verify(conn, never()).commit();
			else verify(conn, times(1)).commit();
		}
	}

	private Iterable<DynamicTest> generateTests(ExceptionRunnable test, Iterable<Session> sessions) {
		List<DynamicTest> tests = new ArrayList<>();

		for (Session session : sessions) {
			tests.add(dynamicTest(session.toString(), () -> {
				this.session = session;
				test.run();
			}));
		}
		return tests;
	}

	private static interface ExceptionRunnable {
		void run() throws Exception;
	}
}
