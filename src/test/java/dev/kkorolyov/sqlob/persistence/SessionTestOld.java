package dev.kkorolyov.sqlob.persistence;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import dev.kkorolyov.sqlob.Session;
import dev.kkorolyov.sqlob.Stub.AnnotatedStub;
import dev.kkorolyov.sqlob.Stub.BasicStub;
import dev.kkorolyov.sqlob.Stub.SmartStub;
import dev.kkorolyov.sqlob.TestAssets;
import dev.kkorolyov.sqlob.utility.Condition;

@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class SessionTestOld {
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
	
	public SessionTestOld(DataSource input) {
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
	public void testPutID() throws SQLException {
		UUID id = UUID.randomUUID();
		
		try (Session s = new Session(ds)) {
			assertFalse(s.put(id, BasicStub.random()));
			assertFalse(s.put(id, AnnotatedStub.random()));
			assertFalse(s.put(id, SmartStub.random()));
			
			s.flush();
			
			assertTrue(s.put(id, BasicStub.random()));
			assertTrue(s.put(id, AnnotatedStub.random()));
			assertTrue(s.put(id, SmartStub.random()));
		}
	}
	
	@Test
	public void testGet() throws SQLException {
		BasicStub bs = BasicStub.random();
		AnnotatedStub as = AnnotatedStub.random();
		SmartStub ss = SmartStub.random();
		
		try (Session s = new Session(ds)) {
			UUID 	bsId = s.put(bs),
						asId = s.put(as),
						ssId = s.put(ss);
			
			s.flush();
			
			assertEquals(bs, s.get(bs.getClass(), bsId));
			assertEquals(as, s.get(as.getClass(), asId));
			assertEquals(ss, s.get(ss.getClass(), ssId));
		}
	}
	@Test
	public void testGetCondition() throws SQLException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		BasicStub bs = BasicStub.random();
		SmartStub ss = new SmartStub(bs);
		Condition matchSS = new Condition("stub", "=", bs),	// Tests recursion
							noMatchSS = new Condition("stub", "!=", bs);
		
		Set<BasicStub> bsSet = buildBasicStubSet(10);
		bsSet.add(bs);
		Condition bsCond = new Condition("short0", "<", Byte.MAX_VALUE / 2).or("string0", "LIKE", "0%");
		
		try (Session s = new Session(ds)) {
			UUID ssId = s.put(ss);
			
			s.flush();
			
			Set<SmartStub> oneMatch = s.get(SmartStub.class, matchSS);
			assertEquals(1, oneMatch.size());
			assertEquals(s.get(ss.getClass(), ssId), oneMatch.iterator().next());
			
			Set<?> noMatch = s.get(ss.getClass(), noMatchSS);
			assertEquals(0, noMatch.size());
			
			Set<BasicStub> filteredBS = new HashSet<>();
			for (BasicStub currentBS : bsSet) {
				s.put(currentBS);
				
				Field shortField = BasicStub.class.getDeclaredField("short0"),
							stringField = BasicStub.class.getDeclaredField("string0");
				shortField.setAccessible(true);
				stringField.setAccessible(true);
				
				if ((shortField.getShort(currentBS) < Byte.MAX_VALUE / 2) || ((String) stringField.get(currentBS)).charAt(0) == '0')
					filteredBS.add(currentBS);
			}
			s.flush();
			
			assertEquals(filteredBS, s.get(BasicStub.class, bsCond));
		}
	}
	
	@Test
	public void testGetId() throws SQLException {
		BasicStub bs = BasicStub.random();
		AnnotatedStub as = AnnotatedStub.random();
		SmartStub ss = SmartStub.random();
		
		try (Session s = new Session(ds)) {
			UUID 	bsId = s.put(bs),
						asId = s.put(as),
						ssId = s.put(ss);
			
			s.flush();
			
			assertEquals(bsId, s.getId(bs));
			assertEquals(asId, s.getId(as));
			assertEquals(ssId, s.getId(ss));
		}
	}
	
	@Test
	public void testDrop() throws SQLException {
		BasicStub bs = BasicStub.random();
		AnnotatedStub as = AnnotatedStub.random();
		SmartStub ss = SmartStub.random();
		
		try (Session s = new Session(ds)) {
			UUID 	bsId = s.put(bs),
						asId = s.put(as),
						ssId = s.put(ss);
			
			s.flush();
			
			assertEquals(bs, s.get(BasicStub.class, bsId));
			assertEquals(as, s.get(AnnotatedStub.class, asId));
			assertEquals(ss, s.get(SmartStub.class, ssId));
			
			assertTrue(s.drop(bs.getClass(), bsId));
			assertTrue(s.drop(as.getClass(), asId));
			assertTrue(s.drop(ss.getClass(), ssId));
			
			s.flush();
			
			assertFalse(s.drop(bs.getClass(), bsId));	// Assert dropped
			assertFalse(s.drop(as.getClass(), asId));
			assertFalse(s.drop(ss.getClass(), ssId));
			
			assertNull(s.get(bs.getClass(), bsId));
			assertNull(s.get(as.getClass(), asId));
			assertNull(s.get(as.getClass(), ssId));
		}
	}
	@Test
	public void testDropCondition() throws SQLException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Set<BasicStub> bsSet = buildBasicStubSet(10);
		Condition bsCond = new Condition("short0", "<", Byte.MAX_VALUE / 2).or("string0", "LIKE", "0%");
		
		try (Session s = new Session(ds)) {
			Set<BasicStub> filteredBS = new HashSet<>();
			for (BasicStub currentBS : bsSet) {
				s.put(currentBS);
				
				Field shortField = BasicStub.class.getDeclaredField("short0"),
							stringField = BasicStub.class.getDeclaredField("string0");
				shortField.setAccessible(true);
				stringField.setAccessible(true);
				
				if ((shortField.getShort(currentBS) < Byte.MAX_VALUE / 2) || ((String) stringField.get(currentBS)).charAt(0) == '0')
					filteredBS.add(currentBS);
			}
			s.flush();
			
			assertEquals(filteredBS.size(), s.drop(BasicStub.class, bsCond));
			
			for (BasicStub bs : filteredBS)
				assertNull(s.getId(bs));
		}
	}
	
	@Test
	public void testExceptions() throws SQLException {
		try (Session s = new Session(ds)) {
			s.getId(null);
			fail("Failed to throw IllegalArgumentException from getId for null Object");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		
		try (Session s = new Session(ds)) {
			s.get(null, UUID.randomUUID());
			fail("Failed to throw IllegalArgumentException from get for null Class");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		try (Session s = new Session(ds)) {
			s.get(BasicStub.class, (UUID) null);
			fail("Failed to throw IllegalArgumentException from get for null ID");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		try (Session s = new Session(ds)) {
			s.get(null, (UUID) null);
			fail("Failed to throw IllegalArgumentException from get for null Class and null ID");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		
		try (Session s = new Session(ds)) {
			s.get(null, new Condition("", "", null));
			fail("Failed to throw IllegalArgumentException from get for null Class");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		try (Session s = new Session(ds)) {
			s.get(null, (Condition) null);
			fail("Failed to throw IllegalArgumentException from get for null Class and null Condition");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		
		try (Session s = new Session(ds)) {
			s.put(null);
			fail("Failed to throw IllegalArgumentException from put for null Object");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		
		try (Session s = new Session(ds)) {
			s.put(null, "");
			fail("Failed to throw IllegalArgumentException from put for null ID");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		try (Session s = new Session(ds)) {
			s.put(UUID.randomUUID(), null);
			fail("Failed to throw IllegalArgumentException from put for null Object");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		try (Session s = new Session(ds)) {
			s.put(null, null);
			fail("Failed to throw IllegalArgumentException from get for null ID and null Object");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		
		try (Session s = new Session(ds)) {
			s.drop(null, UUID.randomUUID());
			fail("Failed to throw IllegalArgumentException from drop for null Class");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		try (Session s = new Session(ds)) {
			s.drop(BasicStub.class, (UUID) null);
			fail("Failed to throw IllegalArgumentException from drop for null ID");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		try (Session s = new Session(ds)) {
			s.drop(null, (UUID) null);
			fail("Failed to throw IllegalArgumentException from drop for null Class and null ID");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		
		try (Session s = new Session(ds)) {
			s.drop(null, new Condition("", "", null));
			fail("Failed to throw IllegalArgumentException from drop for null Class");
		} catch (IllegalArgumentException e) {
			// Expected
		}
		try (Session s = new Session(ds)) {
			s.drop(null, (Condition) null);
			fail("Failed to throw IllegalArgumentException from drop for null Class and null Condition");
		} catch (IllegalArgumentException e) {
			// Expected
		}
	}
	
	private static Set<BasicStub> buildBasicStubSet(int num) {
		Set<BasicStub> bsSet = new HashSet<>();
		for (int i = 0; i < num; i++)
			bsSet.add(BasicStub.random());
		
		return bsSet;
	}
}
