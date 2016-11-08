package dev.kkorolyov.sqlob.persistence;

import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sqlite.SQLiteDataSource;

import dev.kkorolyov.sqlob.annotation.Reference;
import dev.kkorolyov.sqlob.annotation.Sql;

@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class ConnectionTest {
	private static final String SQLITE_FILE = "test/sqlite.db";
	
	private DataSource ds;
	
	@Parameters(name = "{index}({0})")
	public static Iterable<DataSource> data() {
		SQLiteDataSource sqliteDS = new SQLiteDataSource();
		sqliteDS.setUrl("jdbc:sqlite:" + SQLITE_FILE);
		
		return Arrays.asList(sqliteDS);
	}
	
	public ConnectionTest(DataSource input) {
		this.ds = input;
	}
	
	@Test
	public void testGet() throws SQLException {
		Connection conn = new Connection(ds);
		conn.get(DumbStub.class, 0);
		conn.get(SmartStub.class, 0);
	}
	
	class DumbStub {
		@Sql("INT")
		int num;
	}
	@Sql("SS")
	class SmartStub {
		@Reference
		DumbStub stub;
	}
}
