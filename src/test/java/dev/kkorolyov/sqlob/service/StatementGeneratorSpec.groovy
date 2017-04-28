package dev.kkorolyov.sqlob.service

import dev.kkorolyov.simplelogs.Logger
import dev.kkorolyov.sqlob.utility.Condition
import groovy.transform.PackageScope
import spock.lang.Specification

import java.lang.reflect.Field

import static dev.kkorolyov.sqlob.service.Constants.ID_NAME

class StatementGeneratorSpec extends Specification {
  static {
    Logger.getLogger("dev.kkorolyov.sqlob", Logger.Level.DEBUG, new PrintWriter(System.err));	// Enable logging
  }

  Mapper mapper = Mock()

  StatementGenerator generator = new StatementGenerator(mapper)

  def "uses mapped names"() {
		Class c = new Object() {
			private Object field
    }.class
		Field f = c.getDeclaredField("field")
		Condition where = new Condition()

		when:
		mapper.getAssociatedClasses(c) >> [c]
		mapper.getPersistableFields(c) >> [f]

		generator.create(c)
		generator.select(c, where)
		generator.insert(c)
		generator.update(c, where)
		generator.delete(c, where)

		then:
		5 * mapper.getName(c) >> "FakeClass"
		3 * mapper.getName(f) >> "FakeField"
	}

	def "create statements create each associated class"() {
		Object cInner1 = new Object() {}
		Object cInner2 = new Object() {}
		Class c = new Object() {
			private Object o1 = cInner1
			Object o2 = cInner2
		}.class

		when:
		1 * mapper.getName(cInner1.class) >> cInner1.class.getName()
		1 * mapper.getName(cInner2.class) >> cInner2.class.getName()
		1 * mapper.getName(c) >> c.getName()

		1 * mapper.getPersistableFields(c) >> [c.getDeclaredField("o1"),
																					 c.getDeclaredField("o2")]
		2 * mapper.getPersistableFields(_) >> []
		1 * mapper.getAssociatedClasses(c) >> [cInner1.class,
																					 cInner2.class,
																					 c]

		Iterable statements = generator.create(c)

		then:
		statements.size() == 3
		statements[0].contains(cInner1.class.getName())
		statements[1].contains(cInner2.class.getName())
		statements[2].contains(c.getName())
	}

	def "generateInsert() returns statement for class and per complex field"() {
    expect:
    generator.insert(c).size() == size

    where:
    c << [HasPrimitive, HasComplexes]
    size << [1, 4]
  }
  def "generateInsert() returns statement for class last"() {
    expect:
    generator.insert(c).last() == statement

    where:
    c << [HasComplexes]
    statement << ["INSERT INTO TABLE HasComplexes (" + ID_NAME + ", e1, e2, e3) VALUES (?, ?, ?, ?)"]
  }

  def "generateSelect() appends WHERE if condition not null"() {
    expect:
    generator.select(c, where) == statement

    where:
    c << [HasPrimitive, HasPrimitive]
    where << [new Condition("s", "==", "test"), null]
    statement << ["SELECT * FROM HasPrimitive WHERE s == ?",
                  "SELECT * FROM HasPrimitive"]
  }
  def "generateSelectId() selects only id field"() {
    expect:
    generator.selectId(c, where) == statement

    where:
    c << [HasPrimitive, HasPrimitive]
    where << [new Condition("s", "==", "s"), null]
    statement << ["SELECT " + ID_NAME + " FROM HasPrimitive WHERE s == ?",
                  "SELECT " + ID_NAME + " FROM HasPrimitive"]
  }

  def "generateDelete() appends WHERE if condition not null"() {
    expect:
    generator.delete(c, where) == statement

    where:
    c << [HasPrimitive]
    where << [new Condition("s", "==", "test")]
    statement << ["DELETE FROM HasPrimitive WHERE s == ?"]
  }

  class HasPrimitive {
    private String s
  }
  class HasComplexes {
    HasPrimitive e1
    @PackageScope HasPrimitive e2
    private HasPrimitive e3
  }
}
