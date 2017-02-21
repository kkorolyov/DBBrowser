package dev.kkorolyov.sqlob.persistence;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import dev.kkorolyov.sqlob.Stub.BasicStub;
import dev.kkorolyov.sqlob.stub.NoOpDataSource;

public class SessionTest {
	private static final Class<?> stubClass = BasicStub.class;
	private static final Object stub = BasicStub.random();
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
		return Arrays.asList(
				dynamicTest("Buffer0", () -> executeNoCommitOnGetId(new Session(ds, 0))),
				dynamicTest("Buffer1", () -> executeNoCommitOnGetId(new Session(ds, 1)))
		);
	}
	private void executeNoCommitOnGetId(Session session) throws SQLException {
		for (int i = 0; i < 10; i++) session.getId(stub);

		verify(conn, never()).commit();
	}

	@TestFactory
	void noCommitOnGet() throws SQLException {
		return Arrays.asList(
				dynamicTest("Buffer0", () -> executeNoCommitOnGetId())
		);
	}
	private void executeNoCommitOnGet(Session session) throws SQLException {
		for (int i = 0; i < 10; i++) session.get(stubClass, UUID.randomUUID());

		verify(conn, never()).commit();
	}

	@TestFactory
	void noCommitOnGetByCondition() throws SQLException {
	}
	private void executeNoCommitOnGetByCondition(Session session) throws SQLException {
		for (int i = 0; i < 10; i++) session.get(stubClass, condition);

		verify(conn, never()).commit();
	}

	private Iterable<DynamicTest> getAutoCommitSessions(Function<Session, Void> action) {
	}

	@Test
	void commitEveryWhenBuffer0() {
		session = new Session(ds, 0);

		session.p
	}
	@Test
	void commitEveryWhenBuffer1() {
		session = new Session(ds, 1);
	}

	@Test
	void commitAfterBufferFills() {
		session = new Session
	}
}
