package dev.kkorolyov.sqlob;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.*;

import javax.sql.DataSource;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class JDBCMocks {
	@Mock
	public DataSource ds;
	@Mock
	public Connection conn;
	@Mock
	public Statement statement;
	@Mock
	public PreparedStatement preparedStatement;
	@Mock
	public ResultSet resultSet;

	public JDBCMocks() {
		try {
			MockitoAnnotations.initMocks(this);

			when(statement.executeQuery(any(String.class))).thenReturn(resultSet);

			when(preparedStatement.executeQuery()).thenReturn(resultSet);

			when(conn.createStatement()).thenReturn(statement);
			when(conn.prepareStatement(any(String.class))).thenReturn(preparedStatement);

			when(ds.getConnection()).thenReturn(conn);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
