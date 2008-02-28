package org.apache.xbean.recipe;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * ParameterNames explicitly declared the names of a constructor parameters.
 * This annotation was introduced in Java6 and is present here to provide backwards
 * compatability when running on Java5
 */
@Documented
@Retention(value = RUNTIME)
@Target(value = {CONSTRUCTOR, METHOD})
public @interface ParameterNames {
    String[] value();
}