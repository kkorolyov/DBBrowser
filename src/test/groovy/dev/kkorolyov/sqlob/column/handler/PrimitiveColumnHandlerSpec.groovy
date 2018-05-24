package dev.kkorolyov.sqlob.column.handler

import dev.kkorolyov.sqlob.ExecutionContext

import spock.lang.Specification

import java.lang.reflect.Field

class PrimitiveColumnHandlerSpec extends Specification {
	class Stub {
		String value
		Stub otherValue
	}
	Field f = Stub.getDeclaredField("value")
	Field otherF = Stub.getDeclaredField("otherValue")

	ExecutionContext context = Mock()

	PrimitiveColumnHandler handler = new PrimitiveColumnHandler()

	def "accepts field with associated sqlob type"() {
		expect:
		handler.accepts(f)
	}
	def "rejects field without associated sqlob type"() {
		expect:
		!handler.accepts(otherF)
	}
}
