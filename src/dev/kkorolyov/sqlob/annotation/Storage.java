package dev.kkorolyov.sqlob.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Optionally indicates the table used to persist instances of a particular class.
 * May also provide an initialization script for the table.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Storage {
	/** @return name of table to use for persisting instances of this class */
	String table();
	/** @return initialization script for the persisting table */
	String init();
}
