package dev.kkorolyov.sqlob.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.kkorolyov.sqlob.JDBCMocks;
import dev.kkorolyov.sqlob.Stub.BasicStub;

class SqlobCacheTest {
	private static final Class<?> stubClass = BasicStub.class;

	private JDBCMocks mocks = new JDBCMocks();

	private SqlobCache cache = new SqlobCache();

	@Test
	void newClassIsCached() throws NoSuchFieldException, IllegalAccessException, SQLException {
		Field mapField = cache.getClass().getDeclaredField("classMap");
		mapField.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<Class<?>, SqlobClass<?>> classMap = (Map<Class<?>, SqlobClass<?>>) mapField.get(cache);

		assertEquals(0, classMap.size());

		cache.get(stubClass, mocks.conn);

		assertEquals(1, classMap.size());
	}
}
