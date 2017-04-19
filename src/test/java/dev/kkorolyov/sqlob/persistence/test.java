package dev.kkorolyov.sqlob.persistence;

import java.sql.SQLException;

import dev.kkorolyov.sqlob.JDBCMocks;
import dev.kkorolyov.sqlob.Session;
import dev.kkorolyov.sqlob.Stub.BasicStub;

/**
 *
 */
public class test {
	public static void main(String[] args) throws SQLException {
		JDBCMocks mocks = new JDBCMocks();
		while (true) {
			Session session = new Session(mocks.ds);
			session.put(BasicStub.random());
		}
	}
}
