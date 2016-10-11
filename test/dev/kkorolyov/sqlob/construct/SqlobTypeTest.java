package dev.kkorolyov.sqlob.construct;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@SuppressWarnings("javadoc")
public class SqlobTypeTest {
	@Parameters(name = "Class({0})")
	public static Object[] data() {
		return new Object[]{Boolean.class,
												Short.class,
												Integer.class,
												Long.class,
												Float.class,
												Double.class,
												Character.class,
												String.class};
	}
	private final Class<?> typeClass;
	
	public SqlobTypeTest(Class<?> input) {
		typeClass = input;
	}
	@Test
	public void testGetTypeClass() {
		assertEquals(typeClass, new SqlobType(typeClass, getStubTypeName(), getStubTypeCode()).getTypeClass());
	}
	@Test
	public void testGetTypeName() {
		assertEquals(getStubTypeName(), new SqlobType(typeClass, getStubTypeName(), getStubTypeCode()).getTypeName());
	}
	@Test
	public void testGetTypeCode() {
		assertEquals(getStubTypeCode(), new SqlobType(typeClass, getStubTypeName(), getStubTypeCode()).getTypeCode());
	}
	
	@Test
	public void testHashCode() {
		SqlobType type = new SqlobType(typeClass, getStubTypeName(), getStubTypeCode()),
							type2 = new SqlobType(typeClass, getStubTypeName(), getStubTypeCode()),
							nullType = new SqlobType(typeClass, null, 0),
							nullType2 = new SqlobType(typeClass, null, 0),
							nullNullType = new SqlobType(null, null, 0),
							nullNullType2 = new SqlobType(null, null, 0);

		assertEquals(type.hashCode(), type.hashCode());
		assertEquals(type.hashCode(), type2.hashCode());
		
		assertEquals(nullType.hashCode(), nullType.hashCode());
		assertEquals(nullType.hashCode(), nullType2.hashCode());
		
		assertEquals(nullNullType.hashCode(), nullNullType.hashCode());
		assertEquals(nullNullType.hashCode(), nullNullType2.hashCode());
	}
	@Test
	public void testEquals() {
		SqlobType type = new SqlobType(typeClass, getStubTypeName(), getStubTypeCode()),
							type2 = new SqlobType(typeClass, getStubTypeName(), getStubTypeCode()),
							nullType = new SqlobType(typeClass, null, 0),
							nullType2 = new SqlobType(typeClass, null, 0),
							nullNullType = new SqlobType(null, null, 0),
							nullNullType2 = new SqlobType(null, null, 0);
		
		assertEquals(type, type);
		assertEquals(type, type2);
		
		assertEquals(nullType, nullType);
		assertEquals(nullType, nullType2);

		assertEquals(nullNullType, nullNullType);
		assertEquals(nullNullType, nullNullType2);

		assertNotEquals(type, nullType);
		assertNotEquals(type, nullNullType);
		assertNotEquals(nullType, nullNullType);
	}
	
	private static String getStubTypeName() {
		return "TYPE";
	}
	private static int getStubTypeCode() {
		return 145764432;
	}
}
