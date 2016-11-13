package dev.kkorolyov.sqlob.persistence;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.postgresql.ds.PGSimpleDataSource;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import dev.kkorolyov.sqlob.TestAssets;
import dev.kkorolyov.sqlob.annotation.Reference;
import dev.kkorolyov.sqlob.annotation.Sql;

@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class SessionTest {
	private static final String SQLITE_FILE = "test/sqlite.db";
	
	private DataSource ds;
	
	@Parameters(name = "{index}({0})")
	public static Iterable<DataSource> data() {
		SQLiteConfig config = new SQLiteConfig();
		config.enforceForeignKeys(true);
		SQLiteDataSource sqliteDS = new SQLiteDataSource(config);
		sqliteDS.setUrl("jdbc:sqlite:" + SQLITE_FILE);
		
		MysqlDataSource mysqlDS = new MysqlDataSource();
		mysqlDS.setServerName(TestAssets.host());
		mysqlDS.setDatabaseName(TestAssets.database());
		mysqlDS.setUser(TestAssets.user());
		mysqlDS.setPassword(TestAssets.password());
		
		PGSimpleDataSource pgDS = new PGSimpleDataSource();
		pgDS.setServerName(TestAssets.host());
		pgDS.setDatabaseName(TestAssets.database());
		pgDS.setUser(TestAssets.user());
		pgDS.setPassword(TestAssets.password());
		
		return Arrays.asList(sqliteDS, mysqlDS, pgDS);
	}
	
	@AfterClass
	public static void tearDownAfterClass() {
		System.out.println((new File(SQLITE_FILE).delete() ? "Deleted " : "Failed to delete ") + "test SQLite file: " + SQLITE_FILE);
	}
	@After
	public void tearDown() throws SQLException {
		try (Connection conn = ds.getConnection()) {
			conn.setAutoCommit(false);
			
			Statement s = conn.createStatement();
			s.addBatch("DROP TABLE IF EXISTS SS");
			s.addBatch("DROP TABLE IF EXISTS ss");

			s.addBatch("DROP TABLE IF EXISTS DumbStub");
			s.addBatch("DROP TABLE IF EXISTS dumbstub");
			
			s.addBatch("DROP TABLE IF EXISTS Test");
			s.addBatch("DROP TABLE IF EXISTS test");

			s.executeBatch();
			
			conn.commit();
		}
	}
	
	public SessionTest(DataSource input) {
		this.ds = input;
	}
	
	@Test
	public void testPerformance() throws SQLException {
		int tests = 100;
		
		long start = System.nanoTime();
		try (Connection conn = ds.getConnection()) {
			conn.setAutoCommit(false);
			
			Statement s = conn.createStatement();
			s.executeUpdate("DROP TABLE IF EXISTS Test");
			s.executeUpdate("CREATE TABLE IF NOT EXISTS Test (id CHAR(3) PRIMARY KEY, num INT)");
			conn.commit();
			
			PreparedStatement ps = conn.prepareStatement("INSERT INTO Test (id,num) VALUES (?,?)");
			for (int i = 0; i < tests; i++) {
				ps.setString(1, String.valueOf(i));
				ps.setInt(2, i);
				ps.executeUpdate();
			}
			conn.commit();
		}
		long ms = (System.nanoTime() - start) / 1000000;

		System.out.println(ms + "ms to PUT " + tests + " things using " + ds);
	}
	
	@Test
	public void testPerformanceDumbStub() throws SQLException {
		int tests = 100;
		
		List<UUID> uuids = new LinkedList<>();
		
		long start = System.nanoTime();
		try (Session session = new Session(ds)) {
			for (int i = 0; i < tests; i++)
				uuids.add(session.put(new DumbStub(i)));
		}
		long ms = (System.nanoTime() - start) / 1000000;
		
		System.out.println(ms + "ms to PUT " + tests + " DumbStubs using " + ds);
		
		start = System.nanoTime();
		try (Session session = new Session(ds)) {
			for (UUID uuid : uuids)
				session.get(DumbStub.class, uuid);
		}
		ms = (System.nanoTime() - start) / 1000000;
		
		System.out.println(ms + " ms to GET " + tests + " DumbStubs using " + ds);
	}
	@Test
	public void testPerformanceSmartStub() throws SQLException {
		int tests = 100;
		
		List<UUID> uuids = new LinkedList<>();
		
		long start = System.nanoTime();
		try (Session session = new Session(ds)) {
			for (int i = 0; i < tests; i++)
				uuids.add(session.put(new SmartStub(new DumbStub(i))));
		}
		long ms = (System.nanoTime() - start) / 1000000;
		
		System.out.println(ms + "ms to PUT " + tests + " SmartStubs using " + ds);
		
		start = System.nanoTime();
		try (Session session = new Session(ds)) {
			for (UUID uuid : uuids)
				session.get(SmartStub.class, uuid);
		}
		ms = (System.nanoTime() - start) / 1000000;
		
		System.out.println(ms + " ms to GET " + tests + " SmartStubs using " + ds);
	}
	
	@Test
	public void testGetId() throws SQLException {
		try (Session session = new Session(ds)) {
			DumbStub ds = new DumbStub(125135);
			SmartStub ss = new SmartStub(ds);
			
			UUID 	dsID = session.put(ds),
						ssID = session.put(ss);
			
			assertEquals(ds, session.get(ds.getClass(), dsID));
			assertEquals(ss, session.get(ss.getClass(), ssID));
		}
	}
	@Test
	public void testGetCondition() throws SQLException {
		try (Session session = new Session(ds)) {
			int num = 17;
			DumbStub ds = new DumbStub(17);
			Condition cond = new Condition("num", "=", num);
			
			session.put(ds);
			
			assertEquals(ds, session.get(ds.getClass(), cond).iterator().next());
		}
	}
	
	static class DumbStub {
		@Sql("INTEGER")
		int num;
		
		DumbStub() {}
		DumbStub(int num) {
			this.num = num;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;

			result = prime * result + num;
			
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			
			if (obj == null)
				return false;
			if (!(obj instanceof DumbStub))
				return false;
			
			DumbStub o = (DumbStub) obj;
			
			if (num != o.num)
				return false;
			
			return true;
		}
		
		@Override
		public String toString() {
			return getClass().getName() + "(" + num + ")";
		}
	}
	@Sql("SS")
	static class SmartStub {
		@Reference
		DumbStub stub;
		
		SmartStub(){}
		SmartStub(DumbStub stub) {
			this.stub = stub;
		}
		
		@Override
		public String toString() {
			return getClass().getName() + "(" + stub + ")";
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			
			result = prime * result + ((stub == null) ? 0 : stub.hashCode());
			
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			
			if (obj == null)
				return false;
			if (!(obj instanceof SmartStub))
				return false;
			
			SmartStub o = (SmartStub) obj;
			
			if (stub == null) {
				if (o.stub != null)
					return false;
			} else if (!stub.equals(o.stub))
				return false;
			
			return true;
		}
	}
}
