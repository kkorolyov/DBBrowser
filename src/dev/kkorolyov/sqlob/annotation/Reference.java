package dev.kkorolyov.sqlob.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that a field should be persisted as a foreign reference to another object persisted in its own table.
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface Reference {
	// Tag
}
