package dev.kkorolyov.sqlob.util;

import dev.kkorolyov.sqlob.annotation.Column;
import dev.kkorolyov.sqlob.annotation.Table;
import dev.kkorolyov.sqlob.annotation.Transient;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Provides static utility methods for translating properties between Java and SQL.
 */
public final class PersistenceHelper {
	private PersistenceHelper() {}

	/**
	 * Returns the name of the table associated with class {@code c}.
	 * Table name defaults to the name of the class, but may be customized with the {@link Table} annotation.
	 * @param c class to get table name for
	 * @return name of table matching {@code c}
	 * @see Table
	 */
	public static String getName(Class<?> c) {
		Table override = c.getAnnotation(Table.class);
		if (override != null && override.value().length() <= 0) throw new IllegalArgumentException(c + " has a Table annotation with an empty name");

		return (override == null) ? c.getSimpleName() : override.value();
	}
	/**
	 * Returns the name of the column associated with field {@code f}.
	 * Column name defaults to the name of the field, but may be customized with the {@link Column} annotation.
	 * @param f field to get column name for
	 * @return name of column matching {@code f}
	 * @see Column
	 */
	public static String getName(Field f) {
		Column override = f.getAnnotation(Column.class);
		if (override != null && override.value().length() <= 0) throw new IllegalArgumentException(f + " has a Column annotation with an empty name");

		return (override == null) ? f.getName() : override.value();
	}

	/**
	 * Returns the name of the table associated with field {@code f}.
	 * This is meant for special cases where a field requires its own table instead of a column on the declaring class's table.
	 * Table name is defined as {@code {DECLARING_CLASS_NAME}_{FIELD_NAME}}
	 * @param f field to get table name for
	 * @return name of table matching {@code f}
	 */
	public static String getFieldTableName(Field f) {
		return getName(f.getDeclaringClass()) + "_" + getName(f);
	}

	/** @return all declared fields in {@code c} matching the requirements of {@link #isPersistable(Field)} */
	public static Stream<Field> getPersistableFields(Class<?> c) {
		return Arrays.stream(c.getDeclaredFields())
				.filter(PersistenceHelper::isPersistable);
	}
	/**
	 * Checks whether {@code f} is a "persistable" field.
	 * A field is deemed persistable if it is
	 * <pre>
	 *   - Not static
	 *   - Not transient
	 *   - Not {@link Transient}
	 *   - Not synthetic
	 * </pre>
	 * @param f field to test
	 * @return whether {@code f} is a persistable field
	 */
	public static boolean isPersistable(Field f) {
		int modifiers = f.getModifiers();
		return !Modifier.isStatic(modifiers)
				&& !Modifier.isTransient(modifiers)
				&& f.getAnnotation(Transient.class) == null
				&& !f.isSynthetic();
	}
}
