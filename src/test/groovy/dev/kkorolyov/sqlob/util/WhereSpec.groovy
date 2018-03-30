package dev.kkorolyov.sqlob.util

import spock.lang.Shared
import spock.lang.Specification

import java.sql.PreparedStatement

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class WhereSpec extends Specification {
	@Shared String attribute = randString()
	@Shared String operator = randString()
	@Shared Object value = Mock()

	Where where = new Where(attribute, operator, value)

	def "appends other where with delimiter"() {
		Where original = new Where(attribute, operator, value)
		Where other = new Where(randString(), randString(), Mock(Object))

		when:
		where."$method"(other)

		then:
		where.getSql() == "${original.getSql()} $delimiter (${other.getSql()})"

		where:
		method << ["and", "or"]
		delimiter << ["AND", "OR"]
	}

	def "is resolved after all attributes resolved"() {
		String otherAttribute = randString()

		where.and(otherAttribute, operator, value)

		expect:
		!where.isResolved()
		!where.resolve(attribute, { value -> value }).isResolved()
		where.resolve(otherAttribute, { value -> value }).isResolved()
	}
	def "returns unresolved attributes"() {
		String otherAttribute = randString()

		where.and(otherAttribute, operator, value)

		expect:
		where.getUnresolvedAttributes() == [attribute, otherAttribute] as Set
		where.resolve(attribute, { value -> value }).getUnresolvedAttributes() == [otherAttribute] as Set
		where.resolve(otherAttribute, { value -> value }).getUnresolvedAttributes() == [] as Set
	}

	def "contributes resolved values to statement"() {
		PreparedStatement statement = Mock()

		when:
		where.resolve(attribute, { value -> value })
				.contributeToStatement(statement)

		then:
		1 * statement.setObject(1, value)
	}
	def "excepts when contributing to statement with unresolved values"() {
		PreparedStatement statement = Mock()

		when:
		where.contributeToStatement(statement)

		then:
		thrown IllegalStateException
	}

	def "formats SQL string with wildcard"() {
		expect:
		where.getSql() == "$attribute $operator ?"
	}
	def "formats toString() with resolved value"() {
		expect:
		where.toString() == "$attribute $operator null"
		where.resolve(attribute, { value -> value }).toString() == "$attribute $operator $value"
	}
}
