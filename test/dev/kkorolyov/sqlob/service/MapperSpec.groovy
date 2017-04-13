package dev.kkorolyov.sqlob.service

import dev.kkorolyov.sqlob.annotation.Transient
import spock.lang.Specification

import java.lang.reflect.Field
import java.sql.ResultSet

/**
 *
 */
class MapperSpec extends Specification {
  def mapper = new Mapper()

  def "put() uses sanitized sql type"() {
    Class<?> c = Empty.class
    String sqlType = "Some SQL"

    when:
    mapper.put(c, sqlType, {ResultSet.getString})

    then:
    mapper.getSql(c) == Constants.sanitize(sqlType)
  }

  def "getPersistableFields() returns one of each persistable field"() {
    when:
    Class<?> c = Multi.class

    then:
    Iterable<Field> results = mapper.getPersistableFields(c)

    results.size() == 3
    results.containsAll(Multi.class.getDeclaredField("e1"),
        Multi.class.getDeclaredField("e2"),
        Multi.class.getDeclaredField("e3"))
  }
  def "getPersistableFields() ignores Transient-tagged fields"() {
    when:
    Class<?> c = TransientTag.class

    then:
    mapper.getPersistableFields(c).size() == 0
  }
  def "getPersistableFields() ignores transient fields"() {
    when:
    Class<?> c = TransientModifier.class

    then:
   mapper.getPersistableFields(c).size() == 0
  }

  def "getAssociatedClasses() returns one of each class"() {
    when:
    Class<?> c = Multi.class

    then:
    Iterable<Class<?>> results = mapper.getAssociatedClasses(c)
    results.size() == 2
    results.containsAll(Multi.class, Empty.class)
  }

  def "getAssociatedClasses() counts self-ref once"() {
    when:
    Class<?> c = SelfRef.class

    then:
    Iterable<Class<?>> results = mapper.getAssociatedClasses(c)
    results.size() == 1
    results.contains(c)
  }

  class Empty {}

  class TransientTag {
    @Transient
    private Empty e
  }
  class TransientModifier {
    private transient Empty e
  }

  class Multi {
    private Empty e1
    private Empty e2
    private Empty e3
  }

  class SelfRef {
    SelfRef selfRef;
  }
}
