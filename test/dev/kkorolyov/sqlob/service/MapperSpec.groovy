package dev.kkorolyov.sqlob.service

import spock.lang.Specification

import java.sql.ResultSet

/**
 *
 */
class MapperSpec extends Specification {
  def mapper = new Mapper()

  def "put uses sanitized sql type"() {
    Class<?> c = Mapper.class
    String sqlType = "Some SQL"

    when:
    mapper.put(c, sqlType, {ResultSet.getString})

    then:
    mapper.getSql(c) == Constants.sanitize(sqlType)
  }

  def "class with same-class field noticed"() {
    Class<?> c = SelfRefStub.class
    String sqlType = "SelfRef"

    when:
    mapper.put(c, sqlType, {ResultSet.getString})

    then:
    Iterable<Class<?>> results = mapper.getAssociatedClasses(c)
    results.size() == 1
    results.contains(c)
  }

  class SelfRefStub {
    SelfRefStub selfRef;
  }
}
