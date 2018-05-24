package dev.kkorolyov.sqlob.type;

import java.sql.ResultSet;

public class StringSqlobType extends BaseSqlobType<String> {
	public StringSqlobType() {
		put(DEFAULT,
				new Delegate<>("VARCHAR", ResultSet::getString,
						String.class));
		put("MySQL",
				new Delegate<>("TEXT", ResultSet::getString,
						String.class));
	}
}
