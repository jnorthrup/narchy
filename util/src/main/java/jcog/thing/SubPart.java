package jcog.thing;

/** part of a part.  simplified instance for management by the super-part */
public interface SubPart<T extends Thing> {

    default void startIn(T t, Part<T> parent) {

    }

    default void stopIn(T t, Part<T> parent) {

    }

}
