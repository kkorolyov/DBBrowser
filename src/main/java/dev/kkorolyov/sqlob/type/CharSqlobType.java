package dev.kkorolyov.sqlob.type;

import dev.kkorolyov.sqlob.type.BaseSqlobType;

public class CharSqlobType extends BaseSqlobType<Character> {
	public CharSqlobType() {
		put(DEFAULT,
				new Delegate<>("CHAR(1)", (rs, column) -> {
					String string = rs.getString(column);
					return string == null ? null : string.charAt(0);
				}, Character.TYPE, Character.class));
	}
}
