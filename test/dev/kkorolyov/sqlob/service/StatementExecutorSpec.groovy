package dev.kkorolyov.sqlob.service

import spock.lang.Shared
import spock.lang.Specification

import java.sql.Connection

class StatementExecutorSpec extends Specification {
  @Shared Connection conn = Mock()
  @Shared Mapper mapper = Mock()

	def "create() executes all statements in order"() {

	}
}
