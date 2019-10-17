package jcog.pri;

/**
 * whether it has been deleted (read-only)
 */
@FunctionalInterface
public interface Deleteable {

    boolean isDeleted();

}
