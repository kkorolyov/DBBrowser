package dev.kkorolyov.sqlob.type;

import java.util.UUID;

public class UuidSqlobType extends BaseSqlobType<UUID> {
	public UuidSqlobType() {
		put("PostgreSQL",
				new Delegate<>("UUID", (rs, column) -> rs.getObject(column, UUID.class),
						UUID.class));
		put("MySQL",
				new Delegate<>("CHAR(36)", UUID::toString, (rs, column) -> UUID.fromString(rs.getString(column)),
						UUID.class));
		put("SQLite",
				new Delegate<>("UUID", (rs, column) -> UUID.fromString(rs.getString(column)),
						UUID.class));
	}
}
