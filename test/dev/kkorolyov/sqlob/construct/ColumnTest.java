package dev.kkorolyov.sqlob.construct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.sql.Types;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@SuppressWarnings("javadoc")
public class ColumnTest {
	@Parameters
	public static Object[] data() {
		return new Object[]{new SqlobType(Boolean.class, "BOOLEAN", Types.BOOLEAN),
												new SqlobType(Short.class, "SMALLINT", Types.SMALLINT),
												new SqlobType(Integer.class, "INTEGER", Types.INTEGER),
												new SqlobType(Long.class, "BIGINT", Types.BIGINT),
												new SqlobType(Float.class, "REAL", Types.REAL),
												new SqlobType(Double.class, "DOUBLE PRECISION", Types.DOUBLE),
												new SqlobType(Character.class, "CHAR", Types.CHAR),
												new SqlobType(String.class, "VARCHAR", Types.VARCHAR)};
	}
	private final SqlobType type;
	
	public ColumnTest(SqlobType input) {
		type = input;
	}
	
	@Test
	public void testGetSql() {
		// TODO Nothing to assert
	}
	
	@Test
	public void testGetTable() {
		String table = "TABLE";
		assertEquals(table, new Column(table, "", type).getTable());
	}
	
	@Test
	public void testGetName() {
		String name = "NAME";
		assertEquals(name, new Column("", name, type).getName());
	}

	@Test
	public void testGetType() {
		assertEquals(type, new Column("", "", type).getType());
	}

	@Test
	public void testHashCode() {
		String 	table = "TABLE",
						name = "NAME";
		Column 	column1 = new Column(table, name, type),
						column2 = new Column(table, name, type);
		assertEquals(column1.hashCode(), column2.hashCode());
	}
	@Test
	public void testEquals() {
		String 	table = "TABLE",
						name = "NAME";
		Column 	column1 = new Column(table, name, type),
						column2 = new Column(table, name, type),
						columnNot = new Column(null, null, type);
		
		assertEquals(column1, column2);
		assertNotEquals(column1, columnNot);
		assertNotEquals(column2, columnNot);
	}
}
