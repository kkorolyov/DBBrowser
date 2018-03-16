package dev.kkorolyov.sqlob

import dev.kkorolyov.sqlob.util.Where

import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

class SessionSpec extends Specification {
	ResultSet rs = Mock()
	Statement s = Mock()
	PreparedStatement ps = Mock()
	Connection conn = Mock()
	DataSource ds = Mock()

	Session session = new Session(ds)

	def setup() {
		s.executeQuery(_) >> rs
		ps.executeQuery() >> rs
		conn.createStatement() >> s
		conn.prepareStatement(_) >> ps
		ds.getConnection() >> conn
	}
	def cleanup() {
		session.close()
	}

	def "query actions do not commit underlying Connection"() {
		session = new Session(ds, 0)

		Class c = Object
		Object o = new Object()
		UUID id = UUID.randomUUID()
		Where condition = new Where()

		when:
		session.get(c, id)
		then:
		0 * conn.commit()

		when:
		session.get(c, condition)
		then:
		0 * conn.commit()

		when:
		session.getId(o)
		then:
		0 * conn.commit()
	}

	def "commits underlying Connection when update actions fill buffer"() {
		session = new Session(ds, bufferSize)

		Class c = Object
		Object o = new Object()
		UUID id = UUID.randomUUID()
		Where condition = new Where()

		int cycles = 5

		when:
		(1..(cycles * bufferSize)).each { session.put(o) }
		then:
		cycles * conn.commit()

		when:
		(1..(cycles * bufferSize)).each { session.put(id, o) }
		then:
		cycles * conn.commit()

		when:
		(1..(cycles * bufferSize)).each { session.drop(c, id) }
		then:
		cycles * conn.commit()

		when:
		(1..(cycles * bufferSize)).each { session.drop(c, condition) }
		then:
		cycles * conn.commit()

		where:
		bufferSize << (1..100)
	}

	def "flush() commits underlying Connection"() {
		Object o = new Object()

		when:
		session.getId(o)
		session.flush()

		then:
		1 * conn.commit()
	}
	def "close() commits underlying Connection"() {
		Object o = new Object()

		when:
		session.getId(o)
		session.close()

		then:
		1 * conn.commit()
	}
}
