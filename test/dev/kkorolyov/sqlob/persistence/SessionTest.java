package dev.kkorolyov.sqlob.persistence;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import dev.kkorolyov.sqlob.Stub.AnnotatedStub;
import dev.kkorolyov.sqlob.Stub.BasicStub;
import dev.kkorolyov.sqlob.Stub.SmartStub;
import dev.kkorolyov.sqlob.TestAssets;

@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class SessionTest {
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
		String[] tables = new String[]{"SmartStub", "BasicStub", "A"};
		
		try (Connection conn = ds.getConnection()) {
			conn.setAutoCommit(false);
			
			Statement s = conn.createStatement();
			for (String table : tables) {
				s.addBatch("DROP TABLE IF EXISTS " + table);
				s.addBatch("DROP TABLE IF EXISTS " + table.toLowerCase());
			}
			s.executeBatch();
			
			conn.commit();
		}
	}
	
	public SessionTest(DataSource input) {
		this.ds = input;
	}
	
	@Test
	public void testPut() throws SQLException {
		try (Session s = new Session(ds)) {
			s.put(BasicStub.random());
			s.put(AnnotatedStub.random());
			s.put(SmartStub.random());
		}
	}
	@Test
	public void testPutID() throws SQLException {
		UUID id = UUID.randomUUID();
		
		try (Session s = new Session(ds)) {
			assertFalse(s.put(id, BasicStub.random()));
			assertFalse(s.put(id, AnnotatedStub.random()));
			assertFalse(s.put(id, SmartStub.random()));
			
			s.flush();
			
			assertTrue(s.put(id, BasicStub.random()));
			assertTrue(s.put(id, AnnotatedStub.random()));
			assertTrue(s.put(id, SmartStub.random()));
		}
	}
	
	@Test
	public void testGet() throws SQLException {
		BasicStub bs = BasicStub.random();
		AnnotatedStub as = AnnotatedStub.random();
		SmartStub ss = SmartStub.random();
		
		try (Session s = new Session(ds)) {
			UUID 	bsId = s.put(bs),
						asId = s.put(as),
						ssId = s.put(ss);
			
			s.flush();
			
			assertEquals(bs, s.get(bs.getClass(), bsId));
			assertEquals(as, s.get(as.getClass(), asId));
			assertEquals(ss, s.get(ss.getClass(), ssId));
		}
	}
	@Test
	public void testGetCondition() throws SQLException {	// TODO More thorough
		BasicStub bs = BasicStub.random();
		SmartStub ss = new SmartStub(bs);
		Condition cond = new Condition("stub", "=", bs);	// Tests recursion
		
		try (Session s = new Session(ds)) {
			UUID ssId = s.put(ss);
			
			s.flush();
			
			assertEquals(s.get(ss.getClass(), ssId), s.get(ss.getClass(), cond).iterator().next());
		}
	}
	
	@Test
	public void testGetId() throws SQLException {
		BasicStub bs = BasicStub.random();
		AnnotatedStub as = AnnotatedStub.random();
		SmartStub ss = SmartStub.random();
		
		try (Session s = new Session(ds)) {
			UUID 	bsId = s.put(bs),
						asId = s.put(as),
						ssId = s.put(ss);
			
			s.flush();
			
			assertEquals(bsId, s.getId(bs));
			assertEquals(asId, s.getId(as));
			assertEquals(ssId, s.getId(ss));
		}
	}
	
	@Test
	public void testDrop() throws SQLException {
		BasicStub bs = BasicStub.random();
		AnnotatedStub as = AnnotatedStub.random();
		SmartStub ss = SmartStub.random();
		
		try (Session s = new Session(ds)) {
			UUID 	bsId = s.put(bs),
						asId = s.put(as),
						ssId = s.put(ss);
			
			s.flush();
			
			assertEquals(bs, s.get(BasicStub.class, bsId));
			assertEquals(as, s.get(AnnotatedStub.class, asId));
			assertEquals(ss, s.get(SmartStub.class, ssId));
			
			assertTrue(s.drop(bs.getClass(), bsId));
			assertTrue(s.drop(as.getClass(), asId));
			assertTrue(s.drop(ss.getClass(), ssId));
			
			s.flush();
			
			assertFalse(s.drop(bs.getClass(), bsId));	// Assert dropped
			assertFalse(s.drop(as.getClass(), asId));
			assertFalse(s.drop(ss.getClass(), ssId));
			
			assertNull(s.get(bs.getClass(), bsId));
			assertNull(s.get(as.getClass(), asId));
			assertNull(s.get(as.getClass(), ssId));
		}
	}
	@Test
	public void testDropCondition() throws SQLException {
		// TODO
	}
	
	@Test
	public void testExceptions() throws SQLException {
		// TODO
	}
}
