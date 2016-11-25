package dev.kkorolyov.sqlob.persistence;

import java.sql.Connection;

class SessionWorker {
	final Connection conn;
	
	SessionWorker(Connection conn) {
		this.conn = conn;
	}
}
