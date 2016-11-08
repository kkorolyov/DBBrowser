package dev.kkorolyov.sqlob.persistence;

import static org.junit.Assert.fail;

import org.junit.Test;

import dev.kkorolyov.sqlob.annotation.Reference;
import dev.kkorolyov.sqlob.annotation.Sql;

@SuppressWarnings("javadoc")
public class ConnectionTest {

	@Test
	public void testGet() {
		fail("Not yet implemented");
	}
	
	class DumbStub {
		@Sql("INT")
		int num;
	}
	class SmartStub {
		@Reference
		DumbStub stub;
	}
}
