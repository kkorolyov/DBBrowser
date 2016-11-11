package dev.kkorolyov.sqlob.persistence;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

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
		
		return Arrays.asList(sqliteDS);
	}
	
	@AfterClass
	public static void tearDownAfterClass() {
		System.out.println((new File(SQLITE_FILE).delete() ? "Deleted " : "Failed to delete ") + "test SQLite file: " + SQLITE_FILE);
	}
	
	public SessionTest(DataSource input) {
		this.ds = input;
	}
	
	@Test
	public void testGetId() throws SQLException {
		Session session = new Session(ds);
		
		DumbStub ds = new DumbStub(125135);
		SmartStub ss = new SmartStub(ds);
		
		UUID 	dsID = session.put(ds),
					ssID = session.put(ss);
		
		assertEquals(ds, session.get(ds.getClass(), dsID));
		assertEquals(ss, session.get(ss.getClass(), ssID));
	}
	@Test
	public void testGetCondition() throws SQLException {
		Session session = new Session(ds);
		int num = 17;
		DumbStub ds = new DumbStub(17);
		Condition cond = new Condition("num", "=", num);
		
		session.put(ds);
		
		assertEquals(ds, session.get(ds.getClass(), cond).iterator().next());
	}
	
	static class DumbStub {
		@Sql("INT")
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
