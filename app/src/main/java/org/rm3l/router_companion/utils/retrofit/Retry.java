package org.rm3l.router_companion.utils.retrofit;

/**
 * Created by rm3l on 2/10/17.
 */

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Makes the Call retry on failure
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface Retry {
    int value() default 3;
}
