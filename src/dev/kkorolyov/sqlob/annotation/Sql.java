package dev.kkorolyov.sqlob.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates the SQL type mapped to a field.
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface Sql {
	/** @return SQL type mapped to the field with this annotation */
	String value();
}
