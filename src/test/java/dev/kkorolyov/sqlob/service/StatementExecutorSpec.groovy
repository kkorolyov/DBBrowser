package dev.kkorolyov.sqlob.service

import dev.kkorolyov.sqlob.utility.Condition
import spock.lang.Specification

import java.lang.reflect.Field
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

class StatementExecutorSpec extends Specification {
  Mapper mapper = Mock()
	StatementGenerator generator = Mock()

	ResultSet rs = Mock()
	Statement s = Mock()
	PreparedStatement ps = Mock()
	Connection conn = Mock()

	StatementExecutor executor = new StatementExecutor(mapper)

	def setup() {
		rs.getObject(_ as String) >> null
		s.executeQuery(_) >> rs
		ps.executeQuery() >> rs
		conn.createStatement() >> s
		conn.prepareStatement(_) >> ps

		executor.setConnection(conn)

		Field generatorField = executor.class.getDeclaredField("generator")
		generatorField.setAccessible(true)
		generatorField.set(executor, generator)
	}

	def "create() creates as a batch"() {
		Class c = new Object() {}.class
		Iterable<String> creates = ["1", "2", "3", "4"]

		when:
		executor.create(c)

		then:
		1 * generator.create(c) >> creates

		1 * conn.createStatement() >> s
		creates.each {
			1 * s.addBatch(it)
		}
		1 * s.executeBatch()
	}

	static class Zero {}
	static class One {
		public Object f = new Object()
	}
	static class Two {
		public Object f1 = new Object()
		public Object f2 = new Object()
	}
	def "select(Condition) selects per class and complex field"() {
		mapper.isPrimitive({
			[One.getDeclaredField("f"), Two.getDeclaredField("f1"), Two.getDeclaredField("f2")].contains(it)
		}) >> false
		mapper.isPrimitive({
			![One.getDeclaredField("f"), Two.getDeclaredField("f1"), Two.getDeclaredField("f2")].contains(it)
		}) >> true
		when:
		executor.select(c, where as Condition)

		then:
		(1 + fields.size()) * conn.prepareStatement(_) >> ps
		(1 + fields.size()) * ps.executeQuery() >> rs
		1 * rs.next() >> true
		1 * mapper.getPersistableFields(c) >> fields

		then:
		1 * rs.next() >> false

		where:
		c << [Zero, One, Two]
		fields << [[],
							 [One.getDeclaredField("f")],
							 [Two.getDeclaredField("f1"), Two.getDeclaredField("f2")]]
		where << [new Condition(),
							new Condition("Fake", "!=", "Fake"),
							null]
	}
	def "select(Condition) selects per complex condition"() {

	}

	def "setting new Connection commits current Connection"() {
		when:
		executor.setConnection(Mock(Connection))

		then:
		1 * conn.commit()
	}

	def "flush() commits underlying Connection"() {
		when:
		executor.flush()

		then:
		1 * conn.commit()
	}
	def "close() commits before closing"() {
		when:
		0 * conn.commit()

		then:
		!executor.isClosed()

		when:
		executor.close()

		then:
		executor.isClosed()
		1 * conn.commit()
	}
}
