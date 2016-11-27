package dev.kkorolyov.sqlob.persistence;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
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

import dev.kkorolyov.sqlob.TestAssets;

@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class SessionPerformanceTest {
	private DataSource ds;

	@Parameters(name = "{index}({0})")
	public static Iterable<DataSource> data() {
		return TestAssets.dataSources();
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws FileNotFoundException, IOException {
		TestAssets.cleanUp();
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
	
	public SessionPerformanceTest(DataSource input) {
		this.ds = input;
	}
	
	@Test
	public void testPerformanceRaw() throws SQLException {
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
}
