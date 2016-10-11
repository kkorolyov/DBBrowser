package dev.kkorolyov.sqlob.construct;

import static org.junit.Assert.*;

import java.sql.Types;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@SuppressWarnings("javadoc")
public class EntryTest {
	@Parameters(name = "SqlobType({0})")
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
	
	public EntryTest(SqlobType input) {
		type = input;
	}
	
	@Test
	public void testHasParameter() {
		assertFalse(new Entry(getStubColumn(), null).hasParameter());	// =NULL
		assertTrue(new Entry(getStubColumn(), getStubValue()).hasParameter());	// =VALUE
		assertFalse(new Entry(getStubColumn(), getStubColumn()).hasParameter());	// =COLUMN
		
		for (Entry.Operator operator : Entry.Operator.values()) {
			assertFalse(new Entry(getStubColumn(), operator, null).hasParameter());	// [OP]NULL
			assertTrue(new Entry(getStubColumn(), operator, getStubValue()).hasParameter());	// [OP]VALUE
			assertFalse(new Entry(getStubColumn(), operator, getStubColumn()).hasParameter());	// [OP]COLUMN
		}
	}
	
	@Test
	public void testGetSql() {
		assertEquals(getStubColumn().getSql() + " IS NULL", new Entry(getStubColumn(), null).getSql());	// =NULL
		assertEquals(getStubColumn().getSql() + "=?", new Entry(getStubColumn(), getStubValue()).getSql());	// =VALUE
		assertEquals(getStubColumn().getSql() + "=" + getStubColumn().getSql(), new Entry(getStubColumn(), getStubColumn()).getSql());	// =COLUMN
		
		for (Entry.Operator operator : Entry.Operator.values()) {
			assertEquals(getStubColumn().getSql() + " IS NULL", new Entry(getStubColumn(), operator, null).getSql());	// [OP]NULL
			assertEquals(getStubColumn().getSql() + operator.getSql(), new Entry(getStubColumn(), operator, getStubValue()).getSql());	// [OP]VALUE
			assertEquals(getStubColumn().getSql() + operator.getSql().replaceFirst(Pattern.quote("?"), getStubColumn().getSql()), new Entry(getStubColumn(), operator, getStubColumn()).getSql());	// [OP]COLUMN
		}
	}
	
	@Test
	public void testGetColumn() {
		assertEquals(getStubColumn(), new Entry(getStubColumn(), null).getColumn());	// =NULL
		assertEquals(getStubColumn(), new Entry(getStubColumn(), getStubValue()).getColumn());	// =VALUE
		assertEquals(getStubColumn(), new Entry(getStubColumn(), getStubColumn()).getColumn());	// =COLUMN
		
		for (Entry.Operator operator : Entry.Operator.values()) {
			assertEquals(getStubColumn(), new Entry(getStubColumn(), operator, null).getColumn());	// [OP]NULL
			assertEquals(getStubColumn(), new Entry(getStubColumn(), operator, getStubValue()).getColumn());	// [OP]VALUE
			assertEquals(getStubColumn(), new Entry(getStubColumn(), operator, getStubColumn()).getColumn());	// [OP]COLUMN
		}
	}
	@Test
	public void testGetOperator() {
		assertEquals(Entry.Operator.EQUALS, new Entry(getStubColumn(), null).getOperator());	// =NULL
		assertEquals(Entry.Operator.EQUALS, new Entry(getStubColumn(), getStubValue()).getOperator());	// =VALUE
		assertEquals(Entry.Operator.EQUALS, new Entry(getStubColumn(), getStubColumn()).getOperator());	// =COLUMN
		
		for (Entry.Operator operator : Entry.Operator.values()) {
			assertEquals(operator, new Entry(getStubColumn(), operator, null).getOperator());	// [OP]NULL
			assertEquals(operator, new Entry(getStubColumn(), operator, getStubValue()).getOperator());	// [OP]VALUE
			assertEquals(operator, new Entry(getStubColumn(), operator, getStubColumn()).getOperator());	// [OP]COLUMN
		}
	}
	@Test
	public void testGetValue() {
		assertEquals(null, new Entry(getStubColumn(), null).getValue());	// =NULL
		assertEquals(getStubValue(), new Entry(getStubColumn(), getStubValue()).getValue());	// =VALUE
		assertEquals(getStubColumn(), new Entry(getStubColumn(), getStubColumn()).getValue());	// =COLUMN
		
		for (Entry.Operator operator : Entry.Operator.values()) {
			assertEquals(null, new Entry(getStubColumn(), operator, null).getValue());	// [OP]NULL
			assertEquals(getStubValue(), new Entry(getStubColumn(), operator, getStubValue()).getValue());	// [OP]VALUE
			assertEquals(getStubColumn(), new Entry(getStubColumn(), operator, getStubColumn()).getValue());	// [OP]COLUMN
		}
	}
	
	@Test
	public void testHashCode() {
		Entry eNull = new Entry(getStubColumn(), null),
					eNull2 = new Entry(getStubColumn(), null),
					eVal = new Entry(getStubColumn(), getStubValue()),
					eVal2 = new Entry(getStubColumn(), getStubValue()),
					eCol = new Entry(getStubColumn(), getStubColumn()),
					eCol2 = new Entry(getStubColumn(), getStubColumn());
		
		assertEquals(eNull.hashCode(), eNull.hashCode());
		assertEquals(eNull.hashCode(), eNull2.hashCode());
		
		assertEquals(eVal.hashCode(), eVal.hashCode());
		assertEquals(eVal.hashCode(), eVal2.hashCode());
		
		assertEquals(eCol.hashCode(), eCol.hashCode());
		assertEquals(eCol.hashCode(), eCol2.hashCode());
	}
	@Test
	public void testEquals() {
		Entry eNull = new Entry(getStubColumn(), null),
					eNull2 = new Entry(getStubColumn(), null),
					eVal = new Entry(getStubColumn(), getStubValue()),
					eVal2 = new Entry(getStubColumn(), getStubValue()),
					eCol = new Entry(getStubColumn(), getStubColumn()),
					eCol2 = new Entry(getStubColumn(), getStubColumn());
		
		assertEquals(eNull, eNull);
		assertEquals(eNull, eNull2);
		
		assertEquals(eVal, eVal);
		assertEquals(eVal, eVal2);
		
		assertEquals(eCol, eCol);
		assertEquals(eCol, eCol2);
		
		assertNotEquals(eNull, eVal);
		assertNotEquals(eNull, eCol);
		assertNotEquals(eVal, eCol);
	}

	private Column getStubColumn() {
		return new Column("TABLE", "COLUMN", type);
	}
	
	private Object getStubValue() {
		if (type.getTypeClass() == Boolean.class)
			return false;
		else if (type.getTypeClass() == Short.class)
			return (short) 0;
		else if (type.getTypeClass() == Integer.class)
			return (int) 0;
		else if (type.getTypeClass() == Long.class)
			return (long) 0;
		else if (type.getTypeClass() == Float.class)
			return (float) 0;
		else if (type.getTypeClass() == Double.class)
			return (double) 0;
		else if (type.getTypeClass() == Character.class)
			return 'a';
		else if (type.getTypeClass() == String.class)
			return "A";
		else
			return null;	// Should not happen
	}
}
