package dev.kkorolyov.sqlob.service

import dev.kkorolyov.sqlob.util.Where
import groovy.transform.PackageScope
import spock.lang.Specification

import java.lang.reflect.Field

import static dev.kkorolyov.sqlob.service.Constants.ID_NAME

class StatementGeneratorSpec extends Specification {
  Mapper mapper = Mock()

  StatementGenerator generator = new StatementGenerator(mapper)

  def "uses mapped names"() {
		Class c = new Object() {
			private Object field
    }.class
		Field f = c.getDeclaredField("field")
		Where where = new Where()

		mapper.getAssociatedClasses(c) >> [c]
		mapper.getPersistableFields(c) >> [f]

		when:
		generator.create(c)
		generator.select(c, where)
		generator.insert(c)
		generator.update(c, where)
		generator.delete(c, where)

		then:
		5 * mapper.getName(c) >> "FakeClass"
		3 * mapper.getName(f) >> "FakeField"
	}

	def "appends WHERE clause if condition specified"() {
		Class c = new Object() {}.class
		Where where = new Where("FakeColumn", "=", null)

		2 * mapper.getPersistableFields(c) >> []

		String whereClause = "WHERE " + where.toString()

		expect:
		!generator.select(c, null).contains(whereClause)
		generator.select(c, where).contains(whereClause)

		!generator.selectId(c, null).contains(whereClause)
		generator.selectId(c, where).contains(whereClause)

		!generator.update(c, null).contains(whereClause)
		generator.update(c, where).contains(whereClause)

		!generator.delete(c, null).contains(whereClause)
		generator.delete(c, where).contains(whereClause)
	}

	def "create() creates each associated class"() {
		Object cInner1 = new Object() {}
		Object cInner2 = new Object() {}
		Class c = new Object() {
			private Object o1 = cInner1
			Object o2 = cInner2
		}.class

		1 * mapper.getName(cInner1.class) >> cInner1.class.getName()
		1 * mapper.getName(cInner2.class) >> cInner2.class.getName()
		1 * mapper.getName(c) >> c.getName()

		1 * mapper.getPersistableFields(c) >> [c.getDeclaredField("o1"),
																					 c.getDeclaredField("o2")]
		2 * mapper.getPersistableFields(_) >> []
		1 * mapper.getAssociatedClasses(c) >> [cInner1.class,
																					 cInner2.class,
																					 c]

		expect:
		Iterable statements = generator.create(c)

		statements.size() == 3
		statements[0].contains(cInner1.class.getName())
		statements[1].contains(cInner2.class.getName())
		statements[2].contains(c.getName())
	}

	def "select() selects all columns"() {
		Class c = new Object() {}.class

		expect:
		generator.select(c, null).contains("*")
	}
	def "selectId() selects only ID column"() {
		Class c = new Object() {}.class

		expect:
		generator.selectId(c, null).contains(ID_NAME)
	}

	def "insert() inserts ID and persistable fields"() {
		Class c = new Object() {
			private String s
			@PackageScope int i
			boolean b
			UUID u
		}.class

		String fieldName = "FakeField"

		1 * mapper.getPersistableFields(c) >> [c.getDeclaredField("s"),
																					 c.getDeclaredField("i"),
																					 c.getDeclaredField("b"),
																					 c.getDeclaredField("u")]
		1 * mapper.getName(_ as Class) >> "FakeClass"
		4 * mapper.getName(_ as Field) >> fieldName

		expect:
		String statement = generator.insert(c)

		statement.contains(ID_NAME)
		statement.count(fieldName) == 4
		statement.count("?") == 5
	}

	def "update() updates only persistable fields"() {
		Class c = new Object() {
			private String s
			@PackageScope int i
			boolean b
			char c
		}.class

		String fieldName = "FakeField"

		1 * mapper.getPersistableFields(c) >> [c.getDeclaredField("s"),
																					 c.getDeclaredField("i"),
																					 c.getDeclaredField("b"),
																					 c.getDeclaredField("c")]
		1 * mapper.getName(_ as Class) >> "FakeClass"
		4 * mapper.getName(_ as Field) >> fieldName

		expect:
		String statement = generator.update(c, null)

		!statement.contains(ID_NAME)
		statement.count(fieldName) == 4
		statement.count("=")
		statement.count("?")
	}
}
