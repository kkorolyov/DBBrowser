package dev.kkorolyov.sqlob.type;

import dev.kkorolyov.sqlob.type.BaseSqlobType;

import java.sql.ResultSet;
import java.sql.Timestamp;

public class SqlTimestampSqlobType extends BaseSqlobType<Timestamp> {
	public SqlTimestampSqlobType() {
		put(DEFAULT,
				new Delegate<>("TIMESTAMP(6)", ResultSet::getTimestamp,
						Timestamp.class));
	}
}
