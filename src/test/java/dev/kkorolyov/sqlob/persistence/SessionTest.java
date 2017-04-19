package dev.kkorolyov.sqlob.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import dev.kkorolyov.sqlob.JDBCMocks;
import dev.kkorolyov.sqlob.Session;
import dev.kkorolyov.sqlob.Stub.BasicStub;
import dev.kkorolyov.sqlob.utility.Condition;

class SessionTest {
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

	private JDBCMocks mocks = new JDBCMocks();

	private Session session = new Session(mocks.ds);

	@TestFactory
	Iterable<DynamicTest> negativeBufferSizeDefaultsToZero() {
		return generateTests(
				() -> assertEquals(0, readBufferSize()),
				IntStream.range(-100, 0).mapToObj(i -> new Session(mocks.ds, i))::iterator
		);
	}
	@Test
	void oneBufferSizeDefaultsToZero() throws NoSuchFieldException, IllegalAccessException {
		session = new Session(mocks.ds, 1);

		assertEquals(0, readBufferSize());
	}
	@Test
	void greaterThanOneBufferSizeStaysAsIs() throws NoSuchFieldException, IllegalAccessException {
		int bufferSize = 2;
		session = new Session(mocks.ds, bufferSize);

		assertEquals(bufferSize, readBufferSize());
	}
	private int readBufferSize() throws NoSuchFieldException, IllegalAccessException {
		Field bufferSizeField = session.getClass().getDeclaredField("bufferSize");
		bufferSizeField.setAccessible(true);

		return (int) bufferSizeField.get(session);
	}

	@TestFactory
	Iterable<DynamicTest> noCommitOnGetId() throws SQLException {
		return generateTests(
				() -> {
					for (Object stub : stubs) session.getId(stub);
					verify(mocks.conn, never()).commit();
				},
				IntStream.range(0, NUM_PARAMETERIZED_TESTS).mapToObj(i -> new Session(mocks.ds, i))::iterator
		);
	}
	@TestFactory
	Iterable<DynamicTest> noCommitOnGet() throws SQLException {
		return generateTests(
				() -> {
					for (UUID uuid : uuids) session.get(stubClass, uuid);
					verify(mocks.conn, never()).commit();
				},
				IntStream.range(0, NUM_PARAMETERIZED_TESTS).mapToObj(i -> new Session(mocks.ds, i))::iterator
		);
	}
	@TestFactory
	Iterable<DynamicTest> noCommitOnGetByCondition() throws SQLException {
		return generateTests(
				() -> {
					for (Condition condition : conditions) session.get(stubClass, condition);
					verify(mocks.conn, never()).commit();
				},
				IntStream.range(0, NUM_PARAMETERIZED_TESTS).mapToObj(i -> new Session(mocks.ds, i))::iterator
		);
	}

	@Test
	void commitEveryPutWhenNoBuffer() throws SQLException {
		session = new Session(mocks.ds, 0);

		for (Object stub : stubs) session.put(stub);
		verify(mocks.conn, times(NUM_PARAMETERIZED_TESTS)).commit();
	}
	@Test
	void commitEveryDropWhenNoBuffer() throws SQLException {
		session = new Session(mocks.ds, 0);

		for (UUID uuid : uuids) session.drop(stubClass, uuid);
		verify(mocks.conn, times(NUM_PARAMETERIZED_TESTS)).commit();
	}
	@Test
	void commitEveryDropByConditionWhenNoBuffer() throws SQLException {
		session = new Session(mocks.ds, 0);

		for (Condition condition : conditions) session.drop(stubClass, condition);
		verify(mocks.conn, times(NUM_PARAMETERIZED_TESTS)).commit();
	}

	@Test
	void commitUponBufferFill() throws SQLException {
		session = new Session(mocks.ds, NUM_PARAMETERIZED_TESTS);

		Iterator<Object> it = stubs.iterator();
		while (it.hasNext()) {
			session.put(it.next());

			if (it.hasNext()) verify(mocks.conn, never()).commit();
			else verify(mocks.conn, times(1)).commit();
		}
	}

	@Test
	void flushForcesCommit() throws SQLException, NoSuchFieldException, IllegalAccessException {
		injectConn();

		session.flush();

		verify(mocks.conn, times(1)).commit();
	}
	@Test
	void closeForcesCommit() throws SQLException, NoSuchFieldException, IllegalAccessException {
		injectConn();

		session.close();

		verify(mocks.conn, times(1)).commit();
	}
	private void injectConn() throws NoSuchFieldException, IllegalAccessException {
		Field connField = session.getClass().getDeclaredField("conn");
		connField.setAccessible(true);
		connField.set(session, mocks.conn);
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

	private interface ExceptionRunnable {
		void run() throws Exception;
	}
}
