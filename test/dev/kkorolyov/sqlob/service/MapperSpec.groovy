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
    mapper.put(c, sqlType, {ResultSet.getString}())

    then:
    mapper.getSql(c) == Constants.sanitize(sqlType)
  }
}
