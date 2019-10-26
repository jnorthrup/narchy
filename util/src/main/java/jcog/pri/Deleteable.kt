package jcog.pri

/**
 * whether it has been deleted (read-only)
 */
@FunctionalInterface
interface Deleteable {

    val isDeleted: Boolean

}
