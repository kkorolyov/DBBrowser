package dev.kkorolyov.sqlob.persistence;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;

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
public class ConnectionTest {
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
		System.out.println(new File(SQLITE_FILE).delete());
	}
	
	public ConnectionTest(DataSource input) {
		this.ds = input;
	}
	
	@Test
	public void testGet() throws SQLException {
		Session conn = new Session(ds);
		conn.get(DumbStub.class, 0);
		conn.get(SmartStub.class, 0);
	}
	
	@Sql("DS")
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
