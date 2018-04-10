package dev.kkorolyov.sqlob.type;

import dev.kkorolyov.sqlob.type.BaseSqlobType;

import java.sql.Date;
import java.sql.ResultSet;

public class SqlDateSqlobType extends BaseSqlobType<Date> {
	public SqlDateSqlobType() {
		put(DEFAULT,
				new Delegate<>("DATE", ResultSet::getDate,
						Date.class));
	}
}
