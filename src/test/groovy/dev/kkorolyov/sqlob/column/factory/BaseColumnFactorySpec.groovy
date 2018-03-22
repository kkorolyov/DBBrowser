package dev.kkorolyov.sqlob.column.factory

import spock.lang.Specification

import java.lang.reflect.Field

class BaseColumnFactorySpec extends Specification {
	Byte aByte = 54
	Field f = getClass().getDeclaredField("aByte")

	BaseColumnFactory factory = Spy()

	def "accepts if type assignable from accepted type"() {
		when:
		factory.addAll([f.getType()])

		then:
		factory.accepts(f)
	}
	def "rejects if type not assignable from accepted type"() {
		expect:
		!factory.accepts(f)
	}
}
