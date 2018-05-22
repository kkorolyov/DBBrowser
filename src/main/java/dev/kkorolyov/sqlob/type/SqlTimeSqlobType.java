package dev.kkorolyov.sqlob.type;

import dev.kkorolyov.sqlob.type.BaseSqlobType;

import java.sql.ResultSet;
import java.sql.Time;

public class SqlTimeSqlobType extends BaseSqlobType<Time> {
	public SqlTimeSqlobType() {
		put(DEFAULT,
				new Delegate<>("TIME(6)", ResultSet::getTime,
						Time.class));
	}
}
