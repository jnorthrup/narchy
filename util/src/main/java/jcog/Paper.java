package jcog;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Indicates that the documentation (ie. JavaDoc) contains or suggests the
 * content of a an "Academic" paper which can elucidate the implemented and
 * potential functionality of the tagged subsystem or feature.
 */
@Retention(RetentionPolicy.SOURCE)
@Paper public @interface Paper {
}
