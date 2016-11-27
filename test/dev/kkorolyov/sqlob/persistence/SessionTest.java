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
	public void testGet() throws SQLException {
		try (Session session = new Session(ds)) {
			BasicStub bs = BasicStub.random();
			AnnotatedStub as = AnnotatedStub.random();
			SmartStub ss = SmartStub.random();
			
			UUID 	bsId = session.put(bs),
						asId = session.put(as),
						ssId = session.put(ss);
			
			assertEquals(bs, session.get(bs.getClass(), bsId));
			assertEquals(as, session.get(as.getClass(), asId));
			assertEquals(ss, session.get(ss.getClass(), ssId));
		}
	}
	@Test
	public void testGetCondition() throws SQLException {
		fail("TODO");
	}
}
