package dev.kkorolyov.sqlob;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;

import dev.kkorolyov.sqlob.Stub.SmartStub;
import dev.kkorolyov.sqlob.persistence.Session;

public class ProfilerTest {
	public static void main(String[] args) {
		DataSource ds = null;

		for (DataSource dss : TestAssets.dataSources()) {
			if (dss instanceof PGSimpleDataSource) ds = dss;
		}
		int tests = 1000;

		while (true) {
			try (Connection conn = ds.getConnection()) {
				Statement st = conn.createStatement();
				st.executeUpdate("DROP TABLE IF EXISTS SmartStub");
				st.executeUpdate("DROP TABLE IF EXISTS BasicStub");
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try (Session s = new Session(ds, 10000)) {
				List<UUID> ids = new LinkedList<>();
				long start = System.nanoTime();

				for (int i = 0; i < tests; i++) ids.add(s.put(SmartStub.random()));
				System.out.println((System.nanoTime() - start) / 1000000 + " ms to PUT " + tests + " items");
				start = System.nanoTime();

				for (UUID id : ids) s.get(SmartStub.class, id);
				System.out.println((System.nanoTime() - start) / 1000000 + " ms to GET " + tests + " items");
				start = System.nanoTime();

				for (UUID id : ids) s.drop(SmartStub.class, id);
				System.out.println((System.nanoTime() - start) / 1000000 + " ms to DROP " + tests + " items");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
