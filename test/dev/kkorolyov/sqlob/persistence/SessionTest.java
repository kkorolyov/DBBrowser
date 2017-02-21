package dev.kkorolyov.sqlob.persistence;

import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import dev.kkorolyov.sqlob.Stub.BasicStub;
import dev.kkorolyov.sqlob.stub.NoOpDataSource;

public class SessionTest {
	private static final int NUM_STUBS = 10;
	private static final Class<?> stubClass = BasicStub.class;
	private static final Iterable<Object> stubs = Stream.generate(BasicStub::random)
																											.limit(NUM_STUBS)
																											.collect(Collectors.toList());
	private static final Condition condition = new Condition("short", "=", 4);

	private Connection conn;
	private DataSource ds;
	private Session session;

	@BeforeEach
	void beforeEach() {
		conn = mock(Connection.class);
		ds = new NoOpDataSource() {
			@Override
			public Connection getConnection() throws SQLException {
				return conn;
			}
		};
		session = new Session(ds);
	}

	@TestFactory
	Iterable<DynamicTest> noCommitOnGetId() throws SQLException {
		return generateTests(session -> {
			for (Object stub : stubs) session.getId(stub);
			verify(conn, never()).commit();
		}, new Session(ds, 0), new Session(ds, 1));
	}
	@TestFactory
	Iterable<DynamicTest> noCommitOnGet() throws SQLException {
		return generateTests(session -> {
			for (int i = 0; i < NUM_STUBS; i++) session.get(stubClass, UUID.randomUUID());
			verify(conn, never()).commit();
		}, new Session(ds, 0), new Session(ds, 1));
	}
	@TestFactory
	Iterable<DynamicTest> noCommitOnGetByCondition() throws SQLException {
		return generateTests(session -> {
			for (int i = 0; i < NUM_STUBS; i++) session.get(stubClass, condition);
			verify(conn, never()).commit();
		}, new Session(ds, 0), new Session(ds, 1));
	}

	@Test
	void commitEveryWhenBuffer0() throws SQLException {
		session = new Session(ds, 0);

		for (Object stub : stubs) session.put(stub);
		verify(conn, never()).commit();
	}
	@Test
	void commitEveryWhenBuffer1() {
		session = new Session(ds, 1);
	}

	@Test
	void commitAfterBufferFills() {

	}

	private static Iterable<DynamicTest> generateTests(SessionConsumer test, Session... sessions) {
		List<DynamicTest> tests = new ArrayList<>();

		for (Session session : sessions) {
			tests.add(DynamicTest.dynamicTest(session.toString(), () -> test.accept(session)));
		}
		return tests;
	}

	private static interface SessionConsumer {
		void accept(Session session) throws SQLException;
	}
}
