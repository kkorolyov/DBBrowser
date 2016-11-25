package dev.kkorolyov.sqlob.persistence;

import static org.junit.Assert.assertEquals;

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

import dev.kkorolyov.sqlob.TestAssets;
import dev.kkorolyov.sqlob.annotation.Column;
import dev.kkorolyov.sqlob.annotation.Table;
import dev.kkorolyov.sqlob.sql.Condition;

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
	public void putDumb() throws SQLException {
		try (Session session = new Session(ds)) {
			session.put(new DumbStub(0));
		}
	}
	@Test
	public void putSmart() throws SQLException {
		try (Session session = new Session(ds)) {
			session.put(new SmartStub(new DumbStub(0)));
		}
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
			Condition cond = new Condition("n", "=", num);
			
			session.put(ds);
			
			assertEquals(ds, session.get(ds.getClass(), cond).iterator().next());
		}
	}
	
	static class DumbStub {
		private int num;
		
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
	@Table("SS")
	static class SmartStub {
		@Column("s")
		private DumbStub stub;
		
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
